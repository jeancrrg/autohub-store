# User Service

**Build Tool:** Maven | **Arquitetura:** MVC | **Porta:** 8003 | **Status:** Em implementação

## Objetivo

Gerenciar cadastro, perfil e endereços de usuários. Publica `user.created` no Kafka após cadastro bem-sucedido. Expõe endpoint interno para o Auth Service validar credenciais.

## Banco de Dados: PostgreSQL (`user_db`)

## Responsabilidades

- Cadastro de novos usuários (hash BCrypt da senha)
- Consulta e atualização de perfil
- CRUD de endereços de entrega
- Publicação de evento `user.created` no Kafka
- Endpoint interno `/internal/users/credentials` para autenticação via Auth Service

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data JPA | 3.x | PostgreSQL |
| Flyway | 9+ | Migrações |
| Spring Kafka | 3.x | Producer `user.created` |
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
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
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
POST   /api/v1/users                              # Cadastro (público)
GET    /api/v1/users/{id}                         # Perfil
PUT    /api/v1/users/{id}                         # Atualizar perfil
GET    /api/v1/users/{id}/addresses               # Listar endereços
POST   /api/v1/users/{id}/addresses               # Criar endereço
DELETE /api/v1/users/{id}/addresses/{addressId}   # Remover endereço
GET    /internal/users/credentials?email={}       # Uso interno (Auth Service)
```

## Schema do Banco (Flyway)

### V1__create_users_schema.sql

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

## Evento Kafka Publicado

**Tópico:** `user.created`

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "fullName": "João Silva",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

## Estrutura de Pacotes (MVC)

```
com.autohubstore.userservice/
├── controller/
│   ├── UserController.java           # POST /users, GET/PUT /users/{id}
│   ├── AddressController.java        # CRUD /users/{id}/addresses
│   └── InternalUserController.java   # GET /internal/users/credentials
├── service/
│   ├── UserService.java              # Regras de negócio de usuário
│   └── AddressService.java           # Regras de negócio de endereço
├── repository/
│   ├── UserRepository.java           # JpaRepository<User, UUID>
│   └── AddressRepository.java        # JpaRepository<Address, UUID>
├── model/
│   ├── User.java                     # @Entity
│   ├── Address.java                  # @Entity
│   ├── UserStatus.java               # Enum: ACTIVE, INACTIVE, BLOCKED
│   ├── CreateUserRequest.java        # DTO entrada
│   ├── UpdateUserRequest.java        # DTO entrada
│   ├── UserResponse.java             # DTO saída
│   ├── AddressRequest.java           # DTO entrada
│   └── AddressResponse.java          # DTO saída
├── messaging/
│   └── UserEventPublisher.java       # KafkaTemplate producer
├── exception/
│   └── GlobalExceptionHandler.java   # @ControllerAdvice
└── config/
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

- **Unitários:** `UserService` (cadastro, e-mail único, hash de senha), `AddressService`
- **Integração:** Testcontainers PostgreSQL + Kafka; fluxo cadastro → evento `user.created` publicado
- **Validação:** Campos obrigatórios ausentes → 400; e-mail duplicado → 409
