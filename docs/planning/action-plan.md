# AutoHubStore — Plano de Ação de Microserviços

## Visão Geral

O AutoHubStore é um e-commerce automotivo construído com arquitetura de microsserviços.
São **10 microsserviços** em **Java 25** (LTS), seguindo diferentes arquiteturas para fins educacionais.

**Repositório:** monorepo em `autohub-store/`  
**Backend:** `backend/<nome-do-servico>/`  
**Infraestrutura:** `infra/`

---

## Stack Tecnológica Global

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | **25** (LTS) | Linguagem de todos os serviços |
| Spring Boot | 3.x | Framework base |
| Maven | 3.9+ | Build tool (5 serviços) |
| Gradle | 8+ | Build tool (5 serviços) |
| Spring Cloud | 2023.x | Gateway, OpenFeign, Circuit Breaker |
| Spring Security | 6.x | Autenticação e autorização |
| JJWT | 0.12+ | JWT (Auth Service) |
| PostgreSQL | 16 | Persistência relacional |
| Flyway | 9+ | Migrações de banco |
| Redis | 7 | Cache, carrinho, blacklist |
| Apache Kafka | 3.6+ | Mensageria assíncrona |
| Elasticsearch | 8.x | Busca full-text |
| Cassandra | 4.x | Analytics (alta escrita) |
| OpenFeign | Spring Cloud | Chamadas REST entre serviços |
| Resilience4j | 2.x | Circuit Breaker, Retry |
| OpenTelemetry | 1.x | Traces distribuídos |
| Micrometer | 1.x | Métricas (Prometheus) |
| Testcontainers | 1.19+ | Testes de integração |
| Springdoc OpenAPI | 2.x | Swagger UI |
| Docker | 24+ | Containerização |
| Docker Compose | 2.x | Ambiente local |

---

## Qualidade de Código — Checkstyle

Todos os microsserviços do monorepo **devem** apontar para o arquivo de regras compartilhado em:

```
infra/checkstyle/checkstyle.xml
```

O arquivo contém regras universais de estilo (limite de linha, imports, complexidade, nomenclatura, boas práticas) que se aplicam igualmente a todas as arquiteturas (MVC, Clean, Hexagonal) e ambos os build tools. O build **falha** em qualquer violação (`failsOnError = true`).

### Configuração obrigatória — Maven (api-gateway, auth-service, user-service, order-service, payment-service)

Adicionar nas `<properties>` e no bloco `<build><plugins>` do `pom.xml`:

```xml
<!-- Em <properties> -->
<checkstyle.version>10.21.0</checkstyle.version>
<maven-checkstyle-plugin.version>3.5.0</maven-checkstyle-plugin.version>
<checkstyle.config.location>${project.basedir}/../../infra/checkstyle/checkstyle.xml</checkstyle.config.location>

<!-- Em <build><plugins> -->
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
        <includeTestSourceDirectory>false</includeTestSourceDirectory>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle-validate</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Configuração obrigatória — Gradle (catalog-service, search-service, cart-service, notification-service, analytics-service)

Adicionar no `build.gradle`:

```groovy
plugins {
    // adicionar ao bloco de plugins existente
    id 'checkstyle'
}

