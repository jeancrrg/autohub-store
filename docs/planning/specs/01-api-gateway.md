# API Gateway

**Build Tool:** Maven | **Arquitetura:** Hexagonal (Ports & Adapters) | **Porta:** 8001

## Objetivo

Ponto de entrada único do AutoHubStore. Roteia requisições para os microsserviços, valida JWT centralizadamente, aplica rate limiting por IP/usuário e configura CORS para o frontend.

## Responsabilidades

- Roteamento de requisições para todos os 9 microsserviços
- Validação centralizada de JWT (sem delegar aos serviços downstream)
- Rate Limiting por IP e por usuário autenticado (Redis)
- Configuração de CORS para o frontend Next.js
- Headers de segurança (X-Content-Type-Options, X-Frame-Options)
- Load balancing via Spring Cloud LoadBalancer

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework base |
| Spring Cloud Gateway | 2023.x | Gateway reativo (WebFlux) |
| Spring Security | 6.x | Validação JWT |
| JJWT | 0.12+ | Parse e validação de JWT |
| Spring Data Redis | 3.x | Rate limiting |
| Micrometer + Prometheus | 1.x | Métricas |
| OpenTelemetry Agent | 1.x | Traces distribuídos |
| Springdoc OpenAPI | 2.x | Documentação |

## Dependências Maven (pom.xml)

```xml
<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
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
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
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

## Estrutura de Pacotes (Hexagonal — Ports & Adapters)

```
com.autohubstore.gateway/
├── GatewayApplication.java
├── domain/
│   ├── model/
│   │   └── JwtClaims.java                    # Value Object — claims do token JWT
│   ├── port/
│   │   ├── in/
│   │   │   ├── ValidateTokenUseCase.java      # Driving port: validate(token) → JwtClaims
│   │   │   └── CheckRateLimitUseCase.java     # Driving port: isAllowed(key, auth) → Mono<Boolean>
│   │   └── out/
│   │       └── RateLimitPort.java             # Driven port: increment + setExpiry
│   └── service/
│       ├── JwtValidationService.java          # Domain service — sem @Service
│       └── RateLimitDomainService.java        # Domain service — sem @Service
└── adapter/
    ├── config/
    │   └── DomainConfig.java                  # Instancia beans do domínio com @Configuration
    ├── in/
    │   └── web/
    │       ├── SecurityConfig.java            # @EnableWebFluxSecurity + AuthenticationWebFilter
    │       ├── GatewayRoutesConfig.java       # RouteLocator (Java DSL)
    │       ├── CorsConfig.java                # CorsWebFilter
    │       ├── RateLimitFilter.java           # GlobalFilter — chama CheckRateLimitUseCase
    │       └── FallbackController.java        # @RestController — respostas 503
    └── out/
        ├── redis/
        │   └── RateLimitRedisAdapter.java     # Implementa RateLimitPort via Redis reativo
        └── web/
            └── GatewayExceptionHandler.java   # ErrorWebExceptionHandler — erros em JSON
```

> **Princípio Hexagonal:** O domínio (`domain/`) não tem nenhuma dependência do Spring. As classes `JwtValidationService` e `RateLimitDomainService` são POJOs puros instanciados via `DomainConfig`. Os adapters (`adapter/`) contêm toda a cola com o framework.

## Configuração de Rotas (application.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates: [Path=/api/v1/auth/**]
        - id: user-service
          uri: lb://user-service
          predicates: [Path=/api/v1/users/**]
        - id: catalog-service
          uri: lb://catalog-service
          predicates: [Path=/api/v1/catalog/**]
        - id: search-service
          uri: lb://search-service
          predicates: [Path=/api/v1/search/**]
        - id: cart-service
          uri: lb://cart-service
          predicates: [Path=/api/v1/cart/**]
        - id: order-service
          uri: lb://order-service
          predicates: [Path=/api/v1/orders/**]
        - id: payment-service
          uri: lb://payment-service
          predicates: [Path=/api/v1/payments/**]
        - id: analytics-service
          uri: lb://analytics-service
          predicates: [Path=/api/v1/analytics/**]
```

## Endpoints Públicos (sem JWT)

```
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/users                    # Cadastro
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
GET  /api/v1/catalog/**               # Listagem e detalhes de produtos
GET  /api/v1/search/**                # Busca
```

## Endpoints Protegidos

Todos os demais endpoints exigem header `Authorization: Bearer <token>`.

## Rate Limiting

- Por IP: 100 req/min para endpoints públicos
- Por usuário: 200 req/min para endpoints autenticados
- Chave Redis: `ratelimit:{ip}:{endpoint}` e `ratelimit:{userId}:{endpoint}`
- TTL: 60 segundos

## Variáveis de Ambiente

```
JWT_SECRET=<chave-base64-256bit>
JWT_EXPIRATION_MS=3600000
REDIS_HOST=redis
REDIS_PORT=6379
ALLOWED_ORIGINS=http://localhost:3000,https://autohubstore.com
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/api-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Estratégia de Testes

- **Unitários:** JwtValidationService (token válido, expirado, inválido, ausente), RateLimitService
- **Integração:** WebTestClient testando roteamento e respostas de erro (401, 429)
- **Segurança:** Endpoint protegido sem token → 401; rate limit excedido → 429
