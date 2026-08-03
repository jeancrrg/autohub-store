# AutoHubStore — Contexto do Projeto para Claude

## O que é

**AutoHubStore** é um e-commerce automotivo fictício desenvolvido como projeto de estudo avançado em Backend Java. Objetivo: evoluir para nível Pleno I e Pleno II aplicando microsserviços, DDD, Clean Architecture, Event-Driven Architecture e Cloud Native. Toda arquitetura e decisões técnicas são elaboradas com rigor de produto real.

---

## Status Atual (Fase 1 — Fundação)

| Componente | Status |
|---|---|
| API Gateway (`backend/api-gateway/`) | Implementado e funcional |
| Infraestrutura (`infra/docker-compose.yml`) | Implementada |
| Frontend (`apps/frontend/ecommerce/`) | Estruturado com mock data (sem integração com API) |
| Demais 9 microsserviços | Planejados — specs em `docs/planning/specs/` |

**Branch principal de desenvolvimento:** `feature-ecommerce`

---

## Estrutura do Monorepo

```
autohub-store/
├── apps/
│   └── frontend/
│       └── ecommerce/         # Next.js 16 + React 19 + TypeScript + Tailwind
├── backend/
│   └── api-gateway/           # IMPLEMENTADO — Spring Boot 3.x, Java 25, Maven, Hexagonal
│   # (futuros serviços criados em backend/<nome-do-servico>/)
├── infra/
│   ├── docker-compose.yml     # PostgreSQL x5, Redis, Kafka, Elasticsearch, Cassandra + monitoring
│   └── prometheus.yml
├── docs/
│   ├── apps/                  # Documentação técnica e didática de cada app
│   │   └── api-gateway.md     # Explicação detalhada do Gateway (Hexagonal, JWT, rate limit)
│   └── planning/              # Planejamento e specs
│       ├── action-plan.md     # Plano de ação das 10 fases (inclui Decisões de Consolidação)
│       └── specs/             # Specs detalhadas por microsserviço
│           ├── 01-api-gateway.md
│           ├── 02-user-service.md
│           └── ... (03 a 10)
├── README.md
└── CLAUDE.md                  # este arquivo
```

---

## 10 Microsserviços

