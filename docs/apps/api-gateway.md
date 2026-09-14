# API Gateway

**Build Tool:** Maven | **Arquitetura:** MVC (adaptada) | **Porta:** 8001 | **Status:** Concluído

**Código:** `backend/api-gateway/` | **Spec:** [docs/planning/specs/01-api-gateway.md](../planning/specs/01-api-gateway.md)
| **DER:** [docs/planning/der/api-gateway.mmd](../planning/der/api-gateway.mmd)

## Objetivo

O API Gateway é o único ponto de entrada externo do AutoHubStore. Toda requisição do frontend
Next.js (ou de qualquer cliente HTTP) passa por ele antes de chegar a um dos 9 microsserviços do
MVP. Ele concentra quatro responsabilidades que, se replicadas em cada serviço, gerariam
duplicação de lógica de segurança e superfície de ataque maior:

- Roteamento das requisições para o microsserviço correto.
- Validação centralizada do JWT lido do cookie httpOnly `access_token`.
- Rate limiting por IP (endpoints públicos) e por usuário autenticado (endpoints protegidos).
- CORS com credentials para o frontend, e headers de segurança básicos.

## Por que é didaticamente relevante

É o único serviço do MVP construído sobre **Spring WebFlux** (reativo, não bloqueante) em vez de
Spring MVC — decisão obrigatória porque Spring Cloud Gateway só existe na variante reativa. Isso
implica que toda a cadeia de filtros (`GlobalFilter`, `WebFilter`, `ReactiveAuthenticationManager`)
trabalha com `Mono`/`Flux` do Project Reactor, sem bloquear thread.

## Arquitetura (MVC adaptado)

O Gateway não tem JPA nem Kafka, então o padrão MVC dos demais serviços é adaptado: o pacote
`filter/` substitui `repository/`/`messaging/`, hospedando os `GlobalFilter` e os componentes de
segurança reativa do Spring Cloud Gateway.

```
com.autohubstore.gateway/
├── GatewayApplication.java
├── controller/
│   └── FallbackController.java            # respostas 503 quando um serviço downstream está fora
├── service/
│   ├── JwtService.java                    # valida/parseia o JWT (JJWT 0.13)
│   └── RateLimitService.java              # rate limit via Redis reativo (INCR + EXPIRE)
├── filter/
│   ├── RateLimitFilter.java               # GlobalFilter — chama RateLimitService antes da rota
│   ├── JwtServerAuthenticationConverter.java  # extrai o JWT do cookie access_token
│   └── JwtReactiveAuthenticationManager.java  # autentica via JwtService
├── model/
│   ├── JwtClaims.java                     # record — claims extraídas do token
│   ├── RateLimitKey.java                  # record — chave + flag "autenticado" do rate limit
│   └── ServiceRouteDefinition.java        # record — id/predicado/uri de cada rota downstream
├── exception/
│   └── GatewayExceptionHandler.java       # ErrorWebExceptionHandler — erros sempre em JSON
└── config/
    ├── SecurityConfig.java                # @EnableWebFluxSecurity + AuthenticationWebFilter
    ├── GatewayRoutesConfig.java           # RouteLocator (Java DSL) — 8 rotas downstream
    ├── ServiceRouteFactory.java           # monta cada rota com circuit breaker + fallback
    └── CorsConfig.java                    # CorsWebFilter com credentials
```

`JwtService` e `RateLimitService` são `@Service` Spring-gerenciados diretamente, sem porta/
interface intermediária (diferente do Hexagonal usado em Inventory/Order Service).

## Fluxo de uma requisição

1. `CorsWebFilter` (config/CorsConfig.java) trata preflight OPTIONS e valida a origem contra
   `ALLOWED_ORIGINS`. Origem fora da lista não recebe `Access-Control-Allow-Origin` na resposta —
   o browser bloqueia a chamada antes mesmo dela chegar à aplicação.
2. `AuthenticationWebFilter` (config/SecurityConfig.java) tenta extrair o JWT do cookie
   `access_token` via `JwtServerAuthenticationConverter`. Se não houver cookie, segue anônimo.
3. Se houver token, `JwtReactiveAuthenticationManager` chama `JwtService.validate(token)`
   (JJWT — `Jwts.parser().verifyWith(signingKey)...`). Token ausente, expirado, malformado ou com
   assinatura inválida vira `BadCredentialsException`, tratada por
   `SecurityConfig#handleUnauthorized` → HTTP 401 sem repassar a requisição ao serviço downstream.
4. `SecurityConfig#securityWebFilterChain` decide se o path exige autenticação
   (`PUBLIC_ENDPOINTS` vs `anyExchange().authenticated()`).
