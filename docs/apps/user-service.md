# User Service

**Build Tool:** Maven | **Arquitetura:** Clean Architecture | **Porta:** 8003 | **Status:** Concluído (ver § Evidências)

**Código:** `backend/user-service/` | **Spec:** [docs/planning/specs/03-user-service.md](../planning/specs/03-user-service.md)
| **DER:** [docs/planning/der/user-service.mmd](../planning/der/user-service.mmd)

## Objetivo

O User Service é o dono do cadastro, do perfil e dos endereços de entrega dos usuários do
AutoHubStore. Ele **não lida com autenticação** (login, emissão/refresh de token, sessão) — essa
responsabilidade é do Auth Service, que consome os endpoints internos deste serviço
(`/internal/v1/users/**`, não roteados pelo Gateway) via OpenFeign para validar credencial
(`verify-credentials`) e atualizar senha no fluxo de reset.

## Bounded context: Auth vs. User

Auth Service e User Service foram extraídos de um único serviço original porque representam
bounded contexts distintos, ainda que dependentes um do outro:

- **User Service**: dono do dado cadastral (e-mail, nome, hash de senha, status, papel,
  endereços). Fonte única de verdade para "quem é o usuário".
- **Auth Service**: dono da sessão (emissão/validação/revogação de access token, refresh token,
  fluxo de reset de senha). Não guarda dado de perfil — chama o User Service via OpenFeign para
  validar credencial e persistir a nova senha.

Essa separação evita que regra de autenticação (JWT, blacklist, TTL de token) vaze para o domínio
de cadastro, e vice-versa. Ver `docs/planning/action-plan.md § Decisões de Consolidação` para o
racional completo.

## Arquitetura (Clean Architecture)

```
com.autohubstore.userservice/
├── UserServiceApplication.java
├── domain/
│   ├── model/
│   │   ├── User.java                  # POJO puro (Lombok), sem anotação JPA
│   │   ├── Address.java               # POJO puro
│   │   ├── UserRole.java              # USER, ADMIN
│   │   └── UserStatus.java            # ACTIVE, INACTIVE, BLOCKED
│   ├── event/
│   │   └── UserCreatedEvent.java      # payload do tópico Kafka user.created
│   ├── repository/                    # portas de saída (output boundary), sem Spring Data
│   │   ├── UserRepository.java
│   │   ├── AddressRepository.java
│   │   └── UserEventPublisher.java    # porta de publicação de evento (implementada em infra)
│   └── service/
│       └── PasswordDomainService.java # hash/verificação BCrypt — sem @Service, bean via DomainConfig
├── application/
│   ├── dto/
│   │   ├── request/                   # CreateUserRequest, UpdateUserRequest, AddressRequest, ...
│   │   └── response/                  # UserResponse, AddressResponse
│   ├── mapper/                        # UserMapper, AddressMapper (MapStruct — domínio <-> DTO)
│   └── usecase/                       # input boundary: interface + <Nome>Impl
│       ├── CreateUserUseCase(Impl)
│       ├── FindUserUseCase(Impl)
│       ├── UpdateUserUseCase(Impl)
│       ├── UpdatePasswordUseCase(Impl)
│       ├── VerifyCredentialsUseCase(Impl)
│       └── ManageAddressUseCase(Impl)
├── infrastructure/
│   ├── web/
│   │   ├── UserController.java            # /api/v1/users/**
│   │   ├── AddressController.java         # /api/v1/users/{userId}/addresses/**
│   │   ├── UserInternalController.java    # /internal/v1/users/** (consumido pelo Auth Service)
│   │   └── docs/                          # *ControllerDocs — anotações Springdoc separadas do controller
│   ├── persistence/
│   │   ├── UserJpaEntity.java / AddressJpaEntity.java     # entidades JPA reais (@Entity)
│   │   ├── UserJpaRepository.java / AddressJpaRepository.java  # Spring Data
│   │   ├── User/AddressPersistenceMapper.java             # MapStruct — domínio <-> JPA entity
│   │   └── User/AddressRepositoryAdapter.java              # implementa domain/repository/*
│   ├── messaging/
│   │   └── UserEventPublisher.java    # KafkaTemplate — implementa domain.repository.UserEventPublisher
│   └── config/
│       ├── SecurityConfig.java, KafkaProducerConfig.java, OpenApiConfig.java, DomainConfig.java
│       └── security/
│           ├── JwtService.java             # só valida (nunca emite) access token
│           ├── TokenBlacklistService.java  # consulta blacklist no Redis
│           ├── JwtAuthenticationFilter.java
│           └── TokenClaims.java
└── exception/
    ├── EmailAlreadyExistsException, UserNotFoundException, AddressNotFoundException,
    │   InvalidCredentialsException, InactiveAccountException, SecurityInitializationException
    └── handler/GlobalExceptionHandler.java   # @RestControllerAdvice — ProblemDetail (RFC 7807)
```

