# User Service

**Build Tool:** Maven | **Arquitetura:** Clean Architecture | **Porta:** 8003 | **Status:** Em implementação

## Objetivo

Gerenciar cadastro, perfil e endereços de usuários. Não lida com autenticação (login, tokens,
sessão) — isso é responsabilidade do [Auth Service](02-auth-service.md), extraído deste serviço
para bounded context próprio. Ver decisão em
[docs/planning/action-plan.md](../action-plan.md#decisões-de-consolidação).

Expõe endpoints internos (não roteados pelo Gateway) consumidos pelo Auth Service via OpenFeign
para validar credenciais e atualizar senha — o `password_hash` nunca sai deste serviço.

## Banco de Dados: PostgreSQL (`user_db`)

## Responsabilidades

- Cadastro de novos usuários (hash BCrypt da senha)
- Consulta e atualização de perfil
- CRUD de endereços de entrega
- Publicação de evento `user.created` no Kafka
- Endpoints internos para o Auth Service: verificar credenciais, buscar usuário por e-mail,
  atualizar senha

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Security Crypto | 6.x | BCrypt (hash de senha) |
| Spring Data JPA | 3.x | PostgreSQL |
| Flyway | 9+ | Migrações |
| Spring Kafka | 3.x | Producer `user.created` |
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
# Perfil (público via Gateway)
POST   /api/v1/users                              # Cadastro
GET    /api/v1/users/{id}                         # Perfil
PUT    /api/v1/users/{id}                         # Atualizar perfil
GET    /api/v1/users/{id}/addresses               # Listar endereços
POST   /api/v1/users/{id}/addresses               # Criar endereço
DELETE /api/v1/users/{id}/addresses/{addressId}   # Remover endereço

# Internos (não expostos pelo Gateway — chamados via OpenFeign pelo Auth Service)
POST   /internal/v1/users/verify-credentials      # { email, password } → { userId, roles } ou 401
GET    /internal/v1/users/by-email/{email}        # Existência + userId (fluxo forgot-password)
PUT    /internal/v1/users/{id}/password           # { newPassword } → hash + persiste (fluxo reset-password)
```

> **Por que endpoints internos e não acesso direto ao banco:** Database per Service (ADR-002) — o
> Auth Service não tem acesso a `user_db`. Toda validação/alteração de credencial passa por esses
> endpoints, e o `password_hash` nunca atravessa a rede — `verify-credentials` recebe a senha em
> texto puro (canal interno, rede Docker) e responde só o resultado da comparação BCrypt.

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

## Estrutura de Pacotes (Clean Architecture)

```
com.autohubstore.userservice/
├── domain/
│   ├── model/
│   │   ├── User.java                              # Entidade de domínio
│   │   └── Address.java                           # Entidade de domínio
│   ├── event/
│   │   └── UserCreatedEvent.java                   # Domain Event
│   ├── repository/
│   │   ├── UserRepository.java                     # Interface (output boundary)
│   │   └── AddressRepository.java                  # Interface (output boundary)
│   └── service/
│       └── PasswordDomainService.java               # Hash/verificação BCrypt
├── application/
│   ├── usecase/
│   │   ├── CreateUserUseCase.java                  # Input boundary
│   │   ├── UpdateUserUseCase.java
│   │   ├── ManageAddressUseCase.java
│   │   ├── VerifyCredentialsUseCase.java            # Usado pelo endpoint interno
│   │   └── UpdatePasswordUseCase.java               # Usado pelo endpoint interno
│   ├── dto/
│   │   ├── CreateUserRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── UserResponse.java
│   │   ├── AddressRequest.java
│   │   ├── AddressResponse.java
│   │   ├── VerifyCredentialsRequest.java
│   │   ├── VerifyCredentialsResponse.java
│   │   └── UpdatePasswordRequest.java
│   └── mapper/
│       └── UserMapper.java                         # MapStruct
└── infrastructure/
    ├── web/
    │   ├── UserController.java                     # @RestController — /api/v1/users
    │   ├── AddressController.java                  # @RestController — /api/v1/users/{id}/addresses
    │   └── UserInternalController.java              # @RestController — /internal/v1/users
    ├── persistence/
    │   ├── UserJpaEntity.java                      # @Entity JPA
    │   ├── UserJpaRepository.java                  # Implementa UserRepository
    │   ├── AddressJpaEntity.java
    │   └── AddressJpaRepository.java                # Implementa AddressRepository
    ├── messaging/
    │   └── UserEventPublisher.java                  # KafkaTemplate producer user.created
    └── config/
        ├── SecurityConfig.java                      # Libera /internal/v1/** só na rede interna
        └── KafkaProducerConfig.java
```

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-user:5433/user_db
DB_USERNAME=user_user
DB_PASSWORD=<secret>
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/user-service.jar app.jar
EXPOSE 8003
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

- **Unitários:** `PasswordDomainService` (hash/verificação BCrypt); use cases de perfil (e-mail único)
  com mocks das interfaces de domínio
- **Integração:** Testcontainers (PostgreSQL + Kafka) para fluxo cadastro → consulta de perfil
- **Endpoints internos:** `verify-credentials` com senha correta/incorreta; `update-password` persiste
  novo hash
- **Validação:** Campos obrigatórios ausentes → 400; e-mail duplicado → 409
