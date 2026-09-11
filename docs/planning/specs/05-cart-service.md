# Cart Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8005 | **Status:** Planejado

**DER:** [docs/planning/der/cart-service.mmd](../der/cart-service.mmd) (sem banco relacional —
diagrama documenta a estrutura do Hash Redis)

## Objetivo

Carrinho de compras por usuário persistido no Redis com snapshot de preço, TTL de 7 dias e circuit breaker para resiliência quando o Catalog Service estiver indisponível.

## PRD Resumido

Sem um carrinho persistido, o cliente perderia os itens selecionados ao trocar de dispositivo ou
sessão, e sem snapshot de preço o valor do item mudaria retroativamente se o admin alterasse o
preço no catálogo. O Cart Service resolve isso mantendo o carrinho por usuário no Redis com preço
travado no momento da adição. Usado diretamente pelo cliente final autenticado e consumido pelo
Order Service (via chamada que limpa o carrinho após criar o pedido). Depende do Catalog Service
via OpenFeign para validar produto e capturar o preço no momento da adição. Valor de negócio:
experiência de compra consistente entre sessões, proteção contra oscilação de preço durante a
navegação, e resiliência via circuit breaker quando o Catalog está fora do ar.

## Use Cases

- Como cliente autenticado, quero visualizar meu carrinho com itens, subtotais e total, para
  revisar minha compra antes de finalizar o pedido.
- Como cliente autenticado, quero adicionar um item ao carrinho informando produto e quantidade,
  para separar o que pretendo comprar.
- Como cliente autenticado, quero atualizar a quantidade de um item já no carrinho, para ajustar
  minha compra sem removê-lo e adicioná-lo novamente.
- Como cliente autenticado, quero remover um item do carrinho, para desistir da compra daquele
  produto.
- Como Order Service, quero limpar o carrinho de um usuário após criar o pedido, para que os itens
  já comprados não continuem aparecendo como pendentes.
- Como cliente autenticado, quero que meu carrinho continue disponível por até 7 dias de
  inatividade, para não perder a seleção entre visitas.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Adicionar item com sucesso | Produto existe e está disponível no Catalog Service | Cliente chama `POST /api/v1/cart/items` com `productId` e `quantity` válidos | Serviço grava o item no Hash Redis `cart:{userId}` com snapshot do preço atual, renova o TTL para 7 dias e retorna o carrinho atualizado |
| Atualizar quantidade | Item já existe no carrinho do usuário | Cliente chama `PUT /api/v1/cart/items/{productId}` com nova `quantity` | Serviço atualiza a quantidade mantendo o preço do snapshot original (não busca novo preço no Catalog) e recalcula o subtotal |
| Remover item | Item existe no carrinho | Cliente chama `DELETE /api/v1/cart/items/{productId}` | Serviço remove o campo do Hash Redis e o item deixa de aparecer em `GET /api/v1/cart` |
| Produto inexistente | `productId` informado não existe no Catalog Service | Cliente chama `POST /api/v1/cart/items` com esse `productId` | Serviço retorna erro (4xx) sem adicionar item ao carrinho |
| TTL renovado | Carrinho já existe com TTL residual menor que 7 dias | Cliente realiza qualquer operação de escrita no carrinho | TTL da chave `cart:{userId}` é renovado para 7 dias completos |
| Circuit breaker aberto | Catalog Service indisponível (falhas acima do limiar configurado) | Cliente chama `POST /api/v1/cart/items` | Serviço retorna 503 sem adicionar item, nunca gravando item sem validação do Catalog |
| Limpeza pelo Order Service | Carrinho do usuário com itens | Order Service chama `DELETE /api/v1/cart` após criar o pedido | Hash Redis `cart:{userId}` é removido, e `GET /api/v1/cart` volta a retornar carrinho vazio |

## Banco de Dados: Redis

## Responsabilidades

- Adicionar, remover e atualizar quantidade de itens
- Calcular totais em tempo real
- Snapshot de preço no momento da adição (imutável — não atualiza retroativamente)
- TTL de 7 dias renovado a cada operação
- Consulta ao Catalog Service via OpenFeign para validar produto e obter preço
- Circuit Breaker se o Catalog estiver fora → retorna 503

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data Redis | 3.x | Hash por usuário |
| OpenFeign | Spring Cloud | Chamar Catalog Service |
| Resilience4j | 2.x | Circuit Breaker + Retry |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Gradle (build.gradle)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.autohubstore'
version = '1.0.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