### Por que a entidade JPA não é o modelo de domínio

Em Clean Architecture o domínio (`domain/model/User`, `domain/model/Address`) não pode depender de
`jakarta.persistence`. A entidade JPA real (`infrastructure/persistence/UserJpaEntity`) é um
detalhe de infraestrutura: mapeia cada coluna explicitamente via `@Column`, implementa
`@PrePersist`/`@PreUpdate` para os timestamps de auditoria, e é convertida de/para o modelo de
domínio por um `@Mapper` MapStruct dedicado (`UserPersistenceMapper`/`AddressPersistenceMapper`).
O `UserRepositoryAdapter`/`AddressRepositoryAdapter` implementam as interfaces de
`domain/repository/` chamando o `JpaRepository` do Spring Data e convertendo o resultado com esse
mapper — os casos de uso (`application/usecase/`) nunca importam `jakarta.persistence` nem
`org.springframework.data.jpa`.

### Relacionamento sem `@ManyToOne`/`@OneToMany`

`AddressJpaEntity` referencia o usuário apenas pelo `userId` (`UUID`, coluna simples) — nunca por
`@ManyToOne`. `User` não tem `List<Address>`. Quem quer os endereços de um usuário busca via
`AddressRepository.findAllByUserId(userId)`; a garantia de que o usuário existe é feita chamando
`UserRepository.existsById(userId)` dentro do próprio `ManageAddressUseCaseImpl` — nunca acessando
o repositório de outro domínio diretamente (aqui os dois repositórios pertencem ao mesmo serviço/
bounded context, então essa checagem é interna, não uma chamada cross-service).

### Resource server JWT (por que valida mas nunca emite token)

`JwtService`/`TokenBlacklistService`/`JwtAuthenticationFilter`
(`infrastructure/config/security/`) existem só para o User Service se proteger como *resource
server*: o Auth Service emite o access token (cookie httpOnly `access_token`) no login; o User
Service, ao receber uma requisição em `/api/v1/users/**`, extrai o token do cookie, valida
assinatura/expiração (`JwtService`, JJWT — só leitura, nunca `Jwts.builder()`), confere se o `jti`
não está na blacklist do Redis (`TokenBlacklistService`, compartilhando o mesmo Redis do Gateway/
Auth) e popula o `SecurityContextHolder` com o `userId` como principal. `/internal/v1/users/**` e
os endpoints de infraestrutura (`/actuator/**`, `/swagger-ui/**`) ficam fora dessa checagem
(`SecurityConfig#PUBLIC_ENDPOINTS`) porque só são alcançáveis dentro da rede Docker — nunca
expostos pelo Gateway.

## Endpoints públicos (roteados pelo Gateway, `/api/v1/users/**`)

| Método | Path | Autenticação | Descrição |
|---|---|---|---|
| POST | `/api/v1/users` | pública | Cadastro de usuário |
| GET | `/api/v1/users/me` | cookie `access_token` | Perfil do usuário autenticado |
| GET | `/api/v1/users/{id}` | cookie `access_token` | Perfil por id |
| PUT | `/api/v1/users/{id}` | cookie `access_token` | Atualiza nome completo |
| PUT | `/api/v1/users/{id}/password` | cookie `access_token` | Atualiza senha (hash BCrypt) |
| GET/POST | `/api/v1/users/{userId}/addresses` | cookie `access_token` | Lista/cria endereço |
| DELETE | `/api/v1/users/{userId}/addresses/{addressId}` | cookie `access_token` | Remove endereço |

