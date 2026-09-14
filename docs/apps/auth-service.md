# Auth Service

**Build Tool:** Maven | **Arquitetura:** MVC (adaptada) | **Porta:** 8002 | **Status:** Concluído

**Código:** `backend/auth-service/` | **Spec:** [docs/planning/specs/02-auth-service.md](../planning/specs/02-auth-service.md)
| **DER:** [docs/planning/der/auth-service.mmd](../planning/der/auth-service.mmd)

## Objetivo

O Auth Service isola a responsabilidade de **Autenticação** (sessão/token) do restante do domínio de
usuário. Ele emite e revoga tokens (JWT de acesso + refresh token opaco), controla o fluxo de
esqueci/redefinir senha e publica o evento `user.password-reset` no Kafka para o Notification
Service — mas nunca guarda credencial nem dado cadastral do usuário. Essa separação existe porque
Autenticação e Perfil são bounded contexts distintos, mesmo dependendo um do outro (ver
`CLAUDE.md` § Decisões de Consolidação).

## Por que não tem tabela de usuário própria (ADR-002 — Database per Service)

O `auth_db` (PostgreSQL, porta 5432) só tem duas tabelas — `refresh_tokens` e
`password_reset_tokens` — e nenhuma delas guarda e-mail, senha ou nome do usuário. Todo dado de
identidade (e-mail, hash de senha, status da conta) pertence ao **User Service** e é acessado via
OpenFeign (`UserServiceClient`), nunca via join ou tabela replicada. Isso é a aplicação direta do
ADR-002 (Database per Service): o Auth Service só referencia o usuário pelo `user_id` (UUID), sem
foreign key cross-database — a mesma técnica usada em todo o monorepo para relacionamento entre
domínios (ver CLAUDE.md § Padrões de Implementação, proibição de `@ManyToOne`/`@OneToOne`).

## Arquitetura (MVC adaptado)

O padrão MVC dos serviços do MVP é adaptado trocando o `repository/` de acesso a dado de outro
domínio por um `client/` com `@FeignClient` — o Auth Service não tem `Repository` de usuário porque
não é dono desse dado.

```
com.autohubstore.authservice/
├── AuthServiceApplication.java
├── controller/
│   ├── AuthController.java            # login, logout, refresh, forgot/reset-password
│   ├── AuthCookieFactory.java         # monta os ResponseCookie httpOnly de access/refresh token
│   └── docs/
│       └── AuthControllerDocs.java    # contrato Springdoc (interface só de anotações OpenAPI)
├── service/
│   ├── AuthService.java               # orquestra login/logout/refresh/forgot/reset
│   ├── JwtService.java                # emite/valida o JWT de acesso (JJWT 0.13, HMAC-SHA)
│   ├── TokenService.java              # CRUD dos refresh tokens e dos tokens de reset de senha
│   └── TokenBlacklistService.java     # blacklist de access token revogado, no Redis
├── client/
│   ├── UserServiceClient.java         # @FeignClient — chamadas internas ao User Service
│   └── UserServiceGateway.java        # circuit breaker + retry + fallback sobre o client Feign
├── repository/
│   ├── RefreshTokenRepository.java
│   └── PasswordResetTokenRepository.java
├── domain/
│   ├── entity/
│   │   ├── RefreshToken.java
│   │   └── PasswordResetToken.java
│   ├── dto/
│   │   ├── TokenClaims.java
│   │   ├── request/    # LoginRequest, ForgotPasswordRequest, ResetPasswordRequest, ValidateCredentialsRequest
│   │   └── response/   # LoginResponse, UserVerificationResponse
│   └── event/
│       └── PasswordResetRequestedEvent.java
├── messaging/
│   └── PasswordResetEventPublisher.java   # publica user.password-reset no Kafka
├── exception/
│   ├── InvalidCredentialsException.java
│   ├── InactiveAccountException.java
│   ├── InvalidTokenException.java
│   ├── UserServiceUnavailableException.java
│   └── handler/
│       └── GlobalExceptionHandler.java    # @RestControllerAdvice — sempre ProblemDetail
└── config/
    ├── KafkaProducerConfig.java
    └── OpenApiConfig.java
```