ext {
    set('springCloudVersion', "2023.0.3")
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:testcontainers'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

## Endpoints

Todos requerem JWT (usuário autenticado).

```
GET    /api/v1/cart                          # Visualizar carrinho
POST   /api/v1/cart/items                    # Adicionar item { productId, quantity }
PUT    /api/v1/cart/items/{productId}         # Atualizar quantidade { quantity }
DELETE /api/v1/cart/items/{productId}         # Remover item
DELETE /api/v1/cart                          # Limpar carrinho (usado pelo Order Service)
```

## Estrutura Redis

```
Chave: cart:{userId}   →   Redis Hash
  Fields:
    item:{productId}:name      → snapshot do nome do produto
    item:{productId}:price     → snapshot do preço (BigDecimal serializado como string)
    item:{productId}:quantity  → quantidade (int como string)
TTL: 7 dias, renovado a cada operação de escrita
```

**Resposta GET /cart:**

```json
{
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "productName": "Filtro de Ar K&N",
      "unitPrice": 299.90,
      "quantity": 2,
      "subtotal": 599.80
    }
  ],
  "totalAmount": 599.80,
  "itemCount": 2
}
```

## Configuração Circuit Breaker (application.yml)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      catalogService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      catalogService:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
```

**Fallback:** retorna HTTP 503 com mensagem clara. Nunca adiciona item sem validar produto no Catalog.

## Estrutura de Pacotes (MVC)

```
com.autohubstore.cartservice/
├── controller/
│   └── CartController.java              # CRUD do carrinho
├── service/
│   └── CartService.java                 # Snapshot de preço, cálculo de totais, TTL
├── repository/
│   └── CartRedisRepository.java         # Operações Redis Hash
├── model/
│   ├── CartItem.java                    # Item do carrinho (snapshot)
│   ├── CartResponse.java                # DTO resposta
│   └── AddItemRequest.java              # DTO entrada
├── exception/
│   └── GlobalExceptionHandler.java      # @ControllerAdvice
├── external/
│   └── CatalogServiceClient.java        # @FeignClient(name = "catalog-service")
└── config/
    └── RedisConfig.java                 # RedisTemplate + TTL config
```

## Variáveis de Ambiente

```
REDIS_HOST=redis
REDIS_PORT=6379
CATALOG_SERVICE_URL=http://catalog-service:8004
CART_TTL_DAYS=7
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
COPY build/libs/cart-service.jar app.jar
EXPOSE 8005
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Checkstyle

> **Código deve nascer conforme:** escrever classes já seguindo `infra/checkstyle/checkstyle.xml`
> (linha em branco após `{` de abertura e antes do `}` de fechamento da classe, sem números mágicos,
> sem exceções/catches genéricos, campos `private`, etc. — resumo em
> [CLAUDE.md § Checkstyle](../../../CLAUDE.md#checkstyle--obrigatório-em-todo-código-gerado)).
> Não gerar código e corrigir depois.

Apontar para o arquivo compartilhado em `infra/checkstyle/checkstyle.xml`. Adicionar no `build.gradle`:

```groovy
plugins {
    // adicionar ao bloco de plugins existente
    id 'checkstyle'
}

checkstyle {
    toolVersion = '10.21.0'
    configFile = rootProject.file('infra/checkstyle/checkstyle.xml')
    ignoreFailures = false
    showViolations = true
    sourceSets = [sourceSets.main, sourceSets.test] // valida também src/test/
}
```

## Estratégia de Testes

- **Unitários:** `CartService` (snapshot de preço imutável, cálculo de totais, TTL renovado)
- **Integração:** Testcontainers Redis; WireMock para simular Catalog Service
- **Circuit Breaker:** Testar abertura após N falhas consecutivas do Catalog → retorna 503
- **TTL:** Verificar que chave Redis tem TTL renovado a cada adição de item

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/cart-service.md`.
