# Auth Service

**Build Tool:** Maven | **Arquitetura:** MVC | **Porta:** 8002 | **Status:** Em implementação

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
POST /internal/v1/users/verify-credentials   # { email, password } → { userId, roles } ou 401
GET  /internal/v1/users/by-email/{email}     # Existência + userId (fluxo forgot-password)
PUT  /internal/v1/users/{id}/password        # { newPassword } (fluxo reset-password)
```

Fallback (circuit breaker aberto): `login`/`forgot-password`/`reset-password` retornam 503 —
nunca emite token sem confirmação do User Service.

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

```
com.autohubstore.authservice/
├── controller/
│   └── AuthController.java                     # @RestController — /api/v1/auth
├── service/
│   ├── AuthService.java                        # Login, logout, refresh
│   ├── PasswordResetService.java                # Forgot/reset password
│   └── TokenService.java                        # Emissão/validação JWT, rotation
├── repository/
│   ├── RefreshTokenRepository.java              # JpaRepository
│   └── PasswordResetTokenRepository.java        # JpaRepository
├── model/
│   ├── RefreshToken.java                        # @Entity JPA
│   ├── PasswordResetToken.java                  # @Entity JPA
│   ├── LoginRequest.java                        # DTO
│   ├── LoginResponse.java                       # DTO
│   ├── RefreshRequest.java                      # DTO
│   ├── ForgotPasswordRequest.java               # DTO
│   └── ResetPasswordRequest.java                # DTO
├── exception/
│   └── GlobalExceptionHandler.java              # @ControllerAdvice
├── external/
│   └── UserServiceClient.java                   # @FeignClient(name = "user-service")
├── messaging/
│   └── PasswordResetEventPublisher.java         # KafkaTemplate producer user.password-reset
└── config/
    ├── SecurityConfig.java
    ├── JwtConfig.java
    └── KafkaProducerConfig.java
```

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
        <includeTestSourceDirectory>false</includeTestSourceDirectory>
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

- **Unitários:** `TokenService` (TTL, rotation, claims); `PasswordResetService` (TTL do token de
  reset) com mocks do `UserServiceClient`
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka) + WireMock para simular User Service,
  cobrindo fluxo login → refresh → logout e forgot-password → reset-password completos
- **Circuit Breaker:** Testar abertura após N falhas consecutivas do User Service → 503
- **Segurança:** Acesso sem token → 401; token expirado → 401; token na blacklist → 401
- **Validação:** Campos obrigatórios ausentes → 400; credenciais inválidas → 401