## Fluxo de login

1. `POST /api/v1/auth/login` (`AuthController.login`) recebe `LoginRequest` (e-mail + senha,
   validado com Bean Validation, mensagens explícitas).
2. `AuthService.login` chama `UserServiceGateway.verifyCredentials`, que delega ao
   `UserServiceClient` (Feign) — `POST /internal/v1/users/verify-credentials` no User Service.
3. Credencial inválida → o User Service responde 401 (`FeignException.Unauthorized`), traduzido
   para `InvalidCredentialsException`; conta inativa/bloqueada → 403
   (`FeignException.Forbidden`), traduzido para `InactiveAccountException`. Ambas viram resposta
   `ProblemDetail` (401/403) no `GlobalExceptionHandler`.
4. Credencial válida → `JwtService.generateAccessToken` emite o JWT (claims `sub`, `email`,
   `roles`, `jti`, `iat`, `exp`) e `TokenService.createRefreshToken` gera um refresh token opaco
   (64 bytes aleatórios, Base64 URL-safe, via `SecureRandom`), revogando antes qualquer refresh
   token anterior do mesmo `user_id` (`revokeAllByUserId`) — um usuário só tem uma sessão de refresh
   ativa por vez.
5. `AuthController` monta a resposta `200 OK` sem corpo, com os dois cookies httpOnly no header
   `Set-Cookie` (`AuthCookieFactory`).

## Fluxo de logout

`POST /api/v1/auth/logout` lê os cookies `access_token`/`refresh_token` (`@CookieValue`, ambos
`required = false`). Se houver access token, `JwtService.extractClaims` extrai o `jti` e
`getRemainingTtlSeconds` calcula quanto falta para o token expirar naturalmente —
`TokenBlacklistService.blacklist` grava `token:blacklist:{jti}` no Redis com esse TTL restante
(nunca um TTL fixo, para não manter a entrada além da validade original do token). Se houver
refresh token, `TokenService.revokeRefreshToken` marca a linha correspondente em `refresh_tokens`
como `revoked = true`. A resposta sempre expira os dois cookies (`AuthCookieFactory.expired*`).

## Fluxo de refresh (com rotation)

`POST /api/v1/auth/refresh` exige o cookie `refresh_token` (`@CookieValue` obrigatório).
`TokenService.rotateRefreshToken`:

1. Busca o token por valor; não encontrado ou inválido/expirado (`RefreshToken.isValid()` — não
   revogado e `expiresAt` no futuro) → `InvalidTokenException` (401).
2. Revoga o token atual e cria um novo (`RefreshToken.create`) com novo valor e nova validade —
   **rotation**: o valor antigo nunca pode ser reutilizado, mesmo que ainda não tivesse expirado.
   Isso limita o dano de um refresh token vazado a uma única troca.
3. `AuthService.refresh` busca o usuário atualizado no User Service
   (`UserServiceGateway.findUserById`) para emitir um novo JWT com claims em dia, e devolve os dois
   cookies renovados, igual ao login.

## Fluxo de forgot-password / reset-password

- `POST /api/v1/auth/forgot-password`: busca o usuário por e-mail
  (`UserServiceGateway.findUserByEmail`). **Não encontrado é silenciosamente ignorado** (retorna
  sem lançar exceção) — evita enumeração de e-mails cadastrados. Encontrado, gera um token de reset
  (`TokenService.createPasswordResetToken`, TTL configurável via
  `auth.password-reset-ttl-minutes`, default 15 min) e publica `PasswordResetRequestedEvent` no
  Kafka. O endpoint sempre responde `202 Accepted`, com ou sem usuário encontrado.
- `POST /api/v1/auth/reset-password`: `TokenService.consumePasswordResetToken` valida o token
  (existe, não expirado, não usado — `PasswordResetToken.isValid()`), marca como usado
  (`markUsed()`, idempotência de uso único) e `AuthService.resetPassword` chama
  `UserServiceGateway.updatePassword` (`PUT /internal/v1/users/{id}/password` via Feign) para
  efetivar a nova senha no User Service. Resposta `204 No Content`.

## Cookies httpOnly

`AuthCookieFactory` centraliza a montagem dos `ResponseCookie` — nunca corpo JSON com o token, só
`Set-Cookie`:

