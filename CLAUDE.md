# AutoHubStore — Contexto do Projeto para Claude

## O que é

**AutoHubStore** é um e-commerce automotivo fictício desenvolvido como projeto de estudo avançado em Backend Java. Objetivo: evoluir para nível Pleno I e Pleno II aplicando microsserviços, DDD, Clean Architecture, Event-Driven Architecture e Cloud Native. Toda arquitetura e decisões técnicas são elaboradas com rigor de produto real.

> **MVP com 9 microsserviços.** Escopo confirmado com o usuário — ordem de criação e detalhes
> completos das decisões em
> [docs/planning/action-plan.md § Decisões de Consolidação](docs/planning/action-plan.md#decisões-de-consolidação).
> Destaque: **Auth Service** é o 2º serviço criado (logo após o Gateway) — extraído do User
> Service para isolar Autenticação (sessão/token) de Perfil (CRUD cadastral); **Notification
> Service** é o 9º e último do MVP. Search Service, Analytics Service e Compatibility Service
> **ficam fora do MVP** (pós-MVP — specs mantidas em `docs/planning/specs/` para fase futura).

---

## Status Atual (Fase 1 — Fundação)

| Componente | Status |
|---|---|
| API Gateway (`backend/api-gateway/`) | Implementado e funcional |
| Infraestrutura (`infra/docker-compose.yml`) | Implementada |
| Frontend (`apps/frontend/ecommerce/`) | Estruturado com mock data (sem integração com API) |
| Demais 8 microsserviços do MVP | Planejados — specs em `docs/planning/specs/` |

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
│   ├── docker-compose.yml     # PostgreSQL x6, Redis, Kafka, Elasticsearch, Cassandra + monitoring
│   └── prometheus.yml
├── docs/
│   ├── apps/                  # Documentação técnica e didática de cada app — gate de conclusão
│   │   │                      # (ver § Critério de Conclusão): cada um dos 9 serviços do MVP
│   │   │                      # precisa do próprio <nome-servico>.md antes de fechar como pronto
│   │   └── api-gateway.md     # Explicação detalhada do Gateway (Hexagonal, JWT, rate limit) — único já escrito
│   └── planning/              # Planejamento e specs
│       ├── action-plan.md     # Plano de ação de fases (40% spec+arquitetura / 20% dev / 40% testes)
│       ├── der/                # Diagramas DER (Mermaid) por microsserviço do MVP
│       │   └── <nome-servico>.mmd
│       └── specs/             # Specs detalhadas por microsserviço
│           ├── 01-api-gateway.md, 02-auth-service.md, 03-user-service.md, 04-catalog-service.md
│           ├── 05-cart-service.md, 06-inventory-service.md, 07-order-service.md
│           ├── 08-payment-service.md, 09-notification-service.md         # MVP (9 serviços)
│           └── 10-search-service.md, 11-analytics-service.md,
│               12-compatibility-service.md                                # pós-MVP
├── README.md
└── CLAUDE.md                  # este arquivo
```

---

## 9 Microsserviços (MVP)

> Escopo do MVP confirmado com o usuário: **9 serviços**. Inventory Service foi extraído do
> Catalog Service (controle de estoque exige forte consistência/reserva, incompatível com o papel
> de leitura/cache do Catalog) e Auth Service foi extraído do User Service (Autenticação e Perfil
> são bounded contexts distintos, mesmo dependendo um do outro via OpenFeign). Detalhes e
> justificativa completa em
> [docs/planning/action-plan.md § Decisões de Consolidação](docs/planning/action-plan.md#decisões-de-consolidação).
> Cada serviço só é considerado **pronto** quando atender aos 8 itens do checklist — unitários,
> aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build com
> sucesso, DER atualizado e `docs/apps/<nome-servico>.md` publicado — ver
> [action-plan.md § Critério de Conclusão](docs/planning/action-plan.md#critério-de-conclusão-de-microsserviço).

| # | Serviço | Porta | Banco | Arquitetura | Build | Status | DER |
|---|---|---|---|---|---|---|---|
| 1 | **API Gateway** | 8001 | Redis (rate limit) | MVC | Maven | Implementado | [der/api-gateway.mmd](docs/planning/der/api-gateway.mmd) |
| 2 | **Auth Service** | 8002 | PostgreSQL + Redis | MVC | Maven | Em implementação | [der/auth-service.mmd](docs/planning/der/auth-service.mmd) |
| 3 | **User Service** | 8003 | PostgreSQL | Clean Architecture | Maven | Em implementação | [der/user-service.mmd](docs/planning/der/user-service.mmd) |
| 4 | **Catalog Service** | 8004 | PostgreSQL + Redis | MVC | Gradle | Em implementação | [der/catalog-service.mmd](docs/planning/der/catalog-service.mmd) |
| 5 | **Cart Service** | 8005 | Redis | MVC | Gradle | Planejado | [der/cart-service.mmd](docs/planning/der/cart-service.mmd) |
| 6 | **Inventory Service** | 8006 | PostgreSQL | Hexagonal | Maven | Planejado | [der/inventory-service.mmd](docs/planning/der/inventory-service.mmd) |
| 7 | **Order Service** | 8007 | PostgreSQL | Hexagonal | Maven | Planejado | [der/order-service.mmd](docs/planning/der/order-service.mmd) |
| 8 | **Payment Service** | 8008 | PostgreSQL | MVC | Maven | Planejado | [der/payment-service.mmd](docs/planning/der/payment-service.mmd) |
| 9 | **Notification Service** | 8009 | — (stateless) | MVC | Gradle | Planejado | [der/notification-service.mmd](docs/planning/der/notification-service.mmd) |

> Alternância Maven/Gradle é intencional: objetivo educacional de aprender ambos em contexto real.

### Pós-MVP (numeração 10-12, continuação sequencial após o MVP)

| # | Serviço | Porta | Banco | Motivo de ficar fora do MVP |
|---|---|---|---|---|
| 10 | Search Service | 8010 | Elasticsearch | Busca full-text é incremento sobre o Catalog já funcional — não bloqueia o happy path do MVP (listagem/detalhe de produto supre a demo inicial). |
| 11 | Analytics Service | 8011 | Cassandra | Métricas/dashboard administrativo não bloqueiam a jornada de compra do MVP. |
| 12 | Compatibility Service | 8012 | MongoDB | Consulta de fitment peça↔veículo é valor agregado sobre o catálogo, não pré-requisito do fluxo de compra. |

Specs desses três (`docs/planning/specs/10-search-service.md`, `11-analytics-service.md`,
`12-compatibility-service.md`) permanecem no repositório para a fase futura pós-MVP — não foram
apagadas, apenas marcadas como fora de escopo atual.

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
| PostgreSQL | 16 | Persistência relacional (6 serviços do MVP: Auth, User, Catalog, Inventory, Order, Payment) |
| Flyway | 9+ | Migrações de banco |
| Redis | 7 | Cache, carrinho, blacklist de tokens, rate limit |
| Apache Kafka | 3.6+ | Mensageria assíncrona (eventos de domínio) |
| Elasticsearch | 8.x | Busca full-text (Search Service — pós-MVP) |
| Cassandra | 4.x | Analytics (alta taxa de escrita — pós-MVP) |
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

### Hexagonal — Inventory Service, Order Service
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

### MVC — API Gateway e demais serviços MVC do MVP (Auth, Catalog, Cart, Payment, Notification)
> Search, Analytics e Compatibility (pós-MVP) seguem o mesmo padrão MVC quando implementados.
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
> API Gateway (sem JPA/Kafka) adapta o padrão: usa `filter/` no lugar de
> `repository`/`messaging` para os `GlobalFilter`/security filters do Spring
> Cloud Gateway (`RateLimitFilter`, `JwtServerAuthenticationConverter`,
> `JwtReactiveAuthenticationManager`).
>
> Auth Service adapta o padrão MVC substituindo `repository/` (JPA para `refresh_tokens` e
> `password_reset_tokens`) por um `external/` adicional com `UserServiceClient` (`@FeignClient`)
> — não tem tabela de usuário própria, valida/atualiza credencial chamando o User Service.

---

## Convenções de Código

- **Package base:** `com.autohubstore.<servicename>`
- **Domínio isolado:** em Hexagonal/Clean, o domínio não importa nada do Spring
- **DTOs separados de entidades** — nunca expor JPA entity no controller
- **Flyway:** arquivos em `src/main/resources/db/migration/V*.sql`
- **Swagger:** Springdoc OpenAPI 2.x, acessível em `/swagger-ui.html` em todos os serviços
- **Sem @Service no domínio** — instanciado via `@Configuration` (ex: `DomainConfig.java`)
- **Sem `final` desnecessário em parâmetros e variáveis locais** — nunca escrever `final String x` em
  parâmetro de método/construtor nem `final Foo y = ...` em variável local (nem em `catch`/`for`).
  `final` só se mantém em campos de classe (`private final Foo bar;`, exigido por
  `@RequiredArgsConstructor`/imutabilidade) e em constantes (`private static final`).
- **Variáveis de ambiente com default local:** `${VARIAVEL:valor-default}` no `application.yml`
- **Virtual Threads:** `spring.threads.virtual.enabled=true` em todos os serviços (Java 25)
- **Log:** sempre `@Slf4j` (Lombok, `lombok.extern.slf4j.Slf4j`) — nunca instanciar `Logger`/`LoggerFactory` manualmente. Usar campo `log` gerado pela anotação (ex: `log.info(...)`, `log.error(...)`). **Nunca usar `@Log4j`/`@Log4j2`** — projeto roda em SLF4J + Logback (padrão Spring Boot), não Log4j.
- **Sem `record` declarado dentro de classe** — nested record é proibido (garantido mecanicamente
  pelo checkstyle, módulo `DescendantToken` de `CLASS_DEF` para `RECORD_DEF`, `maximum=0`). Record
  continua permitido, mas sempre top-level em arquivo próprio.
- **Resposta JSON sempre em `snake_case`** — campo Java continua `lowerCamelCase` (convenção normal
  da linguagem); só a serialização de saída HTTP vira `snake_case`. Configurar globalmente em
  `application.yml` de cada um dos 9 serviços do MVP que expõem JSON (todos exceto o API Gateway,
  que só roteia e não serializa payload de domínio):
  ```yaml
  spring:
    jackson:
      property-naming-strategy: SNAKE_CASE
  ```
- **Versão do artefato sempre `1.0.0`** — `<version>1.0.0</version>` na tag `<project>` do `pom.xml`
  (serviços Maven) ou `version = '1.0.0'` no `build.gradle` (serviços Gradle) de cada um dos 9
  serviços do MVP — nunca manter o default `0.0.1-SNAPSHOT` do Spring Initializr.
- **Sem nenhum comentário no código dos microsserviços** — nem `//`, nem `/* */`, nem Javadoc; catch
  vazio sem comentário já é bloqueado mecanicamente pelo checkstyle (`EmptyCatchBlock` sem exceção,
  nem `ignored`/`expected`). Documentação técnica do serviço vive em `docs/apps/<nome-servico>.md`
  (gate de conclusão já existente) — comentário no código fica redundante e é proibido.

### Checkstyle — obrigatório em todo código gerado

Todo código Java gerado (novo ou alterado) **deve nascer em conformidade** com
`infra/checkstyle/checkstyle.xml`, compartilhado por todos os microsserviços via
`maven-checkstyle-plugin` (fase `validate`, `failsOnError=true`). O plugin valida também
`src/test/` (`includeTestSourceDirectory=true`) — código de teste segue as mesmas regras do código
de produção, sem exceção. Não gerar código e corrigir depois — aplicar direto ao escrever. Regras
principais:

- **Formatação de classes:**
  - Linha em branco logo após a chave `{` de abertura da declaração da classe (antes do primeiro membro).
  - Linha em branco antes da chave `}` de fechamento final da classe.
  - Indentação: 4 espaços por nível, nunca tab (`FileTabCharacter`).
  - Sem espaços em branco no fim de linha; arquivo termina com newline.
  - Linhas com no máximo 130 caracteres.
- **Sem números mágicos:** todo literal numérico fora de `-1, 0, 1, 2` vira `private static final` nomeado (`MagicNumber`).
- **Exceções:**
  - Nunca lançar tipos genéricos (`RuntimeException`, `Exception`, `Throwable`, `Error`) — criar exceção de domínio específica (`IllegalThrows`).
  - Nunca capturar `RuntimeException`, `Error` ou `Throwable` (`IllegalCatch`); catch vazio é proibido sem exceção nenhuma (`EmptyCatchBlock` endurecido — nem comentário explicando, nem nome de variável `ignored`/`expected` liberam o bypass).
- **Sem `record` aninhado:** `record` declarado dentro de outra classe é bloqueado (`DescendantToken`, `CLASS_DEF`→`RECORD_DEF`, `maximum=0`) — só é permitido top-level em arquivo próprio.
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
  - **Nunca usar `@ManyToOne`/`@OneToMany`/`@OneToOne`/`@ManyToMany`** — relacionamento entre entidades sempre mapeado como o `id` (UUID) da outra entidade em um `@Column` simples (ex.: `categoryId`, `brandId`), nunca a entidade inteira. Resolver nome/dado relacionado explicitamente no `Service` (chamando o `Service` do domínio dono, nunca o `Repository` de outro domínio direto) e montar isso no DTO de resposta — nunca via join automático do Hibernate.
  - Coleção mantida por outra entidade (ex.: imagens de um produto) nunca vira campo dentro da entidade dona (ex.: `Product` não tem `List<ProductImage>`) — a entidade filha referencia o pai só pelo id (`productId`), e quem quer a lista busca via `Repository`/`Service` da entidade filha.
- **Services:**
  - Injeção de dependência sempre via `@RequiredArgsConstructor` (Lombok) com campos `private final` — nunca `@Autowired` em campo ou construtor manual.
  - Um `Service` só pode chamar outro `Service` (ou `Repository` do próprio domínio) — nunca acessar `Repository` de outro serviço/domínio diretamente, e nunca acessar `Controller`.
- **Lombok:** usar para reduzir boilerplate (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Builder`, `@Slf4j`, etc.) em entities, DTOs e services — nunca escrever getters/setters/construtores manuais quando Lombok resolve.
- **MapStruct:** toda conversão Entity ↔ DTO usa `@Mapper` de MapStruct — nunca mapeamento manual campo a campo em service ou controller.
- **Validações em Request DTOs:** toda anotação Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Email`, etc.) sempre com `message` explícita — nunca deixar mensagem default do framework. Regra vale para todo request DTO dos 9 serviços do MVP, sem exceção.
- **`@Override` em controllers:** não usar em métodos de controller (nem quando implementa interface de docs como `*ControllerDocs`) — só quando realmente necessário (ex.: sobrescrita de método de classe abstrata onde o compilador não infere o contrato sozinho).
- **Retorno de endpoints em controllers:** todo método de controller retorna `ResponseEntity` construído de forma explícita, sempre no formato `return ResponseEntity.status(HttpStatus.X).body(response);` (ou `.status(HttpStatus.X).build();` quando não há corpo) — `HttpStatus` sempre explícito, inclusive para `200 OK`. Nunca usar os atalhos `ResponseEntity.ok(...)`, `.noContent()`, `.created(...)`, `.accepted()`, `.badRequest()`, etc.
- **Nome de método de leitura (`find`):** todo método que busca/lista dado — `@GetMapping` de controller, método de `Service` chamado por ele, e método de `Repository` — começa com `find` (ex.: `findProducts`, `findProductById`, `findProductBySlug`, `findCategories`, `findUserByEmail`). Nunca `get*`/`list*`/`search*`/outros verbos nessas três camadas.
  - Exceção: método de query derivada do Spring Data (`JpaRepository`) que usa prefixo reservado do parser (`existsBy*`, `countBy*`, `deleteBy*`) mantém o prefixo reservado — não pode virar `find*` sem quebrar o parsing. `findBy*` já está correto e não muda.
  - Getter simples de campo/valor calculado (ex.: `getRemainingTtlSeconds` de um token) não é "busca de dado" — não entra nessa regra.

### Snyk — obrigatório em todo microsserviço novo

Ao criar um microsserviço do zero, depois de adicionar todas as dependências necessárias (`pom.xml`
ou `build.gradle`), rodar `snyk test` (com `--all-sub-projects --detection-depth=6` para cobrir
também dependências de escopo `test`) dentro da pasta do serviço antes de considerar a fundação
pronta.

- Se aparecer qualquer CVE, corrigir atualizando a versão da dependência afetada — nunca ignorar,
  suprimir ou usar `.snyk` policy para mascarar o resultado.
- Preferir resolver pela raiz: subir a versão do BOM pai (`spring-boot-starter-parent`,
  `spring-cloud-dependencies`) antes de pinar dependência transitiva isolada — várias CVEs costumam
  cair juntas com um único bump de BOM.
- Dependência transitiva que sobrar depois do bump de BOM (ex.: `commons-compress`, `lz4-java`,
  `scala-library` vindos de Kafka/Testcontainers/Zookeeper) resolver com override explícito em
  `dependencyManagement` (Maven) ou bloco `dependencyManagement { dependencies { ... } }` (Gradle,
  plugin `io.spring.dependency-management`), fixando a versão sem CVE.
- Repetir `snyk test` depois de cada rodada de correção até o resultado dar `ok: true` / zero
  vulnerabilidades — nunca declarar a correção concluída sem essa confirmação.
- Se a única correção disponível para uma CVE exigir salto de versão MAJOR do Spring Boot/Framework/
  Security/Cloud (ex.: Boot 3.5.x → 4.0.x), aplicar o salto e revisar os pontos de maior risco de
  quebra de comportamento do serviço (config de segurança, serializadores Jackson, clientes
  OpenFeign/Kafka, Springdoc — que precisa ir para a linha 3.x quando o Boot for 4.x) — nunca deixar
  CVE residual sem justificar por que não foi corrigida.

---

## Padrão de Testes — obrigatório em todo microsserviço (unitário e aceitação)

> Padrão extraído do **API Gateway** (`backend/api-gateway/src/test/`), primeiro serviço do MVP a
> ter a suíte de testes finalizada e validada. É o padrão oficial para os demais 8 serviços do MVP
> e para os 3 pós-MVP — `backend-engineer` segue ao criar/evoluir testes, `quality-analyst` segue
> ao revisar/re-executar. Nenhum dos dois reinventa estrutura, nomenclatura ou convenção Gherkin
> diferente da descrita aqui sem alinhar antes com o software-architect.

### Estrutura de pastas

```
src/test/
├── java/com/autohubstore/<service>/
│   ├── unit/
│   │   └── <camada>/            # ex.: unit/service/, unit/controller/ — espelha o pacote de produção
│   └── acceptance/
│       ├── config/              # CucumberConfig (@CucumberContextConfiguration + @SpringBootTest),
│       │                        # CucumberTest (runner @Suite), WireMockSupport (se o serviço
│       │                        # depender de chamada externa síncrona — OpenFeign/HTTP)
│       ├── steps/                # <Servico>AcceptanceSteps.java — uma classe de steps por .feature
│       └── util/                  # Http[Acceptance]TestUtil.java — único ponto de disparo HTTP do cenário
└── resources/
    └── features/
        └── <servico>.feature     # um .feature por serviço (ou por bounded context relevante do serviço)
```

### Testes unitários

- JUnit 5 + Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock` para colaboradores, `@InjectMocks`
  para a classe sob teste (ver `unit/service/RateLimitServiceTest.java`); quando não há colaborador a
  mockar, instanciar a classe direto em `@BeforeEach` (ver `unit/service/JwtServiceTest.java`).
- Todo método de teste tem `@DisplayName("Deve ... quando ...")` em português, descrevendo
  comportamento esperado e condição.
- Nome do **método** de teste sempre em inglês, estilo `should<Comportamento><Condição>` (ex.:
  `shouldThrowExceptionForExpiredToken`, `shouldBlockRequestExceedingPublicLimit`).
  Nome da **classe** de teste: `<ClasseSobTeste>Test`.
  Valor numérico/literal repetido some para `private static final` nomeado, igual à regra de
  `MagicNumber` do checkstyle (ex.: `ONE_HOUR_MS`, `PUBLIC_LIMIT`, `RATE_LIMIT_WINDOW_REQUESTS`).
- Fluxo reativo (`Mono`/`Flux`, serviços WebFlux) sempre validado com `reactor.test.StepVerifier`
  (`.expectNext(...).verifyComplete()`) — nunca `.block()` em teste.
- `assertThat`/`assertThatThrownBy` de AssertJ para asserção — nunca `assertEquals`/`assertTrue` puro
  do JUnit.

### Testes de aceitação (Cucumber)

**Arquivo `.feature`:**
- `# language: pt` sempre na primeira linha do arquivo.
- Keyword `Entao` **sempre sem acento** (nunca "Então") — as demais (`Dado`, `Quando`, `E`) seguem a
  grafia padrão em português.
- Steps redigidos no **infinitivo, com sujeito explícito** e coerente com o papel do ator no cenário
  (ex.: "o cliente possuir um cookie de acesso valido", "o gateway receber uma requisicao de
  preflight CORS", "o user-service estar saudavel") — nunca em 1ª pessoa ("eu faço...") nem em forma
  impessoal sem sujeito ("é enviado...").
- Path de endpoint (`/api/v1/...`) **nunca aparece como literal no `.feature`** — o texto do cenário
  fica no nível de negócio ("o endpoint de usuarios", "esse endpoint", "o catalogo"); a resolução do
  path concreto é sempre feita no step (`private static final String ..._ENDPOINT_PATH = "..."`).
- `Esquema do Cenario` + tabela `Exemplos` para variações do mesmo fluxo com dados diferentes (ex.:
  token expirado vs. assinatura inválida no mesmo cenário de "token invalido chega a endpoint
  protegido") — evita duplicar cenário quase idêntico.

**Classe de steps (`acceptance/steps/<Servico>AcceptanceSteps.java`):**
- Anotações sempre de `io.cucumber.java.pt.*` (`@Dado`, `@Quando`, `@Entao`) — nunca a variante em
  inglês (`io.cucumber.java.en.*`).
- Texto da expressão Cucumber dentro da anotação bate **1:1** com o texto do `.feature` (inclusive
  sem acento em "Entao").
- Nome do **método** do step sempre em inglês, prefixado por `given`/`when`/`then` conforme o papel
  (`givenTheClientHasAValidAccessCookie`, `whenTheClientCallsWithTheAccessCookie`,
  `thenTheClientReceivesResponseWithStatus`).
- Path de endpoint, segredo de teste, valor de janela/limite: sempre `private static final String`/
  `int`/`long` nomeado no topo da classe — nunca literal solto dentro do corpo do step.
- **Toda chamada HTTP do cenário passa obrigatoriamente pelo util de aceitação**
  (`Http[Acceptance]TestUtil`, injetado via `@Autowired`) — a classe de steps nunca dispara
  `WebTestClient`/`MockMvc` diretamente solta no corpo do método de step.
- Dependência externa síncrona do serviço (ex.: Gateway → User Service) é simulada com WireMock via
  `WireMockSupport` (`server().stubFor(...)`) — nunca subir a dependência real no teste de aceitação.

**Util de aceitação (`acceptance/util/Http[Acceptance]TestUtil.java`):**
- `@Component`, injeta o cliente HTTP de teste via `@Autowired`, expõe um método por tipo de chamada
  do cenário (`executeGetWithCookie`, `executeGetWithoutCookie`, `executeGetRepeatedly`,
  `executeCorsPreflight`) — é o único lugar do módulo de aceitação que conhece o cliente HTTP.
- **Stack de cliente HTTP depende do modelo de concorrência do serviço, nunca escolher por
  preferência:**
  - **`WebTestClient`** (`org.springframework.test.web.reactive.server`) — serviços **reativos**
    (WebFlux). No MVP, hoje só o API Gateway.
  - **`MockMvc`** (`@AutoConfigureMockMvc`, `org.springframework.test.web.servlet`) — todos os
    **demais serviços MVC/servlet** do MVP e pós-MVP (Auth, User, Catalog, Cart, Inventory, Order,
    Payment, Notification, e os 3 pós-MVP).

**Configuração de aceitação (`acceptance/config/`):**
- `CucumberConfig`: `@CucumberContextConfiguration` + `@SpringBootTest(webEnvironment = ...)` +
  `@AutoConfigureWebTestClient(timeout = "PT10S")` (reativo) **ou** `@AutoConfigureMockMvc` (servlet)
  + `@ActiveProfiles("test")`. Dependência de infraestrutura do serviço (Redis, PostgreSQL, Kafka)
  sobe via **Testcontainers** (`GenericContainer`/módulo específico) com `@DynamicPropertySource`
  apontando a propriedade de conexão para o container — nunca infraestrutura real do
  `docker-compose.yml` local dentro do teste automatizado.
- `CucumberTest`: runner via JUnit Platform Suite — `@Suite`, `@IncludeEngines("cucumber")`,
  `@SelectPackages("features")`, `@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME,
  value = "com.autohubstore.<service>.acceptance")`.
- `WireMockSupport`: classe utilitária estática (`private` construtor, classe `final`), servidor
  WireMock em porta dinâmica iniciado uma vez em bloco `static { ... }` — só existe quando o serviço
  tem dependência HTTP síncrona externa a simular (ex.: Auth Service → User Service via OpenFeign,
  Cart Service → Catalog Service, Order Service → Cart/User Service).

Referência completa e funcional do padrão: `backend/api-gateway/src/test/`.

---

## Tópicos Kafka

| Tópico | Producer | Consumer(s) |
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

> Consumers marcados como _pós-MVP_ só existem quando Search/Analytics/Compatibility Service forem
> implementados — não bloqueiam a publicação do evento hoje, apenas não têm consumer ativo no MVP.

---

## Mapa de Dependências entre Serviços

```
API Gateway ──────────────────────────────────────→ Todos os serviços do MVP (roteamento + JWT)
Auth Service ───── OpenFeign ──────────────────────→ User Service (verify-credentials, update-password)
Cart Service ────── OpenFeign ────────────────────→ Catalog Service
Catalog Service ─── OpenFeign ────────────────────→ Inventory Service (disponibilidade)
Order Service ───── OpenFeign ────────────────────→ Cart Service, User Service
Inventory      ←─── Kafka (order.created, payment.*) ─ Order Service, Payment Service
Notification   ←─── Kafka (todos os eventos) ─────── User, Auth, Order, Payment
Payment ──────────── Kafka ────────────────────────→ Order Service (resultado pagamento)
Order ←───────────── Kafka (stock-insufficient) ──── Inventory Service

# Pós-MVP (não fazem parte do fluxo atual):
Search Service ←─── Kafka (product.created/updated) ─ Catalog Service
Analytics      ←─── Kafka (product-viewed, order) ─── Catalog, Order
Catalog Service ─── OpenFeign ────────────────────→ Compatibility Service (veículos compatíveis)
```

---

## Infraestrutura Local (docker-compose)

| Serviço | Porta | Descrição |
|---|---|---|
| postgres-auth | 5432 | Banco do Auth Service (`auth_db`) |
| postgres-user | 5433 | Banco do User Service (`user_db`) |
| postgres-catalog | 5434 | Banco do Catalog Service |
| postgres-order | 5435 | Banco do Order Service |
| postgres-payment | 5436 | Banco do Payment Service |
| postgres-inventory | 5437 | Banco do Inventory Service |
| redis | 6379 | Cache + Carrinho + Blacklist + Rate Limit |
| zookeeper | 2181 | Kafka coordinator |
| kafka | 9092 | Message broker |
| prometheus | 9090 | Métricas |
| grafana | 3011 | Dashboards |
| jaeger | 16686 | Traces distribuídos |
| mailhog | 8025 | SMTP local (testes de e-mail) |

> `elasticsearch`/`kibana` (Search Service), `cassandra` (Analytics Service) e `mongo-compatibility`
> (Compatibility Service) só entram no `docker-compose.yml` quando esses serviços pós-MVP forem
> implementados — não são necessários para subir o MVP de 9 serviços.

---

## Roadmap — Fases do MVP (40% Spec+Arquitetura / 20% Dev / 40% Testes)

Cada fase de microsserviço segue a mesma divisão de esforço, detalhada por serviço em
[docs/planning/action-plan.md](docs/planning/action-plan.md): **40% spec+arquitetura** (use cases,
PRD, critérios de aceite, DER Mermaid), **20% desenvolvimento** (implementação por
backend-engineer/frontend-engineer) e **40% testes** (unitários, aceitação, e2e, segurança, UI).
Um serviço só é considerado **pronto** com testes unitários **e** de aceitação passando.

| Fase | Objetivo | Microsserviço(s) do MVP |
|---|---|---|
| 1 | Fundação e infraestrutura | API Gateway ✅ |
| 2 | Identidade e autenticação | User Service, Auth Service |
| 2.5 | Integração Frontend ↔ Backend (gate) | — (frontend consome Auth/User/Gateway reais) |
| 3 | Catálogo | Catalog Service |
| 4 | Carrinho | Cart Service |
| 5 | Estoque e pedidos | Inventory Service, Order Service |
| 6 | Pagamentos | Payment Service |
| 7 | Notificações | Notification Service |
| 8 | Observabilidade | OTel em todos os serviços do MVP |
| 9 | Deploy e produção | VPS Hostinger + Kubernetes |
| Pós-MVP | Busca, Analytics, Compatibilidade | Search Service, Analytics Service, Compatibility Service |

Detalhes completos: `docs/planning/action-plan.md`

---

## Decisões Arquiteturais (ADRs)

| ADR | Decisão | Justificativa resumida |
|---|---|---|
| ADR-001 | Java 25 com Virtual Threads | LTS + alta concorrência sem callbacks |
| ADR-002 | Database per Service | Autonomia total, sem acoplamento de banco |
| ADR-003 | Apache Kafka | Padrão event-driven, replay, Consumer Groups |
| ADR-004 | Spring Cloud Gateway | Ecossistema Spring nativo, filtros customizáveis |
| ADR-005 | Elasticsearch para busca (pós-MVP) | Full-text scoring, analyzers PT, filtros eficientes |
| ADR-006 | Clean/Hexagonal Architecture | Domínio testável sem Spring |
| ADR-007 | Cassandra para Analytics (pós-MVP) | Otimizado para alta escrita, COUNTER nativo |
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

## Time de Engenharia (Agentes Claude Code)

Este projeto opera com um time de agentes autônomos definidos em `.claude/agents/*.md`
(formato lido nativamente pelo Claude Code).

**Padrão: orquestrador-trabalhador.** Ponto único de entrada é **software-architect** — toda
tarefa de produto, backend, frontend ou QA deve ser endereçada a ele primeiro; é o único agente
com a tool `Agent`, e é quem aciona os demais internamente. Nunca invocar backend-engineer,
frontend-engineer, product-owner ou quality-analyst diretamente a partir da conversa principal.

| Agente | Papel | Aciona |
|---|---|---|
| **software-architect** | Gerente — arquitetura, distribuição de tarefas, validação final de aderência a ADRs | product-owner, backend-engineer, frontend-engineer, quality-analyst |
| **product-owner** | Specs de microsserviço, histórias de usuário, critérios de aceite | — (especialista) |
| **backend-engineer** | Implementa os microsserviços Java/Spring Boot | — (especialista) |
| **frontend-engineer** | Implementa e evolui o e-commerce Next.js/React | — (especialista) |
| **quality-analyst** | Valida entregas de backend/frontend contra os critérios de aceite do PO | — (especialista) |

Regras válidas para todos os agentes do time (reforçam o resto deste `CLAUDE.md`):
- **Bash é escopado, não universal.** `backend-engineer`, `frontend-engineer` e `quality-analyst`
  têm acesso a `Bash` limitado a build/teste/lint/checkstyle/Snyk do próprio serviço em que estão
  trabalhando (`mvn`/`gradle` compile, test, `checkstyle:check`, `snyk test`, `npm run build`/`test`
  no frontend) — usado para produzir e verificar as 8 evidências do [Critério de Conclusão de
  Microsserviço](docs/planning/action-plan.md#critério-de-conclusão-de-microsserviço) antes de
  entregar. `quality-analyst` usa o mesmo Bash escopado para **re-executar** essas verificações de
  forma independente antes de aprovar, nunca só confiando no relato do dev.
  `software-architect` e `product-owner` **não têm** Bash — seguem apenas leitura/edição de
  specs, ADRs e documentação de planejamento.
- **Nunca**, em nenhum agente (inclusive os com Bash escopado): rodar `git` (nenhum subcomando,
  inclusive `status`/`diff`/`log` read-only) nem `docker compose up`/`down`/qualquer variação —
  isso é decisão e ação exclusivas do usuário. `docker compose up -d` de infraestrutura local
  continua rodado só pelo usuário, nunca por um agente.
- Nunca aprovar/entregar tarefa sem os critérios de aceite da spec do product-owner terem sido
  validados pelo quality-analyst, e sem os 8 itens do Critério de Conclusão de Microsserviço
  satisfeitos (testes unitários, aceitação Cucumber, cobertura ≥ 70%, checkstyle, Snyk, build,
  DER atualizado, `docs/apps/<nome-servico>.md`).
- **Toda criação de microsserviço novo ou validação de "está finalizado" termina exibindo ao
  usuário a tabela de evidência dos 8 itens** (formato em
  [action-plan.md § Tabela de evidência — obrigatória ao final](docs/planning/action-plan.md#tabela-de-evidência--obrigatória-ao-final)),
  com status ✅/❌ de cada item — nunca declarar "Concluído" sem mostrar essa tabela, e mostrá-la
  também quando algum item falhar/estiver pendente.
- Nunca expor segredo/credencial em spec, ADR, código ou log.
- JDK do projeto para qualquer execução local dos agentes com Bash: `C:\Users\jeanc\.jdks\ms-25.0.4`
  (`JAVA_HOME`).

---

## Documentação

| Pasta | Conteúdo |
|---|---|
| `docs/apps/api-gateway.md` | Explicação didática completa do Gateway (Hexagonal, JWT, rate limit, circuit breaker) — único `docs/apps/*.md` já escrito; os demais 8 serviços do MVP ganham o próprio arquivo como gate de conclusão (ver § Critério de Conclusão de Microsserviço) |
| `docs/planning/action-plan.md` | Plano de ação das fases do MVP (40% spec+arquitetura / 20% dev / 40% testes) com critério de conclusão |
| `docs/planning/der/*.mmd` | Diagramas DER (Mermaid) por microsserviço do MVP |
| `docs/planning/specs/01-api-gateway.md` | Spec técnica do API Gateway (deps, endpoints, estrutura) |
| `docs/planning/specs/02-auth-service.md` | Spec do Auth Service — login/logout/refresh/reset senha (MVC, JWT, OpenFeign → User Service) |
| `docs/planning/specs/03-user-service.md` | Spec do User Service — cadastro, perfil, endereços (Clean Architecture, eventos Kafka) |
| `docs/planning/specs/06-inventory-service.md` | Spec do Inventory Service — reserva de estoque (Hexagonal, Saga via Kafka) |
| `docs/planning/specs/04-catalog-service.md`, `05-cart-service.md`, `07-order-service.md`, `08-payment-service.md`, `09-notification-service.md` | Specs dos demais microsserviços do MVP |
| `docs/planning/specs/10-search-service.md`, `11-analytics-service.md`, `12-compatibility-service.md` | Specs pós-MVP (fora do escopo atual) |
