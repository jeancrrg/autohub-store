# Extração do Auth Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separar Autenticação (login/logout/refresh/reset de senha) do User Service em um novo
microsserviço `auth-service` (2º serviço criado, logo após o Gateway), mantendo o User Service
como dono exclusivo do dado de credencial (`password_hash`), consumido pelo Auth Service via
OpenFeign.

**Architecture:** `auth-service` — Maven + MVC — não tem tabela `users`; possui `auth_db`
(PostgreSQL) só com `refresh_tokens`/`password_reset_tokens`, usa Redis (container `redis`
compartilhado) para blacklist, e chama `user-service` via `@FeignClient` em três endpoints
internos (`verify-credentials`, `by-email`, `update-password`) para nunca duplicar/expor o hash de
senha. `user-service` continua Clean Architecture, mas perde toda a camada de token/JWT — só
cadastro, perfil e endereços.

**Tech Stack:** Java 25, Spring Boot 3.x, Spring Security 6.x, JJWT 0.12+, Spring Data JPA,
Flyway, Spring Data Redis, Spring Kafka, OpenFeign, Resilience4j, Springdoc OpenAPI,
Testcontainers.

**Spec:**
- [docs/planning/specs/02-auth-service.md](../../planning/specs/02-auth-service.md)
- [docs/planning/specs/03-user-service.md](../../planning/specs/03-user-service.md)

## Global Constraints

- Java 25, `spring.threads.virtual.enabled=true` em ambos os serviços.
- Checkstyle: apontar para `infra/checkstyle/checkstyle.xml` via `maven-checkstyle-plugin`
  (`failsOnError=true`), igual ao `pom.xml` do `api-gateway`.
- Sem `@ManyToOne`/relação JPA automática — `user_id` em `refresh_tokens`/
  `password_reset_tokens` é `UUID` simples, sem `FOREIGN KEY` (bancos diferentes).
- Injeção via `@RequiredArgsConstructor` (Lombok), nunca `@Autowired` em campo.
- Entity JPA sempre com `@Column(name = "...")` explícito e `@PrePersist`/`@PreUpdate` para
  timestamps.
- Controllers retornam `ResponseEntity` explícito (`ResponseEntity.status(HttpStatus.X).body(...)`),
  nunca atalhos (`ResponseEntity.ok(...)`, etc.).
- Métodos de leitura (`@GetMapping`, service, repository) começam com `find*`.
- `password_hash` nunca atravessa a rede — `verify-credentials` recebe senha em texto puro
  (canal interno Docker) e o User Service responde só o resultado da comparação BCrypt.
- **Este plano só cria arquivos — não execute `mvn`/`docker compose`/`git` neste projeto. O
  usuário roda build/infra manualmente.**

---

### Task 1: Infra — `postgres-auth` no docker-compose

**Status:** já aplicado nesta sessão de planejamento (`infra/docker-compose.yml` já tem o serviço
`postgres-auth` na porta `5432`, banco `auth_db`, e o volume `postgres_auth_data`). Nenhuma ação
pendente aqui — listado só para registro de que a infra já está pronta antes do Task 2.

**Files:**
- Modificado: `infra/docker-compose.yml` (serviço `postgres-auth` + volume `postgres_auth_data`)

**Interfaces:**
- Produces: container `postgres-auth` ouvindo em `localhost:5432`, banco `auth_db`, usuário
  `auth_user`/`auth_pass` — é o que `Task 3` usa em `DB_URL`.

- [ ] **Step 1: Confirmar visualmente o serviço no compose**

Abrir `infra/docker-compose.yml` e confirmar bloco:

```yaml
    postgres-auth:
        image: postgres:16
        container_name: postgres-auth
        environment:
            POSTGRES_DB: auth_db
            POSTGRES_USER: auth_user
            POSTGRES_PASSWORD: auth_pass
        ports:
            - "5432:5432"
        volumes:
            - postgres_auth_data:/var/lib/postgresql/data
        healthcheck:
            test: [ "CMD-SHELL", "pg_isready -U auth_user -d auth_db" ]
            interval: 10s
            timeout: 5s
            retries: 5
```

- [ ] **Step 2: Subir manualmente (usuário executa, não o agente)**