5. `RateLimitFilter` (GlobalFilter, `Ordered.HIGHEST_PRECEDENCE + 10`) calcula a chave do rate
   limit — `usuario:path` se autenticado, `ip:path` se anônimo — e chama
   `RateLimitService.isAllowed`. Acima do limite, responde 429 com header `Retry-After: 60` sem
   encaminhar a requisição.
6. Se passou por todos os filtros, o `RouteLocator` (config/GatewayRoutesConfig.java +
   ServiceRouteFactory) encaminha a requisição ao serviço downstream configurado, com circuit
   breaker Resilience4j por rota; se o serviço estiver fora do ar, o fallback
   (`FallbackController`) responde 503.

## JWT via cookie httpOnly

O Gateway **não** lê `Authorization: Bearer`. O token é lido exclusivamente do cookie httpOnly
`access_token`, setado pelo Auth Service no login/refresh. Essa decisão é a mesma documentada em
`docs/integration/frontend-backend-integration.md`: evita expor o token a JavaScript no browser
(mitiga XSS) e mantém o fluxo de autenticação transparente para o client HTTP do frontend
(`withCredentials: true`).

`JwtService` usa HMAC-SHA (chave simétrica `jwt.secret`, base64, mesma configurada no Auth
Service) e devolve um `JwtClaims` (`userId`, `email`, `roles`) a partir do `subject` e dos claims
customizados do token.

## Rate Limiting

Implementado em `RateLimitService` com Redis reativo (`ReactiveStringRedisTemplate`):

- Chave: `ratelimit:{ip-ou-userId}:{path}`.
- `INCR` atômico no Redis; na primeira requisição da janela (`count == 1`), define TTL de 60s.
- Limite: 100 req/min para requisições anônimas, 200 req/min para autenticadas.
- Acima do limite, `RateLimitFilter` devolve 429 antes de a requisição alcançar o roteamento.

## CORS

`CorsConfig` registra um `CorsWebFilter` global: `Access-Control-Allow-Origin` é sempre uma origem
explícita da lista `ALLOWED_ORIGINS` (nunca `*`, que é incompatível com `Access-Control-Allow-
Credentials: true`). Origem fora da lista não recebe o header de allow-origin.

## Circuit Breaker e Fallback

Cada rota criada por `ServiceRouteFactory` recebe um filtro `circuitBreaker` (Resilience4j,
`spring-cloud-starter-circuitbreaker-reactor-resilience4j`) com `fallbackUri` apontando para
`/fallback/{serviço}`. Quando o serviço downstream está fora do ar (ou o circuito abre por excesso
de falhas), `FallbackController` responde 503 com um corpo JSON explicando qual serviço está
indisponível.

## Rotas configuradas

| Rota | Predicado | Destino (`application.yml`) |
|---|---|---|
| user-service | `/api/v1/users/**` | `services.user.url` |
| auth-service | `/api/v1/auth/**` | `services.auth.url` |
| catalog-service | `/api/v1/catalog/**` | `services.catalog.url` |
| search-service | `/api/v1/search/**` | `services.search.url` (pós-MVP) |
| cart-service | `/api/v1/cart/**` | `services.cart.url` |
| order-service | `/api/v1/orders/**` | `services.order.url` |
| payment-service | `/api/v1/payments/**` | `services.payment.url` |
| analytics-service | `/api/v1/analytics/**` | `services.analytics.url` (pós-MVP) |

## Endpoints públicos (sem JWT)

```
/actuator/**, /api-docs/**, /swagger-ui/**, /swagger-ui.html
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
POST /api/v1/users            (cadastro — exato, não wildcard)
GET  /api/v1/catalog/**
GET  /api/v1/search/**
/fallback/**, /fallback
```

Todos os demais endpoints — inclusive `GET /api/v1/users/{id}` — exigem cookie `access_token`
válido.

## Variáveis de Ambiente

```
JWT_SECRET=<chave-base64-256bit>
JWT_EXPIRATION_MS=3600000
REDIS_HOST=redis
REDIS_PORT=6379
ALLOWED_ORIGINS=http://localhost:3000,https://autohubstore.com
AUTH_SERVICE_URL, USER_SERVICE_URL, CATALOG_SERVICE_URL, SEARCH_SERVICE_URL,
CART_SERVICE_URL, ORDER_SERVICE_URL, PAYMENT_SERVICE_URL, ANALYTICS_SERVICE_URL
```

Todas com default local em `application.yml` (`${VARIAVEL:valor-default}`).

## Testes

### Unitários (`src/test/java/.../service/`)

- `JwtServiceTest` — token válido, expirado (`ExpiredJwtException`), assinatura inválida
  (`SignatureException`), malformado (`MalformedJwtException`) e ausente
  (`IllegalArgumentException`).
- `RateLimitServiceTest` — primeira requisição da janela define TTL, requisição dentro do limite
  público/autenticado é permitida, requisição acima do limite é bloqueada, nova janela após
  expiração do TTL reinicia o contador. `ReactiveStringRedisTemplate` mockado com Mockito;
  asserções via `StepVerifier` (Reactor Test).

