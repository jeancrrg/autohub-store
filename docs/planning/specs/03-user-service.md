# User Service

**Build Tool:** Maven | **Arquitetura:** Clean Architecture | **Porta:** 8003 | **Status:** Em implementação

**DER:** [docs/planning/der/user-service.mmd](../der/user-service.mmd)

## Objetivo

Gerenciar cadastro, perfil e endereços de usuários. Não lida com autenticação (login, tokens,
sessão) — isso é responsabilidade do [Auth Service](02-auth-service.md), extraído deste serviço
para bounded context próprio. Ver decisão em
[docs/planning/action-plan.md](../action-plan.md#decisões-de-consolidação).

Expõe endpoints internos (não roteados pelo Gateway) consumidos pelo Auth Service via OpenFeign
para validar credenciais e atualizar senha — o `password_hash` nunca sai deste serviço.

## PRD Resumido

Sem um dono único do dado cadastral, cadastro/perfil/endereço ficariam espalhados ou duplicados
entre serviços que precisam desses dados (Auth, Order). O User Service resolve isso concentrando
CRUD de perfil e endereço em um bounded context próprio, mantendo o `password_hash` isolado de
qualquer serviço externo. Usado diretamente pelo cliente final (cadastro, edição de perfil,
endereços de entrega) e, via endpoints internos, pelo Auth Service (OpenFeign) para autenticação.
Valor de negócio: garante consistência do cadastro (e-mail único), sustenta a base de identidade
usada por Order Service (endereço de entrega) e Notification Service (Kafka `user.created`).

## Use Cases

- Como visitante, quero me cadastrar informando e-mail, nome e senha, para criar minha conta no
  AutoHubStore.
- Como cliente autenticado, quero consultar e atualizar meus dados de perfil, para manter meu
  cadastro correto.
- Como cliente autenticado, quero cadastrar, listar e remover endereços de entrega, para escolher
  onde recebo meus pedidos.
- Como Order Service, quero consultar o endereço de um usuário, para montar o pedido com o destino
  de entrega correto (fora do escopo direto desta spec — ver [07-order-service.md](07-order-service.md)).
- Como Auth Service, quero verificar credenciais de um usuário (e-mail + senha) via endpoint
  interno, para autenticar sem ter acesso direto ao `user_db`.
- Como Auth Service, quero buscar um usuário por e-mail via endpoint interno, para confirmar
  existência de conta no fluxo de recuperação de senha.
- Como Auth Service, quero atualizar a senha de um usuário via endpoint interno, para concluir o
  fluxo de reset de senha sem que o `password_hash` trafegue por outro serviço.
- Como Notification Service, quero ser avisado da criação de um novo usuário via evento Kafka
  `user.created`, para disparar o e-mail de boas-vindas.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Cadastro com sucesso | E-mail ainda não cadastrado | Cliente chama `POST /api/v1/users` com dados válidos | Serviço cria usuário com senha hasheada em BCrypt, retorna 201 com o perfil criado e publica evento `user.created` no Kafka |
| Cadastro com e-mail duplicado | Já existe usuário com o mesmo e-mail | Cliente chama `POST /api/v1/users` reusando o e-mail | Serviço retorna 409, sem criar novo registro nem publicar evento |
| Validação de campos obrigatórios | Corpo da requisição sem `email`, `fullName` ou `password` | Cliente chama `POST /api/v1/users` | Serviço retorna 400 com mensagem explícita do campo ausente |
| Atualização de perfil | Usuário existente autenticado | Cliente chama `PUT /api/v1/users/{id}` com novos dados válidos | Serviço atualiza o registro e retorna 200 com o perfil atualizado |
| CRUD de endereço | Usuário autenticado sem endereços cadastrados | Cliente chama `POST /api/v1/users/{id}/addresses` com endereço válido | Serviço cria o endereço vinculado ao `userId` e retorna 201; endereço passa a aparecer em `GET /api/v1/users/{id}/addresses` |
| Verificação de credenciais correta | Usuário existente com senha conhecida | Auth Service chama `POST /internal/v1/users/verify-credentials` com e-mail e senha corretos | Serviço retorna 200 com `userId` e `roles`, sem expor `password_hash` |
| Verificação de credenciais incorreta | Usuário existente, senha informada não confere com o hash | Auth Service chama `POST /internal/v1/users/verify-credentials` | Serviço retorna 401 |
| Atualização de senha via fluxo interno | Token de reset já validado pelo Auth Service | Auth Service chama `PUT /internal/v1/users/{id}/password` com nova senha | Serviço persiste novo hash BCrypt, sobrescrevendo o anterior |

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

- **Unitários:** `PasswordDomainService` (hash/verificação BCrypt); use cases de perfil (e-mail único)
  com mocks das interfaces de domínio
- **Integração:** Testcontainers (PostgreSQL + Kafka) para fluxo cadastro → consulta de perfil
- **Endpoints internos:** `verify-credentials` com senha correta/incorreta; `update-password` persiste
  novo hash
- **Validação:** Campos obrigatórios ausentes → 400; e-mail duplicado → 409

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/user-service.md`.
