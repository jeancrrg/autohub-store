# User Service

**Build Tool:** Maven | **Arquitetura:** Clean Architecture | **Porta:** 8002 | **Status:** Em implementação

## Objetivo

Serviço único de Identidade: gerencia cadastro, perfil e endereços de usuários **e** autenticação
(login, logout, refresh token com rotation, blacklist de tokens revogados no Redis, reset de
senha via e-mail). Resultado da fusão dos antigos `auth-service` + `user-service` — ambos
pertenciam ao mesmo bounded context ("Identidade") e o Auth já dependia do User via OpenFeign só
para validar credenciais; separá-los era overhead artificial sem ganho de domínio. Ver decisão em
[docs/planning/action-plan.md](../action-plan.md#decisões-de-consolidação).

## Banco de Dados: PostgreSQL (`user_db`) + Redis

## Responsabilidades

### Identidade (ex-Auth)
- Login com e-mail/senha → emite access token (JWT) + refresh token
- Logout → revoga refresh token + adiciona access token na blacklist Redis
- Refresh → valida refresh token, emite novo par de tokens (rotation)
- Forgot password → gera token temporário (15 min), publica evento Kafka
- Reset password → valida token, atualiza senha do usuário

### Perfil (ex-User)
- Cadastro de novos usuários (hash BCrypt da senha)
- Consulta e atualização de perfil
- CRUD de endereços de entrega
- Publicação de evento `user.created` no Kafka

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
| Spring Kafka | 3.x | Producer `user.created`, `user.password-reset` |
| BCrypt | Spring Security Crypto | Hash de senhas |
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
```

## Endpoints

```
# Identidade (público)
POST   /api/v1/auth/login                        # Login → seta cookies httpOnly
POST   /api/v1/auth/logout                        # Logout → revoga tokens + limpa cookies
POST   /api/v1/auth/refresh                       # Refresh token rotation → re-seta cookies
POST   /api/v1/auth/forgot-password                # Solicitar reset de senha
POST   /api/v1/auth/reset-password                 # Confirmar reset com token temporário

# Perfil
POST   /api/v1/users                              # Cadastro (público)
GET    /api/v1/users/{id}                         # Perfil
PUT    /api/v1/users/{id}                         # Atualizar perfil
GET    /api/v1/users/{id}/addresses               # Listar endereços
POST   /api/v1/users/{id}/addresses               # Criar endereço
DELETE /api/v1/users/{id}/addresses/{addressId}   # Remover endereço
```

## Estratégia de Token — httpOnly Cookie

Frontend nunca lê/armazena o JWT diretamente (proteção contra XSS). Fluxo:

- `login`/`refresh` respondem **sem token no body** — o Gateway repassa o `Set-Cookie` do User
  Service pro cliente: `access_token` (httpOnly, Secure, SameSite=Lax, maxAge=1h) e `refresh_token`
  (httpOnly, Secure, SameSite=Lax, path=`/api/v1/auth/refresh`, maxAge=7d).
- Requests subsequentes do front usam `withCredentials:true` — cookie vai automático, sem header
  `Authorization` manual.
- `logout` responde `Set-Cookie` com `maxAge=0` pra ambos os cookies.
- Contrato completo (CORS, client HTTP do front) em
  [docs/integration/frontend-backend-integration.md](../../integration/frontend-backend-integration.md).

## Schema do Banco (Flyway)

### V1__create_user_schema.sql

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(9) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    revoked BOOLEAN DEFAULT FALSE
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
```

## Eventos Kafka Publicados

**Tópico:** `user.created`

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "fullName": "João Silva",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

**Tópico:** `user.password-reset`

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
com.autohubstore.userservice/
├── domain/
│   ├── model/
│   │   ├── User.java                              # Entidade de domínio
│   │   ├── Address.java                           # Entidade de domínio
│   │   ├── RefreshToken.java                      # Entidade de domínio
│   │   └── PasswordResetToken.java                 # Entidade de domínio
│   ├── event/
│   │   ├── UserCreatedEvent.java                   # Domain Event
│   │   └── PasswordResetRequestedEvent.java        # Domain Event
│   ├── repository/
│   │   ├── UserRepository.java                     # Interface (output boundary)
│   │   ├── AddressRepository.java                  # Interface (output boundary)
│   │   ├── RefreshTokenRepository.java             # Interface (output boundary)
│   │   └── PasswordResetTokenRepository.java       # Interface (output boundary)
│   └── service/
│       └── TokenDomainService.java                 # Regras de domínio (TTL, rotation)
├── application/
│   ├── usecase/
│   │   ├── LoginUseCase.java                       # Input boundary
│   │   ├── LogoutUseCase.java
│   │   ├── RefreshTokenUseCase.java
│   │   ├── ForgotPasswordUseCase.java
│   │   ├── ResetPasswordUseCase.java
│   │   ├── CreateUserUseCase.java
│   │   ├── UpdateUserUseCase.java
│   │   └── ManageAddressUseCase.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RefreshRequest.java
│   │   ├── CreateUserRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── UserResponse.java
│   │   ├── AddressRequest.java
│   │   └── AddressResponse.java
│   └── mapper/
│       └── UserMapper.java                         # MapStruct
└── infrastructure/
    ├── web/
    │   ├── AuthController.java                     # @RestController — /api/v1/auth
    │   ├── UserController.java                     # @RestController — /api/v1/users
    │   └── AddressController.java                  # @RestController — /api/v1/users/{id}/addresses
    ├── persistence/
    │   ├── UserJpaEntity.java                      # @Entity JPA
    │   ├── UserJpaRepository.java                  # Implementa UserRepository
    │   ├── AddressJpaEntity.java
    │   ├── AddressJpaRepository.java                # Implementa AddressRepository
    │   ├── RefreshTokenJpaEntity.java
    │   ├── RefreshTokenJpaRepository.java           # Implementa RefreshTokenRepository
    │   ├── PasswordResetTokenJpaEntity.java
    │   └── PasswordResetTokenJpaRepository.java     # Implementa PasswordResetTokenRepository
    ├── messaging/
    │   ├── UserEventPublisher.java                  # KafkaTemplate producer user.created
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

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-user:5433/user_db
DB_USERNAME=user_user
DB_PASSWORD=<secret>
REDIS_HOST=redis
REDIS_PORT=6379
JWT_SECRET=<base64-encoded-256bit-key>
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
PASSWORD_RESET_TOKEN_TTL_MINUTES=15
```

> **Infra:** container `postgres-auth` removido de `infra/docker-compose.yml` — as tabelas
> `refresh_tokens` e `password_reset_tokens` passam a viver em `user_db` (container
> `postgres-user`, porta 5433).

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/user-service.jar app.jar
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

- **Unitários:** `TokenDomainService` (TTL, rotation); use cases de perfil (e-mail único, hash de
  senha) com mocks das interfaces de domínio
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka) para fluxos cadastro → login → refresh →
  logout completos
- **Segurança:** Acesso sem token → 401; token expirado → 401; token na blacklist → 401
- **Validação:** Campos obrigatórios ausentes → 400; e-mail duplicado → 409