checkstyle {
    toolVersion = '10.21.0'
    configFile = rootProject.file('infra/checkstyle/checkstyle.xml')
    ignoreFailures = false
    showViolations = true
    sourceSets = [sourceSets.main] // não aplica nos testes
}
```

> **Regra:** nenhum microsserviço deve ter seu próprio `checkstyle.xml` local. Toda alteração nas regras de qualidade deve ser feita exclusivamente em `infra/checkstyle/checkstyle.xml` e propagará automaticamente para todos os serviços.

> **Código gerado deve nascer em conformidade:** todo código Java (classes, controllers, entidades, exceções, etc.) já deve ser escrito seguindo `infra/checkstyle/checkstyle.xml` desde a primeira versão — sem números mágicos, com linha em branco após abertura e antes do fechamento de cada classe, sem exceções genéricas, campos sempre `private`, etc. Ver resumo em [CLAUDE.md § Checkstyle](../../CLAUDE.md#checkstyle--obrigatório-em-todo-código-gerado). Corrigir depois gera retrabalho e não é o padrão aceito.

---

## Distribuição de Build Tools e Arquiteturas

| # | Serviço | Build Tool | Arquitetura |
|---|---|---|---|
| 0 | API Gateway | **Maven** | **Hexagonal** |
| 1 | Auth Service | **Maven** | **Clean Architecture** |
| 2 | User Service | **Maven** | MVC |
| 3 | Catalog Service | **Gradle** | MVC |
| 4 | Search Service | **Gradle** | MVC |
| 5 | Cart Service | **Gradle** | MVC |
| 6 | Order Service | **Maven** | **Hexagonal** |
| 7 | Payment Service | **Maven** | MVC |
| 8 | Notification Service | **Gradle** | MVC |
| 9 | Analytics Service | **Gradle** | MVC |

> **Objetivo educacional:** A divisão de build tools permite aprender tanto Maven quanto Gradle em contexto real. A variação de arquiteturas demonstra Clean Architecture, Hexagonal (Ports & Adapters) e MVC no mesmo projeto.

---

## Padrões de Pacotes por Arquitetura

### MVC (User, Catalog, Search, Cart, Payment, Notification, Analytics)

```
com.autohubstore.<servicename>/
├── controller/      # @RestController
├── service/         # @Service — lógica de negócio
├── repository/      # @Repository — acesso a dados
├── model/           # Entidades JPA + DTOs + Enums
├── exception/       # Exceções + @ControllerAdvice
├── messaging/       # Kafka producers/consumers
└── config/          # @Configuration
```

### Clean Architecture — Auth Service

```
com.autohubstore.authservice/
├── domain/
│   ├── model/          # Entidades e Value Objects
│   ├── event/          # Domain Events
│   ├── repository/     # Interfaces (output boundary)
│   └── service/        # Domain Services
├── application/
│   ├── usecase/        # Use Cases (input boundary)
│   ├── dto/            # DTOs
│   └── mapper/         # Mappers
└── infrastructure/
    ├── web/            # Controllers REST
    ├── persistence/    # Implementações JPA
    ├── messaging/      # Kafka producers
    └── config/         # Spring config
```

### Hexagonal — API Gateway e Order Service (Ports & Adapters)

```
com.autohubstore.gateway/
├── domain/
│   ├── model/           # JwtClaims (Value Object)
│   ├── service/         # JwtValidationService, RateLimitDomainService (sem @Service)
│   └── port/
│       ├── in/          # ValidateTokenUseCase, CheckRateLimitUseCase
│       └── out/         # RateLimitPort
└── adapter/
    ├── config/          # DomainConfig — instancia beans do domínio
    ├── in/web/          # SecurityConfig, GatewayRoutesConfig, CorsConfig,
    │                    # RateLimitFilter, FallbackController
    └── out/
        ├── redis/       # RateLimitRedisAdapter
        └── web/         # GatewayExceptionHandler
```

### Hexagonal — Order Service (Ports & Adapters)

```
com.autohubstore.orderservice/
├── domain/
│   ├── model/           # Entidades, VOs, Enums
│   ├── service/         # Domain services
│   └── port/
│       ├── in/          # Driving ports (interfaces de use case)
│       └── out/         # Driven ports (interfaces de saída)
└── adapter/
    ├── in/
    │   ├── web/         # Controllers REST
    │   └── messaging/   # Kafka consumers
    └── out/
        ├── persistence/ # JPA repositories
        └── messaging/   # Kafka producers