## Endpoints internos (não roteados pelo Gateway, rede Docker apenas)

| Método | Path | Consumidor | Descrição |
|---|---|---|---|
| POST | `/internal/v1/users/verify-credentials` | Auth Service | Valida e-mail/senha no login |
| GET | `/internal/v1/users/{id}` | Auth Service | Busca usuário por id |
| GET | `/internal/v1/users/by-email/{email}` | Auth Service | Busca usuário por e-mail |
| PUT | `/internal/v1/users/{id}/password` | Auth Service | Atualiza senha (fluxo de reset) |

## Evento Kafka publicado

| Tópico | Producer | Consumer(s) | Payload |
|---|---|---|---|
| `user.created` | User Service | Notification Service | `UserCreatedEvent(userId, email, fullName, createdAt)` |

## Contrato JSON

Toda resposta é serializada em `snake_case` (`spring.jackson.property-naming-strategy:
SNAKE_CASE`), campo Java continua `lowerCamelCase`. Todo DTO de request usa Bean Validation com
`message` explícita (nunca mensagem default do framework).

## Testes

### Unitários (`src/test/java/.../unit/`)

- `service/PasswordDomainServiceTest` — hash difere do texto original, `matches` confirma/nega
  correspondência.
- `usecase/CreateUserUseCaseImplTest` — cria usuário e publica evento quando e-mail é novo; lança
  `EmailAlreadyExistsException` (sem persistir nem publicar evento) quando e-mail já existe.
- `usecase/UpdateUserUseCaseImplTest` — atualiza quando o id existe; lança `UserNotFoundException`
  quando não existe.
- `usecase/UpdatePasswordUseCaseImplTest` — atualiza o hash da senha; lança exceção quando usuário
  não existe.
- `usecase/VerifyCredentialsUseCaseImplTest` — credenciais válidas, e-mail não cadastrado, conta
  inativa/bloqueada, senha incorreta (4 cenários).
- `usecase/ManageAddressUseCaseImplTest` — cria endereço e limpa o padrão anterior quando marcado
  como default; erro ao criar para usuário inexistente; remove endereço pertencente ao usuário;
  erro ao remover endereço de outro usuário.

JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), asserções via AssertJ
(`assertThat`/`assertThatThrownBy`), `@DisplayName` em português, nome de método em inglês
(`should<Comportamento><Condição>`).

### Aceitação (Cucumber — `src/test/resources/features/user-service.feature`)

Cobre cadastro (sucesso, e-mail duplicado → 409, campo obrigatório ausente → 400 via `Esquema do
Cenario`), atualização de perfil, criação/remoção de endereço, e os dois endpoints internos
(`verify-credentials` correto/incorreto, atualização de senha). Contexto (`acceptance/config/
CucumberConfig.java`): `@SpringBootTest` (servlet, `MockMvc` via `@AutoConfigureMockMvc` —
diferente do Gateway, que é reativo e usa `WebTestClient`), infraestrutura via Testcontainers
(PostgreSQL, Kafka, Redis) com `@DynamicPropertySource`. Toda chamada HTTP do cenário passa pelo
`HttpAcceptanceTestUtil` (único ponto de disparo `MockMvc` do módulo de aceitação).

### Como rodar

```bash
cd backend/user-service
mvn validate        # checkstyle (inclui src/test)
mvn test             # unitários + aceitação Cucumber (JUnit Platform Suite) — exige Docker ativo
mvn verify           # idem + gate de cobertura Jacoco (mínimo 70% de linha)
mvn jacoco:report    # relatório HTML em target/site/jacoco/index.html
mvn package          # build final (target/user-service.jar)
snyk test --all-sub-projects --detection-depth=6
```

## Evidências da última execução (backend-engineer, 2026-09-15)

