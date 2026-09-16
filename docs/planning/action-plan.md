# AutoHubStore — Plano de Ação de Microsserviços

## Visão Geral

O AutoHubStore é um e-commerce automotivo construído com arquitetura de microsserviços.
O **MVP tem 9 microsserviços** em **Java 25** (LTS), seguindo diferentes arquiteturas para fins
educacionais. Escopo confirmado com o usuário — não é 12, nem 10.

**Repositório:** monorepo em `autohub-store/`
**Backend:** `backend/<nome-do-servico>/`
**Infraestrutura:** `infra/`

## Divisão de Esforço por Microsserviço (40 / 20 / 40)

Toda fase de desenvolvimento de microsserviço — e o roadmap geral — segue esta divisão fixa de
esforço, aplicada a cada um dos 9 serviços do MVP:

| Fatia | % | Conteúdo | Responsável |
|---|---|---|---|
| **Spec + Arquitetura** | 40% | Use cases, análise de requisitos, PRD, critérios de aceite, DER (Mermaid) | product-owner (specs/PRD/critérios) + software-architect (DER e decisões de arquitetura) |
| **Desenvolvimento** | 20% | Implementação de código (entidades, services, controllers, migrações, mensageria) | backend-engineer / frontend-engineer |
| **Testes** | 40% | Unitários, aceitação, e2e, segurança, UI | backend-engineer/frontend-engineer (escrevem) + quality-analyst (valida) |

> A fatia de Testes (40%) é deliberadamente do mesmo tamanho que Spec+Arquitetura — reflete que
> este é um projeto de estudo em que qualidade e cobertura de teste são objetivo de aprendizado, não
> reboco de última hora.

## Critério de Conclusão de Microsserviço

Nenhum microsserviço do MVP é considerado **pronto** sem que **todos os 8 itens** abaixo sejam
verdadeiros simultaneamente:

1. **Testes unitários** criados e todos passando, cobrindo a lógica de domínio/serviço (ver
   "Estratégia de Testes" de cada spec).
2. **Testes de aceitação (Cucumber)** criados e todos passando, cobrindo os critérios de aceite
   definidos pelo product-owner na spec do serviço (fluxo ponta a ponta via
   Testcontainers/WireMock, conforme o serviço).
