# API Gateway

**Build Tool:** Maven | **Arquitetura:** MVC | **Porta:** 8001 | **Status:** Concluído

**DER:** [docs/planning/der/api-gateway.mmd](../der/api-gateway.mmd) (sem persistência própria —
diagrama documenta estruturas em memória/Redis: `JwtClaims`, `RateLimitKey`, rotas)

## Objetivo

Ponto de entrada único do AutoHubStore. Roteia requisições para os microsserviços, valida JWT centralizadamente, aplica rate limiting por IP/usuário e configura CORS para o frontend.

## PRD Resumido

Sem um ponto de entrada único, cada um dos 9 microsserviços precisaria implementar sua própria
validação de JWT, rate limiting e CORS — duplicação de lógica de segurança e superfície de ataque
maior. O Gateway resolve isso concentrando essas responsabilidades em um único lugar auditável.
Usado indiretamente por todo cliente final (via frontend Next.js) e por qualquer chamada
administrativa à API; os demais 8 microsserviços do MVP dependem dele como única porta de entrada
externa. Valor de negócio: reduz retrabalho de segurança por serviço, protege a infraestrutura
contra abuso de tráfego e permite trocar/escalar serviços internos sem o cliente perceber mudança
de endereço.

## Use Cases

- Como cliente final, quero que minhas requisições sejam roteadas automaticamente ao microsserviço
  correto, para não precisar conhecer a topologia interna do sistema.
- Como cliente autenticado, quero que meu JWT seja validado antes de chegar ao serviço de destino,
  para que endpoints protegidos rejeitem acesso não autorizado de forma centralizada.
- Como frontend Next.js, quero que o Gateway repasse o `Set-Cookie` do Auth Service, para manter o
  fluxo de login/refresh/logout transparente ao meu client HTTP.
- Como operador da plataforma, quero rate limiting por IP e por usuário autenticado, para proteger
  os microsserviços contra abuso de tráfego e picos de requisições.
- Como frontend hospedado em outra origem, quero que o Gateway aplique CORS com credentials
  corretamente, para autenticar via cookie httpOnly em requisições cross-origin.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Roteamento com sucesso | Gateway no ar e User Service saudável | Cliente chama `GET /api/v1/users/{id}` com cookie `access_token` válido | Gateway roteia para `user-service` e retorna a resposta original com status 200 |
| Endpoint protegido sem token | Requisição não envia cookie `access_token` | Cliente chama endpoint protegido | Gateway retorna 401 sem encaminhar a requisição ao serviço downstream |
| Token expirado ou inválido | Cookie `access_token` expirado ou com assinatura inválida | Requisição chega a endpoint protegido | Gateway retorna 401 |
| Rate limit excedido | 100 requisições/min já realizadas pelo mesmo IP em endpoint público | A 101ª requisição chega dentro da mesma janela de 60s | Gateway retorna 429 |
| CORS de origem não permitida | Origem do request não está em `ALLOWED_ORIGINS` | Requisição de preflight CORS chega ao Gateway | Gateway não inclui `Access-Control-Allow-Origin` correspondente na resposta, bloqueando a chamada no browser |

## Responsabilidades

- Roteamento de requisições para todos os 9 microsserviços
- Validação centralizada de JWT lido do cookie httpOnly `access_token` (sem delegar aos serviços downstream)
- Repasse transparente do `Set-Cookie` retornado por Auth Service (login/refresh/logout)
- Rate Limiting por IP e por usuário autenticado (Redis)
- Configuração de CORS com credentials para o frontend Next.js
- Headers de segurança (X-Content-Type-Options, X-Frame-Options)
- Load balancing via Spring Cloud LoadBalancer

> Estratégia de token (cookie httpOnly), CORS com credentials e contrato de erro/paginação
> detalhados em [docs/integration/frontend-backend-integration.md](../../integration/frontend-backend-integration.md).

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework base |
| Spring Cloud Gateway | 2023.x | Gateway reativo (WebFlux) |
| Spring Security | 6.x | Validação JWT |
| JJWT | 0.12+ | Parse e validação de JWT |
| Spring Data Redis | 3.x | Rate limiting |
| Micrometer + Prometheus | 1.x | Métricas |
| OpenTelemetry Agent | 1.x | Traces distribuídos |
| Springdoc OpenAPI | 2.x | Documentação |

## Dependências Maven (pom.xml)