> Escopo revisado: Auth Service foi fundido em User Service (mesmo bounded context de Identidade)
> e um novo Inventory Service foi extraído do Catalog Service (controle de estoque exige forte
> consistência/reserva, incompatível com o papel de leitura/cache do Catalog). Detalhes e
> justificativa completa em
> [docs/planning/action-plan.md § Decisões de Consolidação](docs/planning/action-plan.md#decisões-de-consolidação).

| # | Serviço | Porta | Banco | Arquitetura | Build | Status |
|---|---|---|---|---|---|---|
| 1 | **API Gateway** | 8001 | Redis (rate limit) | Hexagonal | Maven | Implementado |
| 2 | **User Service** | 8002 | PostgreSQL + Redis | Clean Architecture | Maven | Planejado |
| 3 | **Catalog Service** | 8003 | PostgreSQL + Redis | MVC | Gradle | Planejado |
| 4 | **Search Service** | 8004 | Elasticsearch | MVC | Gradle | Planejado |
| 5 | **Cart Service** | 8005 | Redis | MVC | Gradle | Planejado |
| 6 | **Inventory Service** | 8006 | PostgreSQL | Hexagonal | Maven | Planejado |
| 7 | **Order Service** | 8007 | PostgreSQL | Hexagonal | Maven | Planejado |
| 8 | **Payment Service** | 8008 | PostgreSQL | MVC | Maven | Planejado |
| 9 | **Notification Service** | 8009 | — (stateless) | MVC | Gradle | Planejado |
| 10 | **Analytics Service** | 8010 | Cassandra | MVC | Gradle | Planejado |

> Alternância Maven/Gradle é intencional: objetivo educacional de aprender ambos em contexto real.

---

## Stack Tecnológica Global

### Backend
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 (LTS) | Linguagem de todos os serviços |
| Spring Boot | 3.x | Framework base |
| Spring Cloud | 2023.x | Gateway, OpenFeign, Config |
| Spring Security | 6.x | Autenticação e autorização |
| JJWT | 0.12+ | JWT (User Service + Gateway) |
| PostgreSQL | 16 | Persistência relacional (5 serviços) |
| Flyway | 9+ | Migrações de banco |
| Redis | 7 | Cache, carrinho, blacklist de tokens, rate limit |
| Apache Kafka | 3.6+ | Mensageria assíncrona (eventos de domínio) |
| Elasticsearch | 8.x | Busca full-text (Search Service) |
| Cassandra | 4.x | Analytics (alta taxa de escrita) |
| OpenFeign | Spring Cloud | Chamadas REST síncronas entre serviços |
| Resilience4j | 2.x | Circuit Breaker, Retry, Rate Limiter |
| OpenTelemetry | 1.x | Traces distribuídos |
| Micrometer | 1.x | Métricas (Prometheus) |
| Testcontainers | 1.19+ | Testes de integração |
| Springdoc OpenAPI | 2.x | Swagger UI (todos os serviços) |

### Frontend
| Tecnologia | Versão | Uso |
|---|---|---|
| Next.js | 14+ (App Router) | Framework React SSR/SSG |
| TypeScript | 5+ | Tipagem estática |
| Tailwind CSS | 3+ | Estilização |
| Zustand | 4+ | Estado global (carrinho, auth) |
| React Query | 5+ | Estado server-side |
| Axios | 1+ | Cliente HTTP |

---

## Padrões Arquiteturais por Serviço

### Hexagonal — API Gateway, Inventory Service, Order Service
```
com.autohubstore.<service>/
├── domain/
│   ├── model/       # Value Objects — POJO puro, sem anotações Spring
│   ├── service/     # Domain services — sem @Service, instanciados via @Configuration
│   └── port/
│       ├── in/      # Interfaces de use case (driving ports)
│       └── out/     # Interfaces de saída (driven ports)
└── adapter/
    ├── config/      # @Configuration — instancia beans do domínio (DomainConfig)
    ├── in/web/      # Controllers, Filters, Security
    └── out/         # Implementações de portas (Redis, JPA, Kafka)
```

### Clean Architecture — User Service
```
com.autohubstore.userservice/
├── domain/
│   ├── model/       # Entidades e Value Objects
│   ├── event/       # Domain Events
│   ├── repository/  # Interfaces (output boundary)
│   └── service/     # Domain Services
├── application/
│   ├── usecase/     # Use Cases (input boundary)
│   ├── dto/         # DTOs
│   └── mapper/      # Entity <-> DTO mappers
└── infrastructure/
    ├── web/         # Controllers REST
    ├── persistence/ # JPA repositories
    ├── messaging/   # Kafka producers
    └── config/      # Spring config
```

### MVC — demais 6 serviços (Catalog, Search, Cart, Payment, Notification, Analytics)
```
com.autohubstore.<servicename>/
├── controller/    # @RestController
├── service/       # @Service — lógica de negócio
├── repository/    # @Repository
├── model/         # Entidades JPA + DTOs + Enums
├── exception/     # Exceções + @ControllerAdvice
├── messaging/     # Kafka producers/consumers
└── config/        # @Configuration
```

---

## Convenções de Código

- **Package base:** `com.autohubstore.<servicename>`
- **Domínio isolado:** em Hexagonal/Clean, o domínio não importa nada do Spring
- **DTOs separados de entidades** — nunca expor JPA entity no controller
- **Flyway:** arquivos em `src/main/resources/db/migration/V*.sql`
- **Swagger:** Springdoc OpenAPI 2.x, acessível em `/swagger-ui.html` em todos os serviços
- **Sem @Service no domínio** — instanciado via `@Configuration` (ex: `DomainConfig.java`)
- **Variáveis de ambiente com default local:** `${VARIAVEL:valor-default}` no `application.yml`
- **Virtual Threads:** `spring.threads.virtual.enabled=true` em todos os serviços (Java 25)
- **Log:** sempre `@Slf4j` (Lombok, `lombok.extern.slf4j.Slf4j`) — nunca instanciar `Logger`/`LoggerFactory` manualmente. Usar campo `log` gerado pela anotação (ex: `log.info(...)`, `log.error(...)`). **Nunca usar `@Log4j`/`@Log4j2`** — projeto roda em SLF4J + Logback (padrão Spring Boot), não Log4j.

### Checkstyle — obrigatório em todo código gerado

Todo código Java gerado (novo ou alterado) **deve nascer em conformidade** com
`infra/checkstyle/checkstyle.xml`, compartilhado por todos os microsserviços via
`maven-checkstyle-plugin` (fase `validate`, `failsOnError=true`). Não gerar código e
corrigir depois — aplicar direto ao escrever. Regras principais:

- **Formatação de classes:**
  - Linha em branco logo após a chave `{` de abertura da declaração da classe (antes do primeiro membro).
  - Linha em branco antes da chave `}` de fechamento final da classe.
  - Indentação: 4 espaços por nível, nunca tab (`FileTabCharacter`).
  - Sem espaços em branco no fim de linha; arquivo termina com newline.
  - Linhas com no máximo 130 caracteres.
- **Sem números mágicos:** todo literal numérico fora de `-1, 0, 1, 2` vira `private static final` nomeado (`MagicNumber`).
- **Exceções:**
  - Nunca lançar tipos genéricos (`RuntimeException`, `Exception`, `Throwable`, `Error`) — criar exceção de domínio específica (`IllegalThrows`).
  - Nunca capturar `RuntimeException`, `Error` ou `Throwable` (`IllegalCatch`); catch vazio exige comentário explicando (`EmptyCatchBlock`).
- **Design de classes:** campos de instância sempre `private` (`VisibilityModifier`, exceto `serialVersionUID` e DTOs com Lombok/Jackson/JPA); nunca usar `clone()`/`finalize()`; `equals()` sempre acompanhado de `hashCode()`.
- **Nomenclatura:** classes/interfaces `UpperCamelCase`; métodos, campos, variáveis e parâmetros `lowerCamelCase`; constantes `UPPER_SNAKE_CASE`; pacotes `minúsculo.sem.underscore`.
- **Estruturas de controle:** sempre com chaves `{}`, mesmo de uma linha (`NeedBraces`); sem blocos vazios sem comentário; `switch` sempre com `default` e sem fall-through implícito.
- **Complexidade:** métodos com no máximo 50 linhas, complexidade ciclomática ≤ 10, ≤ 5 `if` aninhados, ≤ 4 níveis de aninhamento, ≤ 4 `return` (≤ 3 se `void`), ≤ 3 exceções em `throws`, ≤ 7 parâmetros.
- **Boas práticas:** sem comparação de String com `==`; usar `"literal".equals(var)`; sem atribuição dentro de expressão (`InnerAssignment`); sem variável local com mesmo nome de campo (`HiddenField`); sem import `*` ou não usado.

Antes de considerar uma classe pronta, revisar mentalmente contra essa lista —
a especificação completa e comentada está em `infra/checkstyle/checkstyle.xml`.

### Padrões de Implementação — obrigatórios em todo código gerado

- **Entities JPA:**
  - Todo campo mapeia coluna explícita via `@Column(name = "...")` — nunca depender do nome inferido pelo Hibernate.
  - Sempre implementar `@PrePersist` e `@PreUpdate` para timestamps de auditoria (`createdAt`, `updatedAt`) — nunca delegar isso à aplicação/service.
- **Services:**
  - Injeção de dependência sempre via `@RequiredArgsConstructor` (Lombok) com campos `private final` — nunca `@Autowired` em campo ou construtor manual.
  - Um `Service` só pode chamar outro `Service` (ou `Repository` do próprio domínio) — nunca acessar `Repository` de outro serviço/domínio diretamente, e nunca acessar `Controller`.
- **Lombok:** usar para reduzir boilerplate (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Builder`, `@Slf4j`, etc.) em entities, DTOs e services — nunca escrever getters/setters/construtores manuais quando Lombok resolve.
- **MapStruct:** toda conversão Entity ↔ DTO usa `@Mapper` de MapStruct — nunca mapeamento manual campo a campo em service ou controller.
- **Validações em Request DTOs:** toda anotação Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Email`, etc.) sempre com `message` explícita — nunca deixar mensagem default do framework.

---

## Tópicos Kafka

| Tópico | Producer | Consumer(s) |
|---|---|---|
| `user.created` | User Service | Notification Service |
| `user.password-reset` | User Service | Notification Service |
| `catalog.product-created` | Catalog Service | Search Service, Inventory Service |
| `catalog.product-updated` | Catalog Service | Search Service |
| `catalog.product-viewed` | Catalog Service | Analytics Service |
| `order.created` | Order Service | Notification Service, Analytics Service, Inventory Service |
| `order.status-changed` | Order Service | — |
| `payment.approved` | Payment Service | Order Service, Notification Service, Inventory Service |
| `payment.rejected` | Payment Service | Order Service, Notification Service, Inventory Service |
| `inventory.stock-insufficient` | Inventory Service | Order Service |

---

## Mapa de Dependências entre Serviços

```
API Gateway ──────────────────────────────────────→ Todos os serviços (roteamento + JWT)
Cart Service ────── OpenFeign ────────────────────→ Catalog Service
Catalog Service ─── OpenFeign ────────────────────→ Inventory Service (disponibilidade)
Order Service ───── OpenFeign ────────────────────→ Cart Service, User Service
Search Service ←─── Kafka (product.created/updated) ─ Catalog Service
Inventory      ←─── Kafka (order.created, payment.*) ─ Order Service, Payment Service
Notification   ←─── Kafka (todos os eventos) ─────── User, Order, Payment
Analytics      ←─── Kafka (product-viewed, order) ─── Catalog, Order
Payment ──────────── Kafka ────────────────────────→ Order Service (resultado pagamento)
Order ←───────────── Kafka (stock-insufficient) ──── Inventory Service
```

---

## Infraestrutura Local (docker-compose)

| Serviço | Porta | Descrição |
|---|---|---|
| postgres-user | 5433 | Banco do User Service (fusão Auth+User; `postgres-auth`/5432 descontinuado) |
| postgres-catalog | 5434 | Banco do Catalog Service |
| postgres-order | 5435 | Banco do Order Service |
| postgres-payment | 5436 | Banco do Payment Service |
| postgres-inventory | 5437 | Banco do Inventory Service |
| redis | 6379 | Cache + Carrinho + Blacklist + Rate Limit |
| zookeeper | 2181 | Kafka coordinator |
| kafka | 9092 | Message broker |
| elasticsearch | 9200 | Full-text search |
| kibana | 5601 | UI do Elasticsearch |
| cassandra | 9042 | Analytics (alta escrita) |
| prometheus | 9090 | Métricas |
| grafana | 3011 | Dashboards |
| jaeger | 16686 | Traces distribuídos |
| mailhog | 8025 | SMTP local (testes de e-mail) |

---

## Roadmap — 10 Fases

| Fase | Objetivo | Microsserviço(s) |
|---|---|---|
| 1 | Fundação e infraestrutura | API Gateway ✅ |
| 2 | Identidade e usuários | User Service (fusão Auth+User) |
| 3 | Catálogo e busca | Catalog Service, Search Service |
| 4 | Carrinho | Cart Service |
| 5 | Estoque e pedidos | Inventory Service, Order Service |
| 6 | Pagamentos | Payment Service |
| 7 | Notificações | Notification Service |
| 8 | Analytics | Analytics Service |
| 9 | Observabilidade | OTel em todos os serviços |
| 10 | Deploy e produção | VPS Hostinger + Kubernetes |

Detalhes completos: `docs/planning/action-plan.md`

---

## Decisões Arquiteturais (ADRs)

| ADR | Decisão | Justificativa resumida |
|---|---|---|
| ADR-001 | Java 25 com Virtual Threads | LTS + alta concorrência sem callbacks |
| ADR-002 | Database per Service | Autonomia total, sem acoplamento de banco |
| ADR-003 | Apache Kafka | Padrão event-driven, replay, Consumer Groups |
| ADR-004 | Spring Cloud Gateway | Ecossistema Spring nativo, filtros customizáveis |
| ADR-005 | Elasticsearch para busca | Full-text scoring, analyzers PT, filtros eficientes |
| ADR-006 | Clean/Hexagonal Architecture | Domínio testável sem Spring |
| ADR-007 | Cassandra para Analytics | Otimizado para alta escrita, COUNTER nativo |
| ADR-008 | OpenTelemetry | Vendor-neutral, CNCF standard, nativo Spring Boot 3.x |

---

## Como Rodar Localmente

```bash
# 1. Subir infraestrutura (Redis obrigatório para o Gateway)
cd infra
docker compose up -d redis

# 2. API Gateway — abrir no IntelliJ
# File > Open > backend/api-gateway (selecionar o pom.xml)
# SDK: JDK 25 | Run: GatewayApplication.java
# Health check: curl http://localhost:8001/actuator/health

# 3. Frontend
cd apps/frontend/ecommerce
npm install
npm run dev
# Acesse: http://localhost:3000
```

---

## Documentação

| Pasta | Conteúdo |
|---|---|
| `docs/apps/api-gateway.md` | Explicação didática completa do Gateway (Hexagonal, JWT, rate limit, circuit breaker) |
| `docs/planning/action-plan.md` | Plano de ação das 10 fases com critérios de conclusão |
| `docs/planning/specs/01-api-gateway.md` | Spec técnica do API Gateway (deps, endpoints, estrutura) |
| `docs/planning/specs/02-user-service.md` | Spec do User Service — Identidade+Perfil fundidos (Clean Architecture, JWT, eventos Kafka) |
| `docs/planning/specs/06-inventory-service.md` | Spec do Inventory Service — reserva de estoque (Hexagonal, Saga via Kafka) |
| `docs/planning/specs/03-10-*.md` | Specs dos demais microsserviços |