3. **Cobertura de testes ≥ 70%.**
4. **Checkstyle sem nenhuma violação** (`checkstyle:check` limpo — Maven) / equivalente Gradle, ver
   [§ Qualidade de Código — Checkstyle](#qualidade-de-código--checkstyle).
5. **Snyk sem nenhuma vulnerabilidade** nas dependências (`snyk test` retornando `ok: true`), ver
   [CLAUDE.md § Snyk](../../CLAUDE.md#snyk--obrigatório-em-todo-microsserviço-novo).
6. **Build executado com sucesso** — `mvn`/`gradle` compile + package para backend, `npm run
   build` para frontend.
7. **DER atualizado** em `docs/planning/der/` com todas as tabelas realmente usadas pelo serviço —
   já existe um DER inicial para os 9 serviços do MVP; o critério passa a exigir mantê-lo
   sincronizado conforme o código evolui, não só existir na fase de spec.
8. **Documentação do serviço** em `docs/apps/<nome-servico>.md` — cada um dos 9 serviços do MVP
   precisa do próprio arquivo; o conteúdo é escrito
   durante a implementação, mas o arquivo é gate de conclusão, não opcional.

Entrega sem os 8 itens passando **não é considerada finalizada**, independentemente de o código
compilar ou o endpoint responder manualmente.

### Tabela de evidência — obrigatória ao final

Toda criação de um novo microsserviço **ou** validação de "está de fato finalizado" — feita pelo
`software-architect`/`quality-analyst` — deve terminar exibindo ao usuário uma tabela com os 8
itens e seu status (passou/falhou/pendente), no formato:

| # | Item | Status | Evidência |
|---|---|---|---|
| 1 | Testes unitários | ✅/❌ | |
| 2 | Aceitação (Cucumber) | ✅/❌ | |
| 3 | Cobertura ≥70% | ✅/❌ | |
| 4 | Checkstyle | ✅/❌ | |
| 5 | Snyk `ok: true` | ✅/❌ | |
| 6 | Build | ✅/❌ | |
| 7 | DER atualizado | ✅/❌ | |
| 8 | `docs/apps/<nome-servico>.md` | ✅/❌ | |

Só se declara "Concluído" com os 8/8 marcados ✅. Tabela incompleta (algum item ❌/pendente) deve
ser exibida do mesmo jeito, deixando claro o que falta — nunca omitir a tabela nem declarar
conclusão sem ela.

### Fluxo de responsabilidade

- `backend-engineer`/`frontend-engineer` escrevem os testes (unitários + aceitação) e corrigem
  qualquer bug encontrado **antes** de entregar para validação — não repassam bug para o
  quality-analyst achar. Eles mesmos rodam build, testes, checkstyle, Snyk e cobertura durante a
  implementação (têm acesso a `Bash` escopado a build/teste/lint/checkstyle/Snyk do próprio
  serviço — nunca `git` nem `docker compose up/down`, ver
  [CLAUDE.md § Time de Engenharia](../../CLAUDE.md#time-de-engenharia-agentes-claude-code)).
- `quality-analyst` valida re-executando tudo de forma independente (não confia apenas no relato
  do dev) e só aprova como "pronta" uma entrega que já atenda aos 8 itens acima.
- `software-architect` e `product-owner` não executam build/teste — apenas leem e editam
  specs/ADRs/documentação de planejamento.
- JDK do projeto para qualquer execução local: `C:\Users\jeanc\.jdks\ms-25.0.4` (`JAVA_HOME`).

## Decisões de Consolidação

Revisão de escopo aplicada sobre os microsserviços originais — cada decisão avaliada por bounded
context (DDD), perfil de consistência/concorrência, necessidade de escala independente e valor
didático (projeto de estudo solo). Detalhes completos da análise nas specs referenciadas.

| Decisão | Motivo |
|---|---|
| **Auth Service extraído do User Service** | Autenticação (sessão/token, superfície de ataque própria, cadência de mudança ligada a segurança) e Perfil (CRUD cadastral) são bounded contexts distintos mesmo dependendo um do outro. Auth Service chama User Service via OpenFeign só pra validar/atualizar credencial (`password_hash` nunca sai do User Service). Ver [02-auth-service.md](specs/02-auth-service.md) e [03-user-service.md](specs/03-user-service.md). |
| **Cart Service mantido separado de Order** | Bounded contexts diferentes: Cart é sessão de compra efêmera (Redis, TTL, mutável), Order é agregado transacional durável (Postgres, máquina de estados, auditoria). Fundir quebraria ADR-002 (database per service) e mataria a demo de circuit breaker Cart↔Catalog. Ver [05-cart-service.md](specs/05-cart-service.md) e [07-order-service.md](specs/07-order-service.md). |
| **Inventory Service criado (extraído do Catalog)** | Estoque é recurso de escrita contenciosa que exige forte consistência (evitar overselling) — incompatível com o papel de leitura/cache do Catalog. Novo serviço aplica padrão Saga (reserve/confirm/release via Kafka) entre Order, Payment e Inventory. Ver [06-inventory-service.md](specs/06-inventory-service.md). |
| **Search Service, Analytics Service e Compatibility Service ficam fora do MVP** | Nenhum dos três bloqueia o happy path de compra (cadastro → login → catálogo → carrinho → pedido → estoque → pagamento → e-mail). São incrementos de valor sobre um catálogo/pedido já funcional — busca full-text, dashboard administrativo e fitment peça↔veículo, respectivamente. Ficam documentados como fase pós-MVP; specs mantidas em `docs/planning/specs/10-search-service.md`, `11-analytics-service.md` e `12-compatibility-service.md`, não apagadas. |

**Resultado líquido do MVP:** 9 microsserviços, API Gateway como 1º e Notification Service como 9º
e último. Auth Service é o 2º (logo após o Gateway).

---

## Stack Tecnológica Global

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | **25** (LTS) | Linguagem de todos os serviços |
| Spring Boot | 3.x | Framework base |
| Maven | 3.9+ | Build tool (5 serviços do MVP) |
| Gradle | 8+ | Build tool (4 serviços do MVP) |
| Spring Cloud | 2023.x | Gateway, OpenFeign, Circuit Breaker |
| Spring Security | 6.x | Autenticação e autorização |
| JJWT | 0.12+ | JWT (Auth Service) |
| PostgreSQL | 16 | Persistência relacional (6 serviços do MVP) |
| Redis | 7 | Cache, carrinho, blacklist |
| Apache Kafka | 3.6+ | Mensageria assíncrona |
| OpenFeign | Spring Cloud | Chamadas REST entre serviços |
| Resilience4j | 2.x | Circuit Breaker, Retry |
| OpenTelemetry | 1.x | Traces distribuídos |
| Micrometer | 1.x | Métricas (Prometheus) |
| Testcontainers | 1.19+ | Testes de integração |
| Springdoc OpenAPI | 2.x | Swagger UI |
| Docker | 24+ | Containerização |
| Docker Compose | 2.x | Ambiente local |

> **Pós-MVP:** MongoDB 7.x (Compatibility Service), Elasticsearch 8.x (Search Service) e Cassandra
> 4.x (Analytics Service) entram na stack apenas quando esses três serviços forem implementados.

---

## Qualidade de Código — Checkstyle

Todos os microsserviços do monorepo **devem** apontar para o arquivo de regras compartilhado em:

```
infra/checkstyle/checkstyle.xml
```

O arquivo contém regras universais de estilo (limite de linha, imports, complexidade, nomenclatura, boas práticas) que se aplicam igualmente a todas as arquiteturas (MVC, Clean, Hexagonal) e ambos os build tools. O build **falha** em qualquer violação (`failsOnError = true`).

### Configuração obrigatória — Maven (api-gateway, auth-service, user-service, order-service, payment-service, inventory-service)

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

### Configuração obrigatória — Gradle (catalog-service, cart-service, notification-service — MVP; search-service, analytics-service — pós-MVP)

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

## Distribuição de Build Tools e Arquiteturas — MVP (9 serviços)

| # | Serviço | Porta | Build Tool | Arquitetura |
|---|---|---|---|---|
| 1 | API Gateway | 8001 | **Maven** | MVC |
| 2 | Auth Service | 8002 | **Maven** | MVC |
| 3 | User Service | 8003 | **Maven** | **Clean Architecture** |
| 4 | Catalog Service | 8004 | **Gradle** | MVC |
| 5 | Cart Service | 8005 | **Gradle** | MVC |
| 6 | Inventory Service | 8006 | **Maven** | **Hexagonal** |
| 7 | Order Service | 8007 | **Maven** | **Hexagonal** |
| 8 | Payment Service | 8008 | **Maven** | MVC |
| 9 | Notification Service | 8009 | **Gradle** | MVC |

> **Objetivo educacional:** A divisão de build tools permite aprender tanto Maven quanto Gradle em contexto real. A variação de arquiteturas demonstra Clean Architecture, Hexagonal (Ports & Adapters) e MVC no mesmo projeto.

### Pós-MVP (numeração 10-12, continuação sequencial após o MVP)

| # | Serviço | Porta | Build Tool | Arquitetura |
|---|---|---|---|---|
| 10 | Search Service | 8010 | Gradle | MVC |
| 11 | Analytics Service | 8011 | Gradle | MVC |
| 12 | Compatibility Service | 8012 | Gradle | MVC |

---

## Padrões de Pacotes por Arquitetura

### MVC (API Gateway, Auth, Catalog, Cart, Payment, Notification — MVP; Search, Analytics, Compatibility — pós-MVP)

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

> **Compatibility Service** (pós-MVP) segue o mesmo pacote MVC, porém `repository/` usa
> `MongoRepository` (Spring Data MongoDB) em vez de `JpaRepository`, e `model/` contém documentos
> (`@Document`) em vez de entidades JPA — regra de "sem `@ManyToOne`/relação automática" do
> [CLAUDE.md § Padrões de Implementação](../../CLAUDE.md#padrões-de-implementação--obrigatórios-em-todo-código-gerado)
> continua valendo: referência a `productId` como campo simples, nunca objeto embutido resolvido por join.

> **API Gateway** não tem JPA/Kafka, então adapta o pacote MVC: usa `filter/` no lugar de
> `repository`/`messaging`, para os `GlobalFilter`/security filters do Spring Cloud Gateway
> (`RateLimitFilter`, `JwtServerAuthenticationConverter`, `JwtReactiveAuthenticationManager`).
> `JwtService` e `RateLimitService` são `@Service` Spring-gerenciados de verdade, sem
> porta/interface intermediária. Detalhe completo em
> [docs/planning/specs/01-api-gateway.md](specs/01-api-gateway.md#estrutura-de-pacotes-mvc).

### Clean Architecture — User Service

```
com.autohubstore.userservice/
├── domain/
│   ├── model/          # Entidades e Value Objects (User, Address — sem credencial de token)
│   ├── event/          # Domain Events
│   ├── repository/     # Interfaces (output boundary)
│   └── service/        # Domain Services
├── application/
│   ├── usecase/        # Use Cases (input boundary) — inclui verify-credentials/update-password
│   ├── dto/            # DTOs
│   └── mapper/         # Mappers
└── infrastructure/
    ├── web/            # Controllers REST (público + /internal/v1 pro Auth Service)
    ├── persistence/    # Implementações JPA
    ├── messaging/      # Kafka producers
    └── config/         # Spring config
```

> **Auth Service** não tem domínio próprio de credencial — segue o pacote MVC padrão (ver acima),
> com `external/UserServiceClient.java` (`@FeignClient`) no lugar de acesso direto a repositório de
> usuário. Detalhe completo em
> [docs/planning/specs/02-auth-service.md](specs/02-auth-service.md#estrutura-de-pacotes-mvc).

### Hexagonal — Inventory Service e Order Service (Ports & Adapters)

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

## Mapa de Microsserviços — MVP (9 serviços)

| # | Serviço | Porta | Banco | Kafka | Status |
|---|---|---|---|---|---|
| 1 | API Gateway | 8001 | Redis (rate limit) | — | Concluído |
| 2 | Auth Service | 8002 | PostgreSQL (`auth_db`) + Redis | Producer | Concluído |
| 3 | User Service | 8003 | PostgreSQL (`user_db`) | Producer | Concluído |
| 4 | Catalog Service | 8004 | PostgreSQL (`catalog_db`) + Redis | Producer | Em implementação |
| 5 | Cart Service | 8005 | Redis | — | Planejado |
| 6 | Inventory Service | 8006 | PostgreSQL (`inventory_db`) | Producer + Consumer | Planejado |
| 7 | Order Service | 8007 | PostgreSQL (`order_db`) | Producer + Consumer | Planejado |
| 8 | Payment Service | 8008 | PostgreSQL (`payment_db`) | Producer | Planejado |
| 9 | Notification Service | 8009 | — (stateless) | Consumer | Planejado |

> **Nomes de banco:** seguem exatamente `POSTGRES_DB` definido em `infra/docker-compose.yml`
> (`auth_db`, `user_db`, `catalog_db`, `order_db`, `payment_db`, `inventory_db`) — evita erro
> `FATAL: database "..." does not exist` no Flyway ao subir serviço local contra a infra do compose.

### Pós-MVP (numeração 10-12, continuação sequencial após o MVP)

| # | Serviço | Porta | Banco | Kafka | Status |
|---|---|---|---|---|---|
| 10 | Search Service | 8010 | Elasticsearch | Consumer | Pós-MVP |
| 11 | Analytics Service | 8011 | Cassandra | Consumer | Pós-MVP |
| 12 | Compatibility Service | 8012 | MongoDB (`compatibility_db`) | Consumer | Pós-MVP |

---

## Mapa de Dependências

```
API Gateway ─────────────────────────────→ Todos os serviços do MVP (roteamento)
Auth Service ──── OpenFeign ──────────────→ User Service (verify-credentials, by-email, update-password)
Cart Service ──── OpenFeign ─────────────→ Catalog Service
Catalog Service ── OpenFeign ────────────→ Inventory Service (disponibilidade)
Order Service ───── OpenFeign ─────────────→ Cart Service, User Service
Inventory Service ←── Kafka ──────────────── Order Service (order.created), Payment Service (payment.approved/rejected)
Notification  ←─── Kafka ─────────────────── User, Auth, Order, Payment
Payment ──────── Kafka ──────────────────→ Order Service (resultado pagamento)
Order ←────────── Kafka ─────────────────── Payment Service, Inventory Service (stock-insufficient)

# Pós-MVP:
Catalog Service ── OpenFeign ────────────→ Compatibility Service (veículos compatíveis)
Search Service ←── Kafka ─────────────────── Catalog Service (product.created/updated)
Analytics     ←─── Kafka ─────────────────── Catalog (product-viewed), Order (order.created)
```

---

## Tópicos Kafka

| Tópico | Producer | Consumer |
|---|---|---|
| `user.created` | User Service | Notification Service |
| `user.password-reset` | Auth Service | Notification Service |
| `catalog.product-created` | Catalog Service | Inventory Service (_pós-MVP: Search Service, Compatibility Service_) |
| `catalog.product-updated` | Catalog Service | _pós-MVP: Search Service_ |
| `catalog.product-viewed` | Catalog Service | _pós-MVP: Analytics Service_ |
| `order.created` | Order Service | Notification Service, Inventory Service (_pós-MVP: Analytics Service_) |
| `order.status-changed` | Order Service | — |
| `payment.approved` | Payment Service | Order Service, Notification Service, Inventory Service |
| `payment.rejected` | Payment Service | Order Service, Notification Service, Inventory Service |
| `inventory.stock-insufficient` | Inventory Service | Order Service |

---

## Infraestrutura Local (docker-compose)

Arquivo: `infra/docker-compose.yml`

| Serviço | Porta | Descrição |
|---|---|---|
| postgres-auth | 5432 | Banco do Auth Service (`auth_db`) |
| postgres-user | 5433 | Banco do User Service (`user_db`) |
| postgres-catalog | 5434 | Banco do Catalog Service |
| postgres-order | 5435 | Banco do Order Service |
| postgres-payment | 5436 | Banco do Payment Service |
| postgres-inventory | 5437 | Banco do Inventory Service |
| redis | 6379 | Cache + Carrinho + Blacklist |
| zookeeper | 2181 | Kafka coordinator |
| kafka | 9092 | Message broker |
| prometheus | 9090 | Métricas |
| grafana | 3011 | Dashboards |
| jaeger | 16686 | Traces distribuídos |
| mailhog | 8025 | SMTP local (testes de e-mail) |

> **Pós-MVP:** `mongo-compatibility` (27017), `elasticsearch`/`kibana` (9200/5601) e `cassandra`
> (9042) entram no compose apenas quando Compatibility, Search e Analytics Service forem
> implementados, respectivamente. Não fazem parte da infra necessária para o MVP de 9 serviços.

---

## Ordem de Desenvolvimento — Fases do MVP

Cada fase de microsserviço abaixo segue a divisão 40% Spec+Arquitetura / 20% Desenvolvimento / 40%
Testes (ver [§ Divisão de Esforço](#divisão-de-esforço-por-microsserviço-40--20--40)) e só fecha
quando o [Critério de Conclusão de Microsserviço](#critério-de-conclusão-de-microsserviço) for
satisfeito para todos os serviços da fase.

### Fase 1 — Fundação e Infraestrutura

**Objetivo:** Ambiente de dev completo funcionando localmente.

**40% Spec+Arquitetura:** use cases de roteamento/rate-limit/JWT do Gateway, PRD, DER
([der/api-gateway.mmd](der/api-gateway.mmd) — sem entidade persistente própria, mapeia
`JwtClaims`/`RateLimitKey` como estruturas em memória).
**20% Desenvolvimento:** `infra/docker-compose.yml`, templates Spring Boot (um Maven MVC + um
Gradle MVC), configuração Flyway nos templates, pipeline CI/CD básico.
**40% Testes:** unitários (`JwtService`, `RateLimitService`), integração (`WebTestClient`
roteamento/erros 401/429), segurança (endpoint protegido sem token).

**Critério de conclusão:** `docker compose up` sobe toda infra sem erros; ambos os templates
compilam e testam; API Gateway atende aos [8 itens do Critério de Conclusão de
Microsserviço](#critério-de-conclusão-de-microsserviço) (unitários, aceitação, cobertura ≥ 70%,
checkstyle, Snyk, build, DER, `docs/apps/api-gateway.md`).

**Microsserviço criado:** API Gateway (Maven + MVC)
**Spec:** [docs/planning/specs/01-api-gateway.md](specs/01-api-gateway.md)

---

### Fase 2 — Identidade e Autenticação

**Objetivo:** Fluxo completo de cadastro, login, JWT e gestão de perfil, com Autenticação e Perfil
como serviços separados (ver [Decisões de Consolidação](#decisões-de-consolidação)).

**40% Spec+Arquitetura:** use cases de cadastro/perfil/endereço (User) e login/logout/refresh/reset
(Auth), PRD, critérios de aceite, DER [der/user-service.mmd](der/user-service.mmd) e
[der/auth-service.mmd](der/auth-service.mmd).
**20% Desenvolvimento:** User Service (Maven + Clean Architecture) e Auth Service (Maven + MVC),
integração OpenFeign entre eles, Swagger nos dois.
**40% Testes:** unitários e de integração (Testcontainers) nos dois serviços, incluindo circuit
breaker Auth→User.

**Critério de conclusão:** Fluxo cadastro (User Service) → login → refresh → logout (Auth
Service) funcionando; reset de senha via MailHog; User Service e Auth Service atendem cada um aos
[8 itens do Critério de Conclusão de Microsserviço](#critério-de-conclusão-de-microsserviço),
incluindo `docs/apps/user-service.md` e `docs/apps/auth-service.md`.

**Specs:**
- User Service → [docs/planning/specs/03-user-service.md](specs/03-user-service.md)
- Auth Service → [docs/planning/specs/02-auth-service.md](specs/02-auth-service.md)

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
referência a `id: number` de produto restante no frontend; testes e2e do fluxo de login no
frontend passando.

---

### Fase 3 — Catálogo

**Objetivo:** CRUD de produtos com cache e upload de imagens.

**40% Spec+Arquitetura:** use cases de CRUD de produto/categoria, geração de SKU/slug, PRD, DER
[der/catalog-service.mmd](der/catalog-service.mmd).
**20% Desenvolvimento:** Catalog Service (Gradle + MVC) — CRUD admin + listagem pública + cache
Redis + upload de imagens (MinIO).
**40% Testes:** unitários (`ProductService`, `SlugGenerator`), integração (Testcontainers
PostgreSQL + Redis + Kafka), cache hit/miss.

**Critério de conclusão:** cache Redis e evento `catalog.product-created` publicados
corretamente; Catalog Service atende aos [8 itens do Critério de Conclusão de
Microsserviço](#critério-de-conclusão-de-microsserviço), incluindo `docs/apps/catalog-service.md`.

**Spec:** Catalog Service → [docs/planning/specs/04-catalog-service.md](specs/04-catalog-service.md)

> Search Service e Compatibility Service (busca full-text e fitment peça↔veículo) **não fazem
> parte desta fase** — ficam para a fase Pós-MVP (ver seção final).

---

### Fase 4 — Carrinho

**Objetivo:** Carrinho persistido no Redis com circuit breaker.

**40% Spec+Arquitetura:** use cases de adicionar/remover/atualizar item, snapshot de preço, PRD,
DER [der/cart-service.mmd](der/cart-service.mmd) (estrutura de chave/hash Redis, não schema
relacional).
**20% Desenvolvimento:** Cart Service (Gradle + MVC) — Redis, OpenFeign + Resilience4j, TTL 7 dias.
**40% Testes:** unitários (`CartService` — snapshot imutável, TTL), integração (Testcontainers
Redis + WireMock Catalog), circuit breaker.

**Critério de conclusão:** abertura do circuit breaker quando Catalog está indisponível
funcionando; Cart Service atende aos [8 itens do Critério de Conclusão de
Microsserviço](#critério-de-conclusão-de-microsserviço), incluindo `docs/apps/cart-service.md`.

**Spec:** [docs/planning/specs/05-cart-service.md](specs/05-cart-service.md)

---

### Fase 5 — Estoque e Pedidos

**Objetivo:** Reserva de estoque com padrão Saga e máquina de estados de pedido, ambos em arquitetura Hexagonal.

**40% Spec+Arquitetura:** use cases de reserva/confirmação/liberação de estoque e máquina de
estados de pedido, PRD, critérios de aceite do fluxo Saga completo, DER
[der/inventory-service.mmd](der/inventory-service.mmd) e [der/order-service.mmd](der/order-service.mmd).
**20% Desenvolvimento:** Inventory Service (Maven + Hexagonal) e Order Service (Maven + Hexagonal).
**40% Testes:** unitários dos domain services (testáveis sem Spring context), integração
(Testcontainers PostgreSQL + Kafka), teste de concorrência (reserva simultânea do último item),
teste de compensação Saga.

**Critério de conclusão:** Reserva de estoque decrementa corretamente sob concorrência; pedido
cancela automaticamente quando estoque insuficiente; Inventory Service e Order Service atendem
cada um aos [8 itens do Critério de Conclusão de
Microsserviço](#critério-de-conclusão-de-microsserviço), incluindo `docs/apps/inventory-service.md`
e `docs/apps/order-service.md`.

**Specs:**
- Inventory Service → [docs/planning/specs/06-inventory-service.md](specs/06-inventory-service.md)
- Order Service → [docs/planning/specs/07-order-service.md](specs/07-order-service.md)

---

### Fase 6 — Pagamentos

**Objetivo:** Simulação de pagamento com eventos Kafka.

**40% Spec+Arquitetura:** use cases de criação/idempotência de pagamento, PRD, DER
[der/payment-service.mmd](der/payment-service.mmd).
**20% Desenvolvimento:** Payment Service (Maven + MVC) — simulação 70/30, idempotência, eventos
Kafka.
**40% Testes:** unitários (`PaymentSimulatorService` com taxa determinística), integração
(Testcontainers), teste de idempotência (409 em chamada duplicada).

**Critério de conclusão:** aprovação, rejeição e idempotência cobertas; Payment Service atende aos
[8 itens do Critério de Conclusão de Microsserviço](#critério-de-conclusão-de-microsserviço),
incluindo `docs/apps/payment-service.md`.

**Spec:** [docs/planning/specs/08-payment-service.md](specs/08-payment-service.md)

---

### Fase 7 — Notificações

**Objetivo:** E-mails reativos a eventos com retry e DLT.

**40% Spec+Arquitetura:** use cases de envio por tipo de evento, PRD, DER
[der/notification-service.mmd](der/notification-service.mmd) (serviço stateless — DER documenta os
records de evento consumidos, não schema de banco).
**20% Desenvolvimento:** Notification Service (Gradle + MVC) — Thymeleaf, DLT, retry com backoff
exponencial.
**40% Testes:** unitários (`EmailService` com mock de `JavaMailSender`), integração
(Testcontainers Kafka), teste de DLT após falhas.

**Critério de conclusão:** fluxo de DLT coberto; Notification Service atende aos [8 itens do
Critério de Conclusão de Microsserviço](#critério-de-conclusão-de-microsserviço), incluindo
`docs/apps/notification-service.md`.

**Spec:** [docs/planning/specs/09-notification-service.md](specs/09-notification-service.md)

---

### Fase 8 — Observabilidade

**Objetivo:** Traces, métricas e logs em todos os serviços do MVP.

**Entregas:**
1. OpenTelemetry agent em todos os serviços
2. Traces visíveis no Jaeger
3. Métricas Prometheus exportadas
4. Dashboards Grafana por serviço
5. Logs JSON estruturados → ELK Stack

---

### Fase 9 — Deploy e Produção

**Objetivo:** Deploy no VPS Hostinger + manifests Kubernetes.

**Entregas:**
1. `docker-compose.prod.yml`
2. Manifests Kubernetes (`k8s/`: Deployments, Services, ConfigMaps, Secrets)
3. Pipeline CI/CD completo (build → test → push image → deploy)
4. HTTPS com Let's Encrypt (Certbot + NGINX)

---

### Fase Pós-MVP — Busca, Analytics e Compatibilidade

**Objetivo:** Incrementos de valor sobre o MVP já funcional — busca full-text, dashboard
administrativo e consulta de compatibilidade peça↔veículo. Nenhum bloqueia o happy path do MVP
(ver [Decisões de Consolidação](#decisões-de-consolidação)).

**Entregas (quando esta fase for priorizada):**
1. **Search Service** (Gradle + MVC) — Elasticsearch + Kafka consumer para re-indexação
2. **Analytics Service** (Gradle + MVC) — Cassandra counters, Kafka consumer, dashboard API
3. **Compatibility Service** (Gradle + MVC) — CRUD admin de aplicações (peça × marca/modelo/ano/
   motorização) em MongoDB; endpoint `findCompatibleVehiclesByProductId` consumido pelo Catalog via
   OpenFeign

Cada um segue a mesma divisão 40/20/40 e o mesmo critério de conclusão (testes unitários + aceitação
passando) quando entrar em desenvolvimento.

**Specs:**
- Search Service → [docs/planning/specs/10-search-service.md](specs/10-search-service.md)
- Analytics Service → [docs/planning/specs/11-analytics-service.md](specs/11-analytics-service.md)
- Compatibility Service → [docs/planning/specs/12-compatibility-service.md](specs/12-compatibility-service.md)

---

## Critérios de Aceite do MVP

1. **Happy Path:** Cadastro → Login → Catálogo → Carrinho → Pedido → Estoque reservado → Pagamento aprovado → Estoque confirmado → E-mail de confirmação
2. **Rejeição por pagamento:** Pagamento rejeitado → Estoque liberado → E-mail notificado → Pedido `CANCELLED`
3. **Rejeição por estoque:** Estoque insuficiente na reserva → Pedido `CANCELLED` sem cobrar pagamento (compensação Saga)
4. **Resiliência:** Circuit breaker entre Cart e Catalog funcionando
5. **Observabilidade:** Trace completo visível no Jaeger
6. **Qualidade:** Todos os 9 microsserviços do MVP atendem ao [Critério de Conclusão de Microsserviço](#critério-de-conclusão-de-microsserviço) (testes unitários + aceitação passando)

> Dashboard de Analytics e busca full-text saem do escopo de aceite do MVP — passam a ser critério
> de aceite da fase Pós-MVP.
