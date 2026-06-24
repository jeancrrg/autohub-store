# Auth Service

**Build Tool:** Maven | **Arquitetura:** Clean Architecture | **Porta:** 8081

## Objetivo

Gerenciar autenticação: emissão e validação de JWT, refresh token com rotation, blacklist de tokens revogados no Redis e fluxo de reset de senha via e-mail.

## Banco de Dados: PostgreSQL (`autohubstore_auth`) + Redis

## Responsabilidades

- Login com e-mail/senha → emite access token (JWT) + refresh token
- Logout → revoga refresh token + adiciona access token na blacklist Redis
- Refresh → valida refresh token, emite novo par de tokens (rotation)
- Forgot password → gera token temporário (15 min), publica evento Kafka
- Reset password → valida token, atualiza senha via User Service

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Security | 6.x | Autenticação |
| JJWT | 0.12+ | Geração e validação de JWT |
| Spring Data JPA | 3.x | PostgreSQL |
| Flyway | 9+ | Migrações |
| Spring Data Redis | 3.x | Blacklist de tokens |
| Spring Kafka | 3.x | Producer `auth.password-reset` |
| OpenFeign | Spring Cloud | Chamar User Service (validar credenciais) |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Maven (pom.xml)

```xml
<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
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
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
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
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Endpoints

```
POST /api/v1/auth/login              # Login → retorna access_token + refresh_token
POST /api/v1/auth/logout             # Logout → revoga tokens (requer Authorization)
POST /api/v1/auth/refresh            # Refresh token rotation
POST /api/v1/auth/forgot-password    # Solicitar reset de senha
POST /api/v1/auth/reset-password     # Confirmar reset com token temporário
```

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
```

## Evento Kafka Publicado

**Tópico:** `auth.password-reset`

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "resetToken": "abc123",
  "expiresAt": "2024-01-01T12:15:00Z"
}
```

## Estrutura de Pacotes (Clean Architecture)

```
com.autohubstore.authservice/
├── domain/
│   ├── model/
│   │   ├── RefreshToken.java                     # Entidade de domínio
│   │   └── PasswordResetToken.java               # Entidade de domínio
│   ├── event/
│   │   └── PasswordResetRequestedEvent.java      # Domain Event
│   ├── repository/
│   │   ├── RefreshTokenRepository.java           # Interface (output boundary)
│   │   └── PasswordResetTokenRepository.java     # Interface (output boundary)
│   └── service/
│       └── TokenDomainService.java               # Regras de domínio (TTL, rotation)
├── application/
│   ├── usecase/
│   │   ├── LoginUseCase.java                     # Input boundary
│   │   ├── LogoutUseCase.java
│   │   ├── RefreshTokenUseCase.java
│   │   ├── ForgotPasswordUseCase.java
│   │   └── ResetPasswordUseCase.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── LoginResponse.java
│       └── RefreshRequest.java
└── infrastructure/
    ├── web/
    │   └── AuthController.java                   # @RestController
    ├── persistence/
    │   ├── RefreshTokenJpaEntity.java             # @Entity JPA
    │   ├── RefreshTokenJpaRepository.java         # Implementa RefreshTokenRepository
    │   ├── PasswordResetTokenJpaEntity.java
    │   └── PasswordResetTokenJpaRepository.java
    ├── messaging/
    │   └── PasswordResetEventPublisher.java       # KafkaTemplate producer
    ├── external/
    │   └── UserServiceClient.java                 # @FeignClient para User Service
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

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-auth:5432/autohubstore_auth
DB_USERNAME=auth_user
DB_PASSWORD=<secret>
REDIS_HOST=redis
REDIS_PORT=6379
JWT_SECRET=<base64-encoded-256bit-key>
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
USER_SERVICE_URL=http://user-service:8082
PASSWORD_RESET_TOKEN_TTL_MINUTES=15
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Estratégia de Testes

- **Unitários:** `TokenDomainService` (TTL, rotation), cada UseCase com mocks das interfaces de domínio
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka) para fluxos login/logout/refresh completos
- **Segurança:** Acesso sem token → 401; token expirado → 401; token na blacklist → 401