```xml
<!-- <project> -->
<version>1.0.0</version>

<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Estrutura de Pacotes (MVC)

```
com.autohubstore.gateway/
├── GatewayApplication.java
├── controller/
│   └── FallbackController.java            # @RestController — respostas 503
├── service/
│   ├── JwtService.java                    # @Service — valida/parseia JWT (JJWT)
│   └── RateLimitService.java              # @Service — rate limit via Redis reativo
├── filter/
│   ├── RateLimitFilter.java               # GlobalFilter — chama RateLimitService
│   ├── JwtServerAuthenticationConverter.java  # Extrai token do cookie httpOnly
│   └── JwtReactiveAuthenticationManager.java  # Autentica via JwtService
├── model/
│   ├── JwtClaims.java                     # Claims do token JWT
│   ├── RateLimitKey.java                  # Chave + flag autenticado do rate limit
│   └── ServiceRouteDefinition.java        # Definição de rota por serviço downstream
├── exception/
│   └── GatewayExceptionHandler.java       # ErrorWebExceptionHandler — erros em JSON
└── config/
    ├── SecurityConfig.java                # @EnableWebFluxSecurity + AuthenticationWebFilter
    ├── GatewayRoutesConfig.java           # RouteLocator (Java DSL)
    ├── ServiceRouteFactory.java           # Monta rota + circuit breaker por ServiceRouteDefinition
    └── CorsConfig.java                    # CorsWebFilter
```

> Gateway não tem JPA/Kafka, então adapta o padrão MVC dos demais serviços: `filter/` no
> lugar de `repository`/`messaging`, para os `GlobalFilter`/security filters do Spring Cloud
> Gateway. `JwtService` e `RateLimitService` são `@Service` Spring-gerenciados de verdade
> (sem porta/interface intermediária).

## Configuração de Rotas (application.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates: [Path=/api/v1/auth/**]
        - id: user-service
          uri: lb://user-service
          predicates: [Path=/api/v1/users/**]
        - id: catalog-service
          uri: lb://catalog-service
          predicates: [Path=/api/v1/catalog/**]
        - id: search-service
          uri: lb://search-service
          predicates: [Path=/api/v1/search/**]
        - id: cart-service
          uri: lb://cart-service
          predicates: [Path=/api/v1/cart/**]
        - id: order-service
          uri: lb://order-service
          predicates: [Path=/api/v1/orders/**]
        - id: payment-service
          uri: lb://payment-service
          predicates: [Path=/api/v1/payments/**]
        - id: analytics-service
          uri: lb://analytics-service
          predicates: [Path=/api/v1/analytics/**]
```

## Endpoints Públicos (sem JWT)

```
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/users                    # Cadastro
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
GET  /api/v1/catalog/**               # Listagem e detalhes de produtos
GET  /api/v1/search/**                # Busca
```

## Endpoints Protegidos

Todos os demais endpoints exigem cookie `access_token` httpOnly (setado no login/refresh) —
Gateway extrai o JWT do cookie, não de header `Authorization`.

## CORS

- `Access-Control-Allow-Origin`: origin explícita da lista `ALLOWED_ORIGINS` (nunca `*`, incompatível com credentials)
- `Access-Control-Allow-Credentials: true`
- Frontend faz requests com `withCredentials:true`/`credentials:'include'`

## Rate Limiting

- Por IP: 100 req/min para endpoints públicos
- Por usuário: 200 req/min para endpoints autenticados
- Chave Redis: `ratelimit:{ip}:{endpoint}` e `ratelimit:{userId}:{endpoint}`
- TTL: 60 segundos

## Variáveis de Ambiente

```
JWT_SECRET=<chave-base64-256bit>
JWT_EXPIRATION_MS=3600000
REDIS_HOST=redis
REDIS_PORT=6379
ALLOWED_ORIGINS=http://localhost:3000,https://autohubstore.com
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/api-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Checkstyle

> **Código deve nascer conforme:** escrever classes já seguindo `infra/checkstyle/checkstyle.xml`
> (linha em branco após `{` de abertura e antes do `}` de fechamento da classe, sem números mágicos,
> sem exceções/catches genéricos, campos `private`, etc. — resumo em
> [CLAUDE.md § Checkstyle](../../../CLAUDE.md#checkstyle--obrigatório-em-todo-código-gerado)).
> Não gerar código e corrigir depois.

Apontar para o arquivo compartilhado em `infra/checkstyle/checkstyle.xml`. Adicionar nas `<properties>` e em `<build><plugins>` do `pom.xml`:

```xml
<!-- <properties> -->
<checkstyle.version>10.21.0</checkstyle.version>
<maven-checkstyle-plugin.version>3.5.0</maven-checkstyle-plugin.version>
<checkstyle.config.location>${project.basedir}/../../infra/checkstyle/checkstyle.xml</checkstyle.config.location>

<!-- <build><plugins> -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>${maven-checkstyle-plugin.version}</version>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>${checkstyle.version}</version>
        </dependency>
    </dependencies>
    <configuration>
        <configLocation>${checkstyle.config.location}</configLocation>
        <failsOnError>true</failsOnError>
        <consoleOutput>true</consoleOutput>
        <includeTestSourceDirectory>true</includeTestSourceDirectory>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle-validate</id>
            <phase>validate</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

## Estratégia de Testes

- **Unitários:** JwtService (token válido, expirado, inválido, ausente), RateLimitService
- **Integração:** WebTestClient testando roteamento e respostas de erro (401, 429)
- **Segurança:** Endpoint protegido sem token → 401; rate limit excedido → 429

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação mantida em
`docs/apps/api-gateway.md`.