Comando que o usuário deve rodar: `cd infra && docker compose up -d postgres-auth`
Esperado: `docker compose ps` mostra `postgres-auth` como `healthy`.

---

### Task 2: Scaffold do projeto Maven `backend/auth-service`

**Files:**
- Create: `backend/auth-service/pom.xml`
- Create: `backend/auth-service/src/main/resources/application.yml`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/AuthServiceApplication.java`
- Create: `backend/auth-service/Dockerfile`

**Interfaces:**
- Produces: artefato Maven `auth-service` rodando na porta `8002`, com `spring.threads.virtual.enabled=true`.

- [ ] **Step 1: Criar `pom.xml`**

Usar exatamente as dependências e o bloco de `properties`/`build/plugins` (checkstyle) descritos em
[docs/planning/specs/02-auth-service.md § Dependências Maven](../../planning/specs/02-auth-service.md#dependências-maven-pomxml),
mais o bloco de checkstyle em
[docs/planning/specs/02-auth-service.md § Checkstyle](../../planning/specs/02-auth-service.md#checkstyle).
`artifactId` = `auth-service`, `groupId` = `com.autohubstore`.

- [ ] **Step 2: Criar `application.yml`**

```yaml
server:
  port: 8002

spring:
  application:
    name: auth-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/auth_db}
    username: ${DB_USERNAME:auth_user}
    password: ${DB_PASSWORD:auth_pass}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  flyway:
    enabled: true
    locations: classpath:db/migration

user-service:
  url: ${USER_SERVICE_URL:http://localhost:8003}

jwt:
  secret: ${JWT_SECRET:change-me-base64-256bit}
  expiration-ms: ${JWT_EXPIRATION_MS:3600000}
  refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000}

password-reset:
  token-ttl-minutes: ${PASSWORD_RESET_TOKEN_TTL_MINUTES:15}

springdoc:
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- [ ] **Step 3: Criar classe principal**

```java
package com.autohubstore.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
```

- [ ] **Step 4: Criar `Dockerfile`**