| Cookie | Path | Atributos |
|---|---|---|
| `access_token` | `/` | `httpOnly`, `secure`, `sameSite=Lax`, `maxAge` = TTL do JWT |
| `refresh_token` | `/api/v1/auth/refresh` | `httpOnly`, `secure`, `sameSite=Lax`, `maxAge` = TTL do refresh token |

O `refresh_token` tem `path` restrito ao próprio endpoint de refresh — o browser não o envia em
nenhuma outra rota, reduzindo a superfície de exposição do token de vida mais longa. Mesma
motivação de segurança documentada no API Gateway (mitigar XSS lendo o JWT via cookie, nunca via
JavaScript/`Authorization` header).

## Integração OpenFeign com o User Service

`UserServiceClient` (`@FeignClient(name = "user-service", url = "${user-service.url}")`) declara os
quatro endpoints internos consumidos: `verify-credentials` (POST), `findUserById`/`findUserByEmail`
(GET) e `updatePassword` (PUT). Nenhum controller do Auth Service expõe esses métodos
diretamente — só `AuthService` chama `UserServiceGateway`, que por sua vez chama o `Feign Client`
(regra de "Service só chama Service/Repository do próprio domínio, nunca Repository de outro
domínio", aqui adaptada para "nunca Feign Client direto fora do Gateway").

## Circuit Breaker e Retry (Resilience4j)

`UserServiceGateway` decora as quatro chamadas Feign com `@CircuitBreaker(name = "userService")` +
`@Retry(name = "userService")`. Configuração (`application.yml`):

| Parâmetro | Valor |
|---|---|
| `slidingWindowSize` | 10 chamadas |
| `minimumNumberOfCalls` | 10 |
| `failureRateThreshold` | 50% |
| `waitDurationInOpenState` | 10s |
| `permittedNumberOfCallsInHalfOpenState` | 3 |
| Retry `maxAttempts` | 3, backoff exponencial (`500ms`, multiplicador 2) |

`ignoreExceptions` (tanto no circuit breaker quanto no retry) exclui
`FeignException.Unauthorized`, `FeignException.Forbidden` e `FeignException.NotFound` — essas são
respostas de negócio esperadas do User Service (credencial inválida, conta inativa, usuário/e-mail
inexistente), não falha de infraestrutura, e por isso não devem abrir o circuito nem disparar
retry.

Quando o circuito abre (ou o retry se esgota) por falha real de infraestrutura, o método de
fallback correspondente em `UserServiceGateway` (`verifyCredentialsFallback`, `findUserByIdFallback`
etc.) é chamado. `resolveFailure` decide o que fazer com a `Throwable` recebida: se for uma
`FeignException` (não deveria acontecer aqui, pois essas já estariam na lista de
`ignoreExceptions`, mas é a defesa correta caso surja outro subtipo), ela é relançada como está;
caso contrário — circuito aberto (`CallNotPermittedException`) ou timeout de retry — é logada como
`warn` e relançada como `UserServiceUnavailableException`. `GlobalExceptionHandler` traduz essa
exceção para `503 Service Unavailable` com `ProblemDetail` (título "Service Unavailable", detail
genérico "Servico de usuarios temporariamente indisponivel" — nunca vaza a stack trace/causa ao
cliente).

## Blacklist de token no Redis

`TokenBlacklistService.blacklist(jti, ttlSeconds)` grava a chave `token:blacklist:{jti}` no Redis
com o TTL igual ao tempo restante de validade do JWT (nunca um TTL fixo maior, para não reter a
entrada além do necessário). Essa é a forma de revogar um **access token ainda não expirado** no
logout: o JWT em si continua criptograficamente válido até `exp`, mas qualquer verificador que
consulte a blacklist (o próprio Auth Service ou, tipicamente, o API Gateway antes de rotear) trata
um `jti` presente na blacklist como inválido.

## Evento Kafka `user.password-reset`

`PasswordResetEventPublisher` (tópico `user.password-reset`, chave de partição = `userId`) publica
um `PasswordResetRequestedEvent` (record com `userId`, `email`, `token`, `expiresAt`) sempre que
`forgot-password` encontra o usuário. O único consumer no MVP é o **Notification Service**, que
envia o e-mail de redefinição de senha. Falha de publicação é logada (`error`) via callback
assíncrono do `KafkaTemplate` (`whenComplete`) — não interrompe a resposta HTTP `202 Accepted` já
enviada ao cliente.

## Estrutura das tabelas (`auth_db`)

| Tabela | Colunas | Observação |
|---|---|---|
| `refresh_tokens` | `id`, `user_id`, `token`, `expires_at`, `created_at`, `revoked` | índices em `user_id` e `token`; `token` `UNIQUE` |
| `password_reset_tokens` | `id`, `user_id`, `token`, `expires_at`, `created_at`, `used` | índice em `token`; `token` `UNIQUE` |

Nenhuma das duas tem `@ManyToOne`/FK para uma tabela de usuário — `user_id` é só um `UUID` simples
(ver DER em [docs/planning/der/auth-service.mmd](../planning/der/auth-service.mmd), verificado
sincronizado com `V1__create_auth_schema.sql` e as entidades `RefreshToken`/`PasswordResetToken`
nesta revisão).

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://localhost:5432/auth_db
DB_USERNAME=auth_user
DB_PASSWORD=<senha>
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
USER_SERVICE_URL=http://localhost:8003
JWT_SECRET=<chave-base64-256bit>
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000
PASSWORD_RESET_TOKEN_TTL_MINUTES=15
```

Todas com default local em `application.yml` (`${VARIAVEL:valor-default}`).

## Testes

### Unitários (`src/test/java/.../unit/`)

Cinco classes de teste (27 métodos): `unit/service/AuthServiceTest` (9), `unit/service/
TokenServiceTest` (10), `unit/client/UserServiceGatewayTest` (4), `unit/service/JwtServiceTest`
(3) e `unit/service/TokenBlacklistServiceTest` (1). Cobrem os casos de erro: credencial inválida,
conta inativa, refresh token expirado/revogado/inexistente, token de reset já usado, JWT
expirado/malformado/sem assinatura válida, e circuito aberto/retry esgotado na chamada ao User
Service (fallback → `UserServiceUnavailableException`).

### Aceitação (Cucumber — `src/test/resources/features/`)

Cenários cobertos na última execução: login com sucesso, login com credencial inválida, logout
revoga sessão, refresh com rotation, reset de senha completo, e circuito aberto no User Service.
Infraestrutura
(PostgreSQL, Redis, Kafka) sobe via Testcontainers; a chamada síncrona ao User Service é simulada
com WireMock (`WireMockSupport`), nunca com o User Service real.

> **Nota de rastreabilidade — critério "Acesso com token na blacklist" (spec, linha 55):** este
> serviço não valida JWT em rota própria (não tem Spring Security, ver `config/` real na seção
> acima) — quem de fato rejeita um `access_token` cujo `jti` está em `token:blacklist:{jti}` é o
> **API Gateway**, antes de rotear a requisição para qualquer serviço downstream. Por isso o
> cenário de aceitação que comprova esse critério (`Acesso com token na blacklist` → 401) vive em
> `backend/api-gateway/src/test/resources/features/api-gateway.feature`, não em
> `auth-service.feature` — o Auth Service só é responsável por gravar a chave no Redis no logout
> (`TokenBlacklistService.blacklist`, ver seção "Blacklist de token no Redis" acima). Gap
> identificado e corrigido nesta revisão: até então o Gateway extraía o JWT mas descartava o claim
> `jti`, nunca consultando a blacklist antes de autenticar — corrigido em
> `backend/api-gateway/.../service/JwtService.java`,
> `backend/api-gateway/.../filter/JwtReactiveAuthenticationManager.java` e no novo
> `backend/api-gateway/.../service/TokenBlacklistService.java`.

### Como rodar

```bash
cd backend/auth-service
mvn checkstyle:check   # 0 violações (infra/checkstyle/checkstyle.xml)
mvn test               # unitários + aceitação Cucumber (JUnit Platform Suite)
mvn verify              # idem + gate de cobertura Jacoco (mínimo 70% de linha)
mvn jacoco:report       # relatório HTML em target/site/jacoco/index.html
mvn clean package       # build final (target/auth-service.jar)
snyk test --all-sub-projects --detection-depth=6
```

Pré-requisito para os testes de aceitação: Docker Desktop em execução (Testcontainers sobe
PostgreSQL, Redis e Kafka efêmeros). Nenhum comando de teste depende de `docker compose up` — a
infra do `docker-compose.yml` do projeto não precisa estar de pé.

### Evidências da última execução

- `mvn test`: 33 testes (27 unitários em 5 classes + 6 cenários Cucumber), 0 falhas.
- `mvn verify` (Jacoco `check`, regra `LINE` `COVEREDRATIO` ≥ 0.70): `All coverage checks have been
  met.` — cobertura de linha real ≈ 89% (260/292 linhas, `target/site/jacoco/index.html`).
- `mvn clean package`: `BUILD SUCCESS`, gera `target/auth-service.jar`.
- `snyk test --all-sub-projects --detection-depth=6`: `Tested 183 dependencies for known issues, no
  vulnerable paths found.` (`ok: true`), após pinar via `dependencyManagement`:
  `com.fasterxml.jackson.core:jackson-databind:2.21.6`,
  `com.fasterxml.jackson.core:jackson-annotations:2.22`,
  `tools.jackson.core:jackson-databind:3.2.2`, `tools.jackson.core:jackson-core:3.2.2` e
  `com.github.luben:zstd-jni:1.5.7-14` (CVEs de desserialização/reflection no Jackson trazido pelo
  `jjwt-jackson` e pelo Jackson 3 nativo do Spring Boot 4, e de out-of-bounds read/use-after-free no
  `zstd-jni` transitivo do `kafka-clients`).
- `mvn checkstyle:check`: `0 Checkstyle violations.`

## Dependências relevantes (pom.xml)

Spring Boot `4.0.8` (parent) + Spring Cloud `2025.1.3`, Spring Data JPA + PostgreSQL, Spring Data
Redis, Spring Cloud OpenFeign, Spring Cloud Circuit Breaker Resilience4j, Spring Kafka, Flyway,
JJWT `0.13.0` (`jjwt-api`/`jjwt-impl`/`jjwt-jackson`), Springdoc OpenAPI `3.1.0`. Testes: JUnit 5,
Mockito, AssertJ, Cucumber `7.33.0`, JUnit Platform Suite, Testcontainers `2.0.3` (PostgreSQL +
Kafka), WireMock Standalone `3.13.0`.

`version` do artefato: `1.0.0` (convenção obrigatória do CLAUDE.md para todo serviço do MVP).

### Nota sobre Jackson (Spring Boot 4)

Mesma convivência de duas linhas de Jackson documentada em `docs/apps/api-gateway.md`: o Spring
Boot 4 serializa com o novo Jackson 3 (`tools.jackson.*`), enquanto o `jjwt-jackson` ainda depende
do Jackson 2 clássico (`com.fasterxml.jackson.*`). Ao pinar `tools.jackson.core:jackson-databind`
para uma versão mais nova sem também alinhar `tools.jackson.core:jackson-core` e
`com.fasterxml.jackson.core:jackson-annotations` (esta última compartilhada pelas duas linhas,
mesmo groupId, versionada na numeração 2.x mesmo sob o guarda-chuva do Jackson 3 — decisão da
própria FasterXML), o classpath quebra em runtime
(`ClassNotFoundException: com.fasterxml.jackson.annotation.JsonApplyView`) por divergência de
versão entre `jackson-databind` e `jackson-annotations` resolvida por "nearest wins" do Maven. A
correção definitiva foi alinhar as três coordenadas junto no `dependencyManagement`.

## Limitações conhecidas

- `AuthControllerDocs` documenta o contrato OpenAPI separado do `AuthController` — mudança de
  assinatura em um dos dois exige atualizar o outro manualmente (sem checagem de compilação entre
  eles).
- O consumer do evento `user.password-reset` (Notification Service) ainda não existe no MVP — o
  evento é publicado, mas hoje não há verificação automatizada de consumo fim a fim; isso será
  coberto quando o Notification Service for implementado (fase 7 do roadmap).