### Aceitação (Cucumber — `src/test/resources/features/api-gateway.feature`)

Cobre os 5 cenários da tabela "Critérios de Aceite" de
[docs/planning/specs/01-api-gateway.md](../planning/specs/01-api-gateway.md):

1. Roteamento com sucesso (200, via stub WireMock do user-service).
2. Endpoint protegido sem cookie → 401.
3. Token expirado ou com assinatura inválida → 401 (`Scenario Outline`, 2 exemplos).
4. Rate limit excedido → 429 após 100 requisições na mesma janela.
5. CORS de origem não permitida → resposta sem `Access-Control-Allow-Origin`.

Contexto de teste (`acceptance/CucumberSpringConfiguration.java`): `@SpringBootTest` com contexto
reativo real, Redis via Testcontainers (`redis:7-alpine`, porta dinâmica via
`@DynamicPropertySource`) e o user-service stubado com WireMock (`acceptance/WireMockSupport`) na
mesma técnica. Os demais serviços downstream permanecem com a URL padrão de teste
(`http://localhost:19999`, inalcançável) porque os cenários cobertos não dependem deles.

### Como rodar

```bash
cd backend/api-gateway
mvn validate       # checkstyle (inclui src/test)
mvn test           # unitários + aceitação Cucumber (JUnit Platform Suite)
mvn verify          # idem + gate de cobertura Jacoco (mínimo 70% de linha)
mvn jacoco:report   # relatório HTML em target/site/jacoco/index.html
mvn package         # build final (target/api-gateway.jar)
snyk test --all-sub-projects --detection-depth=6
```

Pré-requisito para os testes de aceitação: Docker Desktop em execução (Testcontainers sobe um
Redis efêmero). Nenhum comando de teste depende de `docker compose up` — a infra do
`docker-compose.yml` do projeto não precisa estar de pé.

### Evidências da última execução

- `mvn test`: 17 testes (11 unitários + 6 cenários Cucumber), 0 falhas.
- `mvn verify` (Jacoco `check`, regra `LINE` `COVEREDRATIO` ≥ 0.70): `All coverage checks have
  been met.` — cobertura de linha ≈ 75% (112/149 linhas, `target/site/jacoco/jacoco.csv`).
- `mvn package`: `BUILD SUCCESS`, gera `target/api-gateway.jar`.
- `snyk test --all-sub-projects --detection-depth=6`: `Tested 161 dependencies for known issues,
  no vulnerable paths found.` (`ok: true`).
- `mvn validate` (checkstyle, `includeTestSourceDirectory=true`): `0 Checkstyle violations.`

## Dependências relevantes (pom.xml)

Spring Boot `4.0.8` (parent) + Spring Cloud `2025.1.3`, Spring Cloud Gateway Server WebFlux,
Spring Security (reativo), Spring Data Redis Reactive, JJWT `0.13.0`, Resilience4j (circuit
breaker), Springdoc OpenAPI WebFlux. Testes: JUnit 5 (Jupiter/Platform 6.0.3), Mockito, Reactor
Test, Cucumber `7.33.0` (`cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`),
JUnit Platform Suite, Testcontainers `2.0.3`, WireMock Standalone `3.13.0`.

`version` do artefato: `1.0.0` (convenção obrigatória do CLAUDE.md para todo serviço do MVP).

### Nota sobre Jackson (Spring Boot 4)

O Spring Boot 4 migrou a serialização interna para o novo Jackson 3 (`tools.jackson.*`), mas o
`jjwt-jackson` (dependência do JJWT) ainda usa o Jackson 2 clássico (`com.fasterxml.jackson.*`) —
por isso o projeto convive com as duas linhas no classpath. Ambas tiveram `jackson-databind`
pinado via `dependencyManagement` (`com.fasterxml.jackson.core:jackson-databind:2.21.6` e
`tools.jackson.core:jackson-databind:3.1.6`) para corrigir CVEs de desserialização/reflection
reportadas pelo Snyk nas versões trazidas por padrão pelo BOM.

## Limitações conhecidas

- `GatewayExceptionHandler` (tratamento de erro genérico, fora dos casos 401/403 já cobertos pelos
  filtros de segurança) não tem teste unitário dedicado — os cenários de aceitação já exercitam os
  caminhos 401/429 mais relevantes; a cobertura de linha do projeto (≈75%) já atende ao critério de
  conclusão (≥70%) mesmo com essa classe zerada.
- Os cenários de aceitação de roteamento usam WireMock apenas para o `user-service`; os demais 7
  serviços downstream não são exercitados nos testes de aceitação deste serviço (ficam cobertos
  quando cada um deles for implementado e testado individualmente).