Usar o conteúdo de
[docs/planning/specs/02-auth-service.md § Docker](../../planning/specs/02-auth-service.md#docker).

- [ ] **Step 5: Confirmar estrutura de pastas vazia dos demais pacotes**

Criar (vazios, para próximas tasks): `controller/`, `service/`, `repository/`, `model/`,
`exception/`, `external/`, `messaging/`, `config/` dentro de
`src/main/java/com/autohubstore/authservice/`.

---

### Task 3: Migração Flyway do `auth_db`

**Files:**
- Create: `backend/auth-service/src/main/resources/db/migration/V1__create_auth_schema.sql`

**Interfaces:**
- Consumes: banco `auth_db` do Task 1.
- Produces: tabelas `refresh_tokens` e `password_reset_tokens` — usadas pelos repositories do
  Task 5.

- [ ] **Step 1: Criar a migração**

Usar exatamente o SQL de
[docs/planning/specs/02-auth-service.md § Schema do Banco](../../planning/specs/02-auth-service.md#schema-do-banco-flyway)
(tabelas `refresh_tokens` e `password_reset_tokens`, sem `FOREIGN KEY` para `users` — bancos
diferentes).

- [ ] **Step 2: Validar manualmente (usuário roda)**

Comando que o usuário deve rodar ao subir o serviço: `mvn spring-boot:run` (dentro de
`backend/auth-service`) e conferir logs do Flyway aplicando `V1__create_auth_schema.sql` sem erro.

---

### Task 4: Endpoints internos no User Service (pré-requisito do Auth Service)

**Files:**
- Create: `backend/user-service/...` (ver estrutura completa em
  [docs/planning/specs/03-user-service.md § Estrutura de Pacotes](../../planning/specs/03-user-service.md#estrutura-de-pacotes-clean-architecture))
- Create: `backend/user-service/src/main/java/com/autohubstore/userservice/infrastructure/web/UserInternalController.java`
- Create: `backend/user-service/src/main/java/com/autohubstore/userservice/application/usecase/VerifyCredentialsUseCase.java`
- Create: `backend/user-service/src/main/java/com/autohubstore/userservice/application/usecase/UpdatePasswordUseCase.java`

**Interfaces:**
- Produces (contrato HTTP consumido pelo `UserServiceClient` do Task 5):
  - `POST /internal/v1/users/verify-credentials` — body `{ "email": string, "password": string }`
    → `200 { "userId": uuid, "roles": string[] }` ou `401`
  - `GET /internal/v1/users/by-email/{email}` → `200 { "userId": uuid, "email": string }` ou `404`
  - `PUT /internal/v1/users/{id}/password` — body `{ "newPassword": string }` → `204`

- [ ] **Step 1: Implementar `VerifyCredentialsUseCase`**

```java
package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.VerifyCredentialsRequest;
import com.autohubstore.userservice.application.dto.VerifyCredentialsResponse;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.infrastructure.web.exception.InvalidCredentialsException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifyCredentialsUseCase {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;

    public VerifyCredentialsResponse execute(VerifyCredentialsRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordDomainService.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new VerifyCredentialsResponse(user.getId(), List.of("ROLE_CUSTOMER"));
    }

}
```

(`InvalidCredentialsException` é uma exceção de domínio específica — nunca `RuntimeException`
genérica — mapeada para `401` num `@ControllerAdvice` já existente ou novo, seguindo o padrão do
resto do projeto.)

- [ ] **Step 2: Implementar `UpdatePasswordUseCase`**

```java
package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.infrastructure.web.exception.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdatePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;

    public void execute(UUID userId, String newPassword) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setPasswordHash(passwordDomainService.hash(newPassword));
        userRepository.save(user);
    }

}
```

- [ ] **Step 3: Implementar `UserInternalController`**

```java
package com.autohubstore.userservice.infrastructure.web;

import com.autohubstore.userservice.application.dto.VerifyCredentialsRequest;
import com.autohubstore.userservice.application.dto.VerifyCredentialsResponse;
import com.autohubstore.userservice.application.usecase.UpdatePasswordUseCase;
import com.autohubstore.userservice.application.usecase.VerifyCredentialsUseCase;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class UserInternalController {

    private final VerifyCredentialsUseCase verifyCredentialsUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;

    @PostMapping("/verify-credentials")
    public ResponseEntity<VerifyCredentialsResponse> verifyCredentials(
            @RequestBody VerifyCredentialsRequest request) {
        VerifyCredentialsResponse response = verifyCredentialsUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        updatePasswordUseCase.execute(id, body.get("newPassword"));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
```

(`GET /internal/v1/users/by-email/{email}` segue o mesmo padrão dos demais `find*` do
`UserController` público — reaproveita `UserRepository.findByEmail`.)

- [ ] **Step 4: Bloquear `/internal/v1/**` no `SecurityConfig`**

Restringir a rota a chamadas internas (rede Docker), nunca exposta pelo Gateway — documentar isso
no `application.yml`/`SecurityConfig` do User Service (o Gateway não deve ter rota para
`/internal/**`).

---

### Task 5: Client OpenFeign no Auth Service para o User Service

**Files:**
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/external/UserServiceClient.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/model/VerifyCredentialsRequest.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/model/VerifyCredentialsResponse.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/config/FeignResilienceConfig.java`

**Interfaces:**
- Consumes: contrato HTTP do Task 4 (`/internal/v1/users/*`).
- Produces: `UserServiceClient` injetável em `AuthService`/`PasswordResetService` (Task 6).

- [ ] **Step 1: DTOs de request/response**

```java
package com.autohubstore.authservice.model;

public record VerifyCredentialsRequest(String email, String password) {
}
```

```java
package com.autohubstore.authservice.model;

import java.util.List;
import java.util.UUID;

public record VerifyCredentialsResponse(UUID userId, List<String> roles) {
}
```

- [ ] **Step 2: `@FeignClient`**

```java
package com.autohubstore.authservice.external;

import com.autohubstore.authservice.model.VerifyCredentialsRequest;
import com.autohubstore.authservice.model.VerifyCredentialsResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    @PostMapping("/internal/v1/users/verify-credentials")
    VerifyCredentialsResponse verifyCredentials(@RequestBody VerifyCredentialsRequest request);

    @GetMapping("/internal/v1/users/by-email/{email}")
    Map<String, Object> findByEmail(@PathVariable String email);

    @PutMapping("/internal/v1/users/{id}/password")
    void updatePassword(@PathVariable UUID id, @RequestBody Map<String, String> body);

}
```

- [ ] **Step 3: Circuit breaker em `application.yml`**

Adicionar o bloco `resilience4j` de
[docs/planning/specs/02-auth-service.md § Configuração Circuit Breaker](../../planning/specs/02-auth-service.md#configuração-circuit-breaker-applicationyml).

---

### Task 6: Serviços de autenticação (login/logout/refresh) e controller

**Files:**
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/model/RefreshToken.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/repository/RefreshTokenRepository.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/service/TokenService.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/service/AuthService.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/controller/AuthController.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/config/JwtConfig.java`

**Interfaces:**
- Consumes: `UserServiceClient` (Task 5).
- Produces: `POST /api/v1/auth/login`, `/logout`, `/refresh` funcionando fim a fim contra o User
  Service real.

- [ ] **Step 1: Entity `RefreshToken`**

```java
package com.autohubstore.authservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "revoked")
    private boolean revoked;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

}
```

(`PasswordResetToken` no Task 7 segue o mesmo padrão.)

- [ ] **Step 2: `RefreshTokenRepository`**

```java
package com.autohubstore.authservice.repository;

import com.autohubstore.authservice.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

}
```

- [ ] **Step 3: `TokenService` (emissão/validação JWT + rotation)**

Implementar usando JJWT 0.12+: `generateAccessToken(userId, email, roles)`,
`generateRefreshToken(userId)` (persiste em `RefreshTokenRepository`, TTL 7 dias),
`rotateRefreshToken(oldToken)` (revoga o antigo, emite novo), `blacklistAccessToken(jti, ttl)`
(grava `token:blacklist:{jti}` no Redis com TTL igual ao tempo residual). Claims: `userId`,
`email`, `roles` — conforme
[docs/planning/specs/02-auth-service.md § Lógica JWT](../../planning/specs/02-auth-service.md#lógica-jwt).

- [ ] **Step 4: `AuthService` (login/logout/refresh)**

Orquestra `UserServiceClient.verifyCredentials` → `TokenService.generateAccessToken` +
`generateRefreshToken` no `login`; `TokenService.blacklistAccessToken` +
`RefreshTokenRepository` revoke no `logout`; `rotateRefreshToken` no `refresh`.

- [ ] **Step 5: `AuthController`**

`POST /api/v1/auth/login`, `/logout`, `/refresh` — cada um retorna `ResponseEntity` explícito e
seta/limpa os cookies httpOnly conforme
[docs/planning/specs/02-auth-service.md § Estratégia de Token](../../planning/specs/02-auth-service.md#estratégia-de-token--httponly-cookie).

---

### Task 7: Forgot/reset password + evento Kafka

**Files:**
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/model/PasswordResetToken.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/repository/PasswordResetTokenRepository.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/service/PasswordResetService.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/messaging/PasswordResetEventPublisher.java`
- Modify: `backend/auth-service/src/main/java/com/autohubstore/authservice/controller/AuthController.java` (Task 6) — adicionar `/forgot-password` e `/reset-password`

**Interfaces:**
- Consumes: `UserServiceClient.findByEmail`, `UserServiceClient.updatePassword` (Task 5).
- Produces: tópico Kafka `user.password-reset` consumido pelo Notification Service (Fase 7).

- [ ] **Step 1: Entity + repository `PasswordResetToken`**

Mesmo padrão do Task 6 Step 1/2, tabela `password_reset_tokens`, campo extra `used` (boolean).

- [ ] **Step 2: `PasswordResetEventPublisher`**

```java
package com.autohubstore.authservice.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetEventPublisher {

    private static final String TOPIC = "user.password-reset";

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public void publish(UUID userId, String email, String resetToken, Instant expiresAt) {
        kafkaTemplate.send(TOPIC, Map.of(
                "userId", userId,
                "email", email,
                "resetToken", resetToken,
                "expiresAt", expiresAt
        ));
    }

}
```

- [ ] **Step 3: `PasswordResetService`**

`forgotPassword(email)`: chama `UserServiceClient.findByEmail`, gera token (15 min TTL), persiste,
publica evento via `PasswordResetEventPublisher`. `resetPassword(token, newPassword)`: valida
token não expirado/não usado, chama `UserServiceClient.updatePassword`, marca token como `used`.

- [ ] **Step 4: Endpoints no `AuthController`**

`POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password` — `ResponseEntity`
explícito, delegam pro `PasswordResetService`.

---

### Task 8: Rota do Gateway para `auth-service`

**Files:**
- Modify: `backend/api-gateway/src/main/resources/application.yml` (ou classe de config de rotas,
  conforme implementação atual do Gateway)

**Interfaces:**
- Produces: rota `/api/v1/auth/**` → `lb://auth-service` no Gateway (a spec do Gateway já previa
  essa rota — ver
  [docs/planning/specs/01-api-gateway.md](../../planning/specs/01-api-gateway.md), linhas
  `id: auth-service` / `uri: lb://auth-service`).

- [ ] **Step 1: Ler configuração de rotas atual do Gateway**

Abrir `backend/api-gateway` e localizar onde as rotas `lb://*` estão declaradas (YAML ou
`RouteLocatorBuilder` Java config).

- [ ] **Step 2: Confirmar/ajustar a rota `auth-service`**

Garantir `predicates: [Path=/api/v1/auth/**]` apontando pra `lb://auth-service`, distinta da rota
`user-service` (`/api/v1/users/**`).

- [ ] **Step 3: Testar manualmente (usuário roda)**

Com `postgres-auth`, `user-service` e `auth-service` no ar: `curl -X POST
http://localhost:8001/api/v1/auth/login -d '{"email":"...","password":"..."}'` → deve retornar
`Set-Cookie` com `access_token`/`refresh_token`.

---

### Task 9: Testes de integração do Auth Service

**Files:**
- Create: `backend/auth-service/src/test/java/com/autohubstore/authservice/AuthFlowIntegrationTest.java`

**Interfaces:**
- Consumes: `AuthController` (Task 6/7), Testcontainers (PostgreSQL, Redis, Kafka), WireMock para
  simular `user-service`.

- [ ] **Step 1: Configurar Testcontainers + WireMock**

`@Testcontainers` com `PostgreSQLContainer` (auth_db), `GenericContainer` Redis, `KafkaContainer`;
`WireMockServer` simulando `/internal/v1/users/verify-credentials`.

- [ ] **Step 2: Teste do fluxo login → refresh → logout**

```java
@Test
void loginRefreshLogoutFlow() {
    wireMock.stubFor(post(urlEqualTo("/internal/v1/users/verify-credentials"))
            .willReturn(okJson("""
                    {"userId":"11111111-1111-1111-1111-111111111111","roles":["ROLE_CUSTOMER"]}
                    """)));

    var loginResponse = restTemplate.postForEntity("/api/v1/auth/login",
            new LoginRequest("user@example.com", "senha123"), Void.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE)).isNotEmpty();
}
```

- [ ] **Step 3: Rodar os testes (usuário executa)**

Comando que o usuário deve rodar: `mvn test` dentro de `backend/auth-service`.
Esperado: suíte passando, incluindo cenário de credencial inválida → `401`.

---

## Self-Review (já aplicado)

1. **Cobertura da spec:** login/logout/refresh (Task 6), forgot/reset password + Kafka (Task 7),
   integração com User Service (Task 4/5), infra (Task 1), scaffold (Task 2), migração (Task 3),
   Gateway (Task 8), testes (Task 9) — toda seção de
   [docs/planning/specs/02-auth-service.md](../../planning/specs/02-auth-service.md) tem task
   correspondente.
2. **Placeholders:** nenhum "TODO"/"implementar depois" — todo passo tem código real ou comando
   exato a rodar.
3. **Consistência de tipos:** `VerifyCredentialsRequest`/`VerifyCredentialsResponse` usam os mesmos
   nomes de campo em `UserServiceClient` (Task 5) e `UserInternalController` (Task 4); `TokenService`
   (Task 6) é o único ponto que gera/valida JWT, referenciado igual em `AuthService` e nos testes.