| Item | Status | Evidência |
|---|---|---|
| Testes unitários (`mvn test`) | ✅ | 21 testes, 0 falhas (`target/surefire-reports/com.autohubstore.userservice.unit.*`) — inclui `FindUserUseCaseImplTest`, adicionado nesta rodada para cobrir `FindUserUseCaseImpl` (antes sem teste próprio) |
| Testes de aceitação (Cucumber) | ✅ | 11 cenários/exemplos em `user-service.feature`, 0 falhas (`CucumberTest`, Testcontainers subindo PostgreSQL, Kafka e Redis via Docker Desktop local sem problema, Flyway migrando o schema normalmente) |
| Cobertura ≥ 70% | ✅ | 83,88% de cobertura de linha combinada (unitário + aceitação) — `target/site/jacoco/jacoco.csv` (333 linhas cobertas / 397 totais), acima do gate `jacoco.line.ratio.minimum=0.70` do `pom.xml` |
| `checkstyle:check` | ✅ | `mvn checkstyle:check` → `BUILD SUCCESS`, 0 violações (produção + testes) |
| `snyk test --all-sub-projects --detection-depth=6` | ✅ | `Tested 159 dependencies for known issues, no vulnerable paths found.` |
| `mvn clean package` | ✅ | Build compila, roda a suíte completa (32 testes) e empacota com sucesso (`target/user-service.jar`) |
| DER atualizado | ✅ | `docs/planning/der/user-service.mmd` já refletia `users`/`addresses`; adicionado campo `role` que faltava na tabela `USERS` (schema Flyway não foi alterado) |
| `docs/apps/user-service.md` | ✅ | Este arquivo |

### Correção aplicada nesta rodada (revalidação do quality-analyst)

A execução anterior (2026-09-14) reportava a suíte de aceitação como bloqueada por "Docker
environment não acessível" — diagnóstico incorreto. A causa real era um bug de compatibilidade
Jackson: `pom.xml` está em `spring-boot-starter-parent` `4.0.8`, cujo `JacksonAutoConfiguration`
expõe beans do tipo `tools.jackson.databind.json.JsonMapper` (Jackson 3), não
`com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2, presente no classpath só como
dependência `runtime` transitiva de `jjwt-jackson`). `HttpTestUtil` injetava
`@Autowired ObjectMapper` (Jackson 2), sem bean correspondente no contexto — o
`ApplicationContext` inteiro do módulo de aceitação falhava ao subir
(`UnsatisfiedDependencyException`), derrubando os 11 cenários de uma vez, não por falha de
infraestrutura Docker (que funcionou normalmente: Postgres, Kafka e Redis subiram via
Testcontainers sem qualquer erro de conectividade). Corrigido trocando o tipo injetado para
`tools.jackson.databind.json.JsonMapper` em `HttpTestUtil`.

Corrigido também: `PUT /api/v1/users/{id}` e `POST /api/v1/users/{id}/addresses` exigem usuário
autenticado (`SecurityConfig` — `anyRequest().authenticated()`), mas os cenários de atualização de
perfil e criação/remoção de endereço disparavam a requisição sem cookie `access_token`, retornando
`403`. `HttpTestUtil` ganhou `executePostAuthenticated`/`executePutAuthenticated`/
`executeDeleteAuthenticated`, que geram um JWT válido (mesma chave `jwt.secret` da aplicação) e o
anexam como cookie `access_token`, simulando um cliente já autenticado pelo Auth Service/Gateway.

### Refatoração realizada

Estrutura de pacotes migrada de MVC (`controller/service/repository/domain.{entity,dto,mapper,enums}/messaging`)
para Clean Architecture (`domain/{model,event,repository,service}`,
`application/{dto,mapper,usecase}`, `infrastructure/{web,persistence,messaging,config}`), sem
alterar contratos de API, regras de negócio ou eventos Kafka. `pom.xml`: versão `1.0.0` (era
`0.0.1`), plugin `jacoco-maven-plugin` (`0.8.14` — necessário para suportar bytecode Java 25/class
file major version 69; `0.8.12` falha ao instrumentar), regra `LINE COVEREDRATIO ≥ 0.70`. DTOs de
request ganharam `message` explícita em toda anotação Bean Validation (antes usavam mensagem
default do framework).