```

---

## Mapa de Microsserviços

| # | Serviço | Porta | Banco | Kafka | Status |
|---|---|---|---|---|---|
| 0 | API Gateway | 8001 | Redis (rate limit) | — | Implementado |
| 1 | Auth Service | 8002 | PostgreSQL (`auth_db`) + Redis | Producer | Em implementação |
| 2 | User Service | 8003 | PostgreSQL (`user_db`) | Producer | Em implementação |
| 3 | Catalog Service | 8004 | PostgreSQL (`catalog_db`) + Redis | Producer | Em implementação |
| 4 | Search Service | 8005 | Elasticsearch | Consumer | Planejado |
| 5 | Cart Service | 8006 | Redis | — | Planejado |
| 6 | Order Service | 8007 | PostgreSQL (`order_db`) | Producer + Consumer | Planejado |
| 7 | Payment Service | 8008 | PostgreSQL (`payment_db`) | Producer | Planejado |
| 8 | Notification Service | 8009 | — (stateless) | Consumer | Planejado |
| 9 | Analytics Service | 8010 | Cassandra | Consumer | Planejado |

> **Nomes de banco:** seguem exatamente `POSTGRES_DB` já definido em `infra/docker-compose.yml` (`auth_db`, `user_db`, `catalog_db`, `order_db`, `payment_db`) — evita erro `FATAL: database "..." does not exist` no Flyway ao subir serviço local contra a infra do compose.

---

## Mapa de Dependências

```
API Gateway ─────────────────────────────→ Todos os serviços (roteamento)
Cart Service ──── OpenFeign ─────────────→ Catalog Service
Order Service ─── OpenFeign ─────────────→ Cart Service, User Service
Search Service ←── Kafka ─────────────────── Catalog Service (product.created/updated)
Notification  ←─── Kafka ─────────────────── Auth, User, Order, Payment
Analytics     ←─── Kafka ─────────────────── Catalog (product-viewed), Order (order.created)
Payment ──────── Kafka ──────────────────→ Order Service (payment.approved/rejected)
Order ←────────── Kafka ─────────────────── Payment Service
```

---

## Tópicos Kafka

| Tópico | Producer | Consumer |
|---|---|---|
| `user.created` | User Service | Notification Service |
| `auth.password-reset` | Auth Service | Notification Service |
| `catalog.product-created` | Catalog Service | Search Service |
| `catalog.product-updated` | Catalog Service | Search Service |
| `catalog.product-viewed` | Catalog Service | Analytics Service |
| `order.created` | Order Service | Notification Service, Analytics Service |
| `order.status-changed` | Order Service | — |
| `payment.approved` | Payment Service | Order Service, Notification Service |
| `payment.rejected` | Payment Service | Order Service, Notification Service |

---

## Infraestrutura Local (docker-compose)

Arquivo: `infra/docker-compose.yml`

| Serviço | Porta | Descrição |
|---|---|---|
| postgres-auth | 5432 | Banco do Auth Service |
| postgres-user | 5433 | Banco do User Service |
| postgres-catalog | 5434 | Banco do Catalog Service |
| postgres-order | 5435 | Banco do Order Service |
| postgres-payment | 5436 | Banco do Payment Service |
| redis | 6379 | Cache + Carrinho + Blacklist |
| zookeeper | 2181 | Kafka coordinator |
| kafka | 9092 | Message broker |
| elasticsearch | 9200 | Full-text search |
| kibana | 5601 | UI do Elasticsearch |
| cassandra | 9042 | Analytics |
| prometheus | 9090 | Métricas |
| grafana | 3011 | Dashboards |
| jaeger | 16686 | Traces distribuídos |
| mailhog | 8025 | SMTP local (testes de e-mail) |

---

## Ordem de Desenvolvimento (10 Fases)

### Fase 1 — Fundação e Infraestrutura

**Objetivo:** Ambiente de dev completo funcionando localmente.

**Entregas:**
1. `infra/docker-compose.yml` com todos os serviços de infra
2. Templates Spring Boot: um Maven (MVC) + um Gradle (MVC)
3. Configuração Flyway nos templates
4. Pipeline CI/CD básico (GitHub Actions: build + test)
5. README com instruções de setup

**Critério de conclusão:** `docker compose up` sobe toda infra sem erros; ambos os templates compilam e testam.

**Microsserviço criado:** API Gateway (Maven + MVC)  
**Spec:** [docs/microservices/01-api-gateway.md](microservices/01-api-gateway.md)

---

### Fase 2 — Autenticação e Usuários

**Objetivo:** Fluxo completo de cadastro, login, JWT e gestão de perfil.

**Entregas:**
1. **Auth Service** (Maven + Clean Architecture) — login, logout, refresh, reset senha
2. **User Service** (Maven + MVC) — cadastro, perfil, endereços
3. JWT com claims customizados, validação no Gateway
4. Blacklist de tokens no Redis
5. Evento `user.created` publicado no Kafka
6. Testes unitários e de integração (Testcontainers)
7. Swagger em ambos

**Critério de conclusão:** Fluxo cadastro → login → refresh → logout funcionando; reset de senha via MailHog.

**Specs:**
- Auth Service → [docs/microservices/02-auth-service.md](microservices/02-auth-service.md)
- User Service → [docs/microservices/03-user-service.md](microservices/03-user-service.md)

---

### Fase 2.5 — Integração Frontend ↔ Backend (GATE — bloqueia Fase 3 em diante)

**Objetivo:** Frontend deixa de ser mock e passa a consumir Auth Service, User Service e API
Gateway reais, com todos os contratos de integração definidos e implementados.

> **Regra:** nenhum microsserviço novo (Fase 3 em diante) deve ser criado antes desta fase estar
> concluída. Detalhes completos, decisões e contrato de integração em
> [docs/integration/frontend-backend-integration.md](../integration/frontend-backend-integration.md).

**Entregas:**
1. MinIO na infra (`infra/docker-compose.yml`) + bucket `catalog-images`
2. Client HTTP central no frontend (axios + interceptor de refresh + `withCredentials`)
3. React Query adotado para estado server-side (`useAuth`, `useProducts`, etc.)
4. Login/logout/refresh reais substituindo `authStore` mock (cookie httpOnly)
5. CORS com credentials configurado no Gateway
6. `types/product.ts` migrado de `id: number` para `id: string` (UUID) em todo o frontend
7. Contrato de erro (RFC 7807) e paginação (Spring `Page`) padronizados e documentados

**Critério de conclusão:** login/logout/refresh funcionando fim a fim via UI real; nenhuma
referência a `id: number` de produto restante no frontend.

---

### Fase 3 — Catálogo e Busca

**Objetivo:** CRUD de produtos com cache e busca full-text.

**Entregas:**
1. **Catalog Service** (Gradle + MVC) — CRUD admin + listagem pública + cache Redis + upload de imagens (MinIO)
2. **Search Service** (Gradle + MVC) — Elasticsearch + Kafka consumer para re-indexação

**Specs:**
- Catalog Service → [docs/microservices/04-catalog-service.md](microservices/04-catalog-service.md)
- Search Service → [docs/microservices/05-search-service.md](microservices/05-search-service.md)

---

### Fase 4 — Carrinho

**Objetivo:** Carrinho persistido no Redis com circuit breaker.

**Entregas:**
1. **Cart Service** (Gradle + MVC) — Redis, OpenFeign + Resilience4j, TTL 7 dias

**Spec:** [docs/microservices/06-cart-service.md](microservices/06-cart-service.md)

---

### Fase 5 — Pedidos

**Objetivo:** Máquina de estados com arquitetura Hexagonal.

**Entregas:**
1. **Order Service** (Maven + Hexagonal) — state machine, Kafka producer+consumer, OpenFeign para Cart e User

**Spec:** [docs/microservices/07-order-service.md](microservices/07-order-service.md)

---

### Fase 6 — Pagamentos

**Objetivo:** Simulação de pagamento com eventos Kafka.

**Entregas:**
1. **Payment Service** (Maven + MVC) — simulação 70/30, idempotência, eventos Kafka

**Spec:** [docs/microservices/08-payment-service.md](microservices/08-payment-service.md)

---

### Fase 7 — Notificações

**Objetivo:** E-mails reativos a eventos com retry e DLT.

**Entregas:**
1. **Notification Service** (Gradle + MVC) — Thymeleaf, DLT, retry com backoff exponencial

**Spec:** [docs/microservices/09-notification-service.md](microservices/09-notification-service.md)

---

### Fase 8 — Analytics

**Objetivo:** Métricas de produto e dashboard admin via Cassandra.

**Entregas:**
1. **Analytics Service** (Gradle + MVC) — Cassandra counters, Kafka consumer, dashboard API

**Spec:** [docs/microservices/10-analytics-service.md](microservices/10-analytics-service.md)

---

### Fase 9 — Observabilidade

**Objetivo:** Traces, métricas e logs em todos os serviços.

**Entregas:**
1. OpenTelemetry agent em todos os serviços
2. Traces visíveis no Jaeger
3. Métricas Prometheus exportadas
4. Dashboards Grafana por serviço
5. Logs JSON estruturados → ELK Stack

---

### Fase 10 — Deploy e Produção

**Objetivo:** Deploy no VPS Hostinger + manifests Kubernetes.

**Entregas:**
1. `docker-compose.prod.yml`
2. Manifests Kubernetes (`k8s/`: Deployments, Services, ConfigMaps, Secrets)
3. Pipeline CI/CD completo (build → test → push image → deploy)
4. HTTPS com Let's Encrypt (Certbot + NGINX)

---

## Critérios de Aceite do MVP

1. **Happy Path:** Cadastro → Login → Busca → Carrinho → Pedido → Pagamento aprovado → E-mail de confirmação
2. **Rejeição:** Pagamento rejeitado → E-mail notificado → Pedido `CANCELLED`
3. **Admin Analytics:** Dashboard com top produtos vistos/vendidos
4. **Resiliência:** Circuit breaker entre Cart e Catalog funcionando
5. **Observabilidade:** Trace completo visível no Jaeger
