# Auth Service

**Build Tool:** Maven | **Arquitetura:** MVC | **Porta:** 8002 | **Status:** Concluído

**DER:** [docs/planning/der/auth-service.mmd](../der/auth-service.mmd)

## Objetivo

Autenticação: login, logout, refresh token com rotation, blacklist de tokens revogados no Redis e
reset de senha via e-mail. Extraído do User Service — decisão reverte a fusão original (ver
[docs/planning/action-plan.md § Decisões de Consolidação](../action-plan.md#decisões-de-consolidação)).
Autenticação (sessão/token, alta sensibilidade de segurança) e Perfil (CRUD de dados cadastrais)
são bounded contexts distintos: ciclo de vida, superfície de ataque e cadência de mudança
diferentes justificam serviço próprio, mesmo dependendo do User Service para os dados de
credencial.

Não possui tabela `users` — não é dono do dado de credencial (`password_hash`). Toda leitura/
escrita de credencial passa por chamada OpenFeign ao [User Service](03-user-service.md), que
mantém o dado (Database per Service — ADR-002).

## PRD Resumido

Sem um serviço próprio de autenticação, sessão/token ficaria acoplada ao CRUD de perfil no User
Service, misturando um dado de alta sensibilidade (credencial) com um dado de baixa sensibilidade
(cadastro), com cadência de mudança e superfície de ataque diferentes. O Auth Service resolve isso
isolando login, logout, refresh e reset de senha em um bounded context próprio. Usado por todo
cliente final autenticado (via frontend, através do Gateway) e depende do User Service via
OpenFeign para validar/atualizar credencial. Valor de negócio: reduz a área exposta a ataques de
credencial, permite evoluir a estratégia de token (rotation, blacklist) sem tocar no domínio de
perfil, e sustenta o fluxo de recuperação de senha via e-mail.

## Use Cases

- Como cliente final, quero fazer login com e-mail e senha, para acessar áreas autenticadas da loja
  sem expor o token diretamente no meu navegador (cookie httpOnly).
- Como cliente final autenticado, quero fazer logout, para revogar meu refresh token e invalidar
  meu access token imediatamente, mesmo antes de expirar.
- Como cliente final com sessão ativa, quero que meu token seja renovado via refresh automático,
  para continuar navegando sem precisar logar novamente a cada hora.
- Como cliente que esqueceu a senha, quero solicitar redefinição por e-mail, para recuperar acesso
  à minha conta sem precisar de suporte manual.
- Como cliente que recebeu o link de redefinição, quero confirmar a nova senha com o token
  temporário, para voltar a acessar minha conta com segurança.
- Como Auth Service, quero validar credenciais e atualizar senha chamando o User Service via
  OpenFeign, para nunca persistir ou expor o `password_hash` fora do dono do dado.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Login com sucesso | Usuário cadastrado no User Service com credencial válida | Cliente chama `POST /api/v1/auth/login` com e-mail/senha corretos | Serviço retorna 200 com `Set-Cookie` `access_token` e `refresh_token` httpOnly, sem token no corpo da resposta |
| Login com credencial inválida | Usuário existe, mas senha está incorreta | Cliente chama `POST /api/v1/auth/login` | Serviço retorna 401 sem emitir nenhum token |
| Refresh com rotation | Cliente possui `refresh_token` válido e não revogado | Cliente chama `POST /api/v1/auth/refresh` | Serviço invalida o refresh token antigo e emite novo par de tokens (access + refresh) |
| Logout revoga sessão | Cliente autenticado com `access_token` e `refresh_token` válidos | Cliente chama `POST /api/v1/auth/logout` | Refresh token é marcado como revogado, access token entra na blacklist Redis, e cookies retornam com `maxAge=0` |
| Acesso com token na blacklist | Access token foi colocado na blacklist Redis após logout | Cliente tenta acessar endpoint protegido reusando o token antigo | Serviço retorna 401 |
| Reset de senha completo | Cliente solicitou `forgot-password` e recebeu token temporário válido (TTL 15 min) | Cliente chama `POST /api/v1/auth/reset-password` com o token e nova senha dentro do prazo | User Service persiste o novo hash da senha e o token de reset é marcado como usado |
| Circuit breaker aberto no User Service | User Service indisponível (falhas consecutivas acima do limiar) | Cliente chama `login`, `forgot-password` ou `reset-password` | Auth Service retorna 503 sem emitir token, nunca autenticando sem confirmação do User Service |

## Banco de Dados: PostgreSQL (`auth_db`) + Redis

## Responsabilidades

- Login com e-mail/senha → valida via User Service, emite access token (JWT) + refresh token
- Logout → revoga refresh token + adiciona access token na blacklist Redis
- Refresh → valida refresh token, emite novo par de tokens (rotation)
- Forgot password → confirma usuário via User Service, gera token temporário (15 min), publica
  evento Kafka
- Reset password → valida token, chama User Service para persistir a nova senha

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Security | 6.x | Filtros de autenticação |
| JJWT | 0.12+ | Geração e validação de JWT |
| Spring Data JPA | 3.x | PostgreSQL (`auth_db`) |
| Flyway | 9+ | Migrações |
| Spring Data Redis | 3.x | Blacklist de tokens |
| Spring Kafka | 3.x | Producer `user.password-reset` |
| OpenFeign | Spring Cloud | Chamar User Service (verificação/atualização de credencial) |
| Resilience4j | 2.x | Circuit Breaker + Retry na chamada ao User Service |
| Bean Validation | Jakarta | Validação de entrada |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Maven (pom.xml)

```xml
<!-- <project> -->
<version>1.0.0</version>

<properties>
    <java.version>25</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
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
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
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
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Endpoints

```
POST   /api/v1/auth/login             # Login → seta cookies httpOnly
POST   /api/v1/auth/logout            # Logout → revoga tokens + limpa cookies
POST   /api/v1/auth/refresh           # Refresh token rotation → re-seta cookies
POST   /api/v1/auth/forgot-password   # Solicitar reset de senha
POST   /api/v1/auth/reset-password    # Confirmar reset com token temporário
```

## Integração com User Service (OpenFeign)

```
POST /internal/v1/users/verify-credentials   # { email, password } → { id, email, role } ou 401/403
GET  /internal/v1/users/{id}                 # Busca por id (fluxo refresh — re-hidrata claims do token)
GET  /internal/v1/users/by-email/{email}     # Existência + id (fluxo forgot-password)
PUT  /internal/v1/users/{id}/password        # { newPassword } (fluxo reset-password)
```

> Implementação real (`UserServiceClient`) devolve `role` (String, singular) e não `roles` (lista)
> como a versão anterior desta spec descrevia — o Auth Service empacota esse único valor em
> `List.of(user.role())` só na hora de montar o claim `roles` do JWT. `verify-credentials` também
> distingue duas falhas do User Service: `401` (credencial inválida) e `403` (conta inativa/
> bloqueada) — o Auth Service traduz isso em duas exceções de domínio distintas
> (`InvalidCredentialsException` → 401, `InactiveAccountException` → 403), não documentadas como
> critério de aceite separado ainda (ver nota de implementação abaixo).

Fallback (circuit breaker aberto): `login`/`forgot-password`/`reset-password` devem retornar 503 —
nunca emitir token sem confirmação do User Service. **Implementado:** ver
[Nota de Implementação Atual](#nota-de-implementação-atual-circuit-breaker) — a chamada ao
`UserServiceClient` passa por `UserServiceGateway`, que aplica `@CircuitBreaker`/`@Retry` e converte
indisponibilidade real em `UserServiceUnavailableException` (503).

## Estratégia de Token — httpOnly Cookie

Frontend nunca lê/armazena o JWT diretamente (proteção contra XSS). Fluxo:

- `login`/`refresh` respondem **sem token no body** — o Gateway repassa o `Set-Cookie` do Auth
  Service pro cliente: `access_token` (httpOnly, Secure, SameSite=Lax, maxAge=1h) e `refresh_token`
  (httpOnly, Secure, SameSite=Lax, path=`/api/v1/auth/refresh`, maxAge=7d).
- Requests subsequentes do front usam `withCredentials:true` — cookie vai automático, sem header
  `Authorization` manual.
- `logout` responde `Set-Cookie` com `maxAge=0` pra ambos os cookies.
- Contrato completo (CORS, client HTTP do front) em
  [docs/integration/frontend-backend-integration.md](../../integration/frontend-backend-integration.md).

## Schema do Banco (Flyway)

### V1__create_auth_schema.sql

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    revoked BOOLEAN DEFAULT FALSE
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
```

> `user_id` é só referência (UUID simples) ao dado dono no User Service — sem `FOREIGN KEY`
> cross-database. Regra do CLAUDE.md ("nunca `@ManyToOne`/relação automática") já se aplica aqui
> por natureza, já que as duas tabelas vivem em bancos diferentes.

## Eventos Kafka Publicados

**Tópico:** `user.password-reset`

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "resetToken": "abc123",
  "expiresAt": "2024-01-01T12:15:00Z"
}
```

## Estrutura de Pacotes (MVC)

> Estrutura real implementada — difere da versão anterior desta spec em vários pontos (ver
> [Nota de Implementação Atual](#nota-de-implementação-atual-circuit-breaker) para o detalhamento do
> circuit breaker, já concluído).

```
com.autohubstore.authservice/
├── controller/
│   ├── AuthController.java                      # @RestController — /api/v1/auth
│   ├── AuthCookieFactory.java                    # Monta/expira os ResponseCookie httpOnly
│   └── docs/
│       └── AuthControllerDocs.java               # Interface Springdoc (@Operation/@ApiResponses)
├── service/
│   ├── AuthService.java                         # Login, logout, refresh, forgot-password, reset-password
│   ├── TokenService.java                        # Refresh token (create/rotate/revoke) + password reset token
│   ├── JwtService.java                          # Emissão/validação/claims do access token JWT
│   └── TokenBlacklistService.java               # Blacklist de access token no Redis
├── repository/
│   ├── RefreshTokenRepository.java              # JpaRepository
│   └── PasswordResetTokenRepository.java        # JpaRepository
├── domain/
│   ├── entity/
│   │   ├── RefreshToken.java                    # @Entity JPA
│   │   └── PasswordResetToken.java              # @Entity JPA
│   ├── dto/
│   │   ├── TokenClaims.java                     # record — claims extraídas do JWT
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── ForgotPasswordRequest.java
│   │   │   ├── ResetPasswordRequest.java
│   │   │   └── ValidateCredentialsRequest.java  # Request ao User Service (OpenFeign)
│   │   └── response/
│   │       ├── LoginResponse.java
│   │       └── UserVerificationResponse.java    # Response do User Service (OpenFeign)
│   └── event/
│       └── PasswordResetRequestedEvent.java     # Payload publicado no Kafka
├── exception/
│   ├── InvalidCredentialsException.java
│   ├── InactiveAccountException.java
│   ├── InvalidTokenException.java
│   ├── UserServiceUnavailableException.java     # Lançada pelo UserServiceGateway (fallback 503)
│   └── handler/
│       └── GlobalExceptionHandler.java          # @RestControllerAdvice — trata também 503
├── client/
│   ├── UserServiceClient.java                   # @FeignClient(name = "user-service")
│   └── UserServiceGateway.java                  # Envolve o Feign com @CircuitBreaker/@Retry (Resilience4j)
├── messaging/
│   └── PasswordResetEventPublisher.java         # KafkaTemplate producer user.password-reset
└── config/
    ├── KafkaProducerConfig.java
    └── OpenApiConfig.java
```

**Divergências relevantes em relação à versão anterior desta spec:**

- **Não existe `service/PasswordResetService.java` separado.** Forgot/reset password vivem em
  `AuthService.java`, junto com login/logout/refresh. `TokenService.java` também não é só "JWT
  rotation" — ele administra tanto `RefreshToken` (create/rotate/revoke) quanto
  `PasswordResetToken` (create/consume). Geração/validação do JWT em si (claims, TTL) ficou isolada
  em `JwtService.java`, e a blacklist do access token virou `TokenBlacklistService.java` — nenhum
  dos dois estava listado na versão anterior desta spec.
- **Pacotes `model/` → `domain/entity/` e `domain/dto/{request,response}`.** Entities JPA e DTOs de
  request/response não ficam achatados em `model/` como documentado antes; seguem a convenção
  `domain/entity`, `domain/dto/request`, `domain/dto/response`, `domain/event` (mais alinhada ao
  padrão Clean Architecture usado no User Service, embora este serviço continue MVC).
- **`external/` → `client/`.** `UserServiceClient` (Feign) vive em `client/`, não `external/`.
- **`exception/GlobalExceptionHandler.java` → `exception/handler/GlobalExceptionHandler.java`**, e o
  pacote `exception/` ganhou as exceções de domínio (`InvalidCredentialsException`,
  `InactiveAccountException`, `InvalidTokenException`) que a versão anterior da spec não detalhava.
- **`config/SecurityConfig.java` e `config/JwtConfig.java` não existem.** O serviço não depende de
  `spring-boot-starter-security` (ausente do `pom.xml`) — todos os endpoints `/api/v1/auth/*` são
  públicos por padrão do Spring MVC, sem filtro de segurança próprio (coerente com o texto do
  Swagger em `OpenApiConfig.java`, mas divergente da tabela de Tecnologias desta spec, que lista
  Spring Security 6.x como dependência — ver nota de implementação). `config/` real só tem
  `KafkaProducerConfig.java` e `OpenApiConfig.java`.
- **Divergência resolvida — Circuit Breaker no `client/`.** A versão anterior desta spec listava só
  `client/UserServiceClient.java` (Feign puro, chamado direto pelo `AuthService`). A implementação
  real adicionou `client/UserServiceGateway.java`, que envolve os 4 métodos do
  `UserServiceClient` (`verifyCredentials`, `findUserById`, `findUserByEmail`, `updatePassword`) com
  `@CircuitBreaker`/`@Retry` (Resilience4j) e a exceção nova `exception/UserServiceUnavailableException.java`
  — ver detalhamento em [Nota de Implementação Atual](#nota-de-implementação-atual-circuit-breaker).
  `AuthService.java` hoje chama `UserServiceGateway`, não mais `UserServiceClient` diretamente.

## Lógica JWT

- **Access Token TTL:** 1 hora (configurável via env)
- **Refresh Token TTL:** 7 dias
- **Claims customizados:** `userId`, `email`, `roles`
- **Blacklist Redis:** chave `token:blacklist:{jti}` com TTL igual ao tempo residual do token

## Configuração Circuit Breaker (application.yml)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      userService:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
```

### Nota de Implementação Atual (Circuit Breaker)

**Implementação concluída — confirmado em revisão de código:**

- `pom.xml` do `auth-service` tem a dependência `spring-cloud-starter-circuitbreaker-resilience4j`.
- `application.yml` tem o bloco `resilience4j.circuitbreaker.instances.userService` e
  `resilience4j.retry.instances.userService` (sliding window 10, failure rate 50%, wait duration
  10s, retry com 3 tentativas), com `ignoreExceptions` configurado para que erros de negócio do
  Feign (401/403/404) não contem como falha para abertura do circuito.
- `AuthService.java` não chama mais `UserServiceClient` diretamente — passou a chamar
  `client/UserServiceGateway.java`, que envolve os 4 métodos do Feign
  (`verifyCredentials`, `findUserById`, `findUserByEmail`, `updatePassword`) com
  `@CircuitBreaker`/`@Retry`. O fallback relança inalterada qualquer `FeignException` de negócio
  (401/403/404), e converte indisponibilidade real (timeout, connection refused, circuito aberto)
  em `exception/UserServiceUnavailableException.java`, nova.
- `exception/handler/GlobalExceptionHandler.java` trata `UserServiceUnavailableException`
  retornando `503 Service Unavailable`, cumprindo o critério de aceite "Circuit breaker aberto no
  User Service" da tabela acima.
- Cobertura de teste: `unit/client/UserServiceGatewayTest.java` (4 testes) no nível unitário, e o
  cenário Cucumber "Circuit breaker aberto no User Service" em
  `src/test/resources/features/auth-service.feature` no nível de aceitação — ambos passando na
  última execução.

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-auth:5432/auth_db
DB_USERNAME=auth_user
DB_PASSWORD=<secret>
REDIS_HOST=redis
REDIS_PORT=6379
USER_SERVICE_URL=http://user-service:8003
JWT_SECRET=<base64-encoded-256bit-key>
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
PASSWORD_RESET_TOKEN_TTL_MINUTES=15
```

**Resposta JSON em `snake_case`:** adicionar em `application.yml` (campo Java continua
`lowerCamelCase`, só a serialização de saída HTTP vira `snake_case` — ver
[CLAUDE.md § Convenções de Código](../../../CLAUDE.md#convenções-de-código)):

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/auth-service.jar app.jar
EXPOSE 8002
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

- **Unitários:** `TokenService` (TTL, rotation, claims); fluxo de forgot/reset password (parte de
  `AuthService`, já que não existe `PasswordResetService` separado — ver
  [Divergências relevantes](#estrutura-de-pacotes-mvc)) com mocks de `UserServiceGateway` (não mais
  `UserServiceClient` direto, desde a introdução do circuit breaker); `UserServiceGateway` também
  tem suíte própria (`UserServiceGatewayTest`) cobrindo fallback de negócio vs. indisponibilidade
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka) + WireMock para simular User Service,
  cobrindo fluxo login → refresh → logout e forgot-password → reset-password completos
- **Circuit Breaker:** Testar abertura após N falhas consecutivas do User Service → 503
- **Segurança:** Acesso sem token → 401; token expirado → 401; token na blacklist → 401
- **Validação:** Campos obrigatórios ausentes → 400; credenciais inválidas → 401

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/auth-service.md`.
