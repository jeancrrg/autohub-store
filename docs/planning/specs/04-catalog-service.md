# Catalog Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8004

## Objetivo

Gerenciar produtos e categorias com cache Redis para reduzir latência e publicação de eventos Kafka para sincronizar o Search Service e alimentar o Analytics Service.

## Banco de Dados: PostgreSQL (`autohubstore_catalog`) + Redis (cache)

## Responsabilidades

- CRUD completo de produtos (admin)
- CRUD de categorias (hierarquia pai/filho)
- Listagem paginada de produtos com filtro por categoria
- Cache de produtos no Redis (TTL 5 minutos)
- Controle básico de estoque (quantidade)
- Publicar: `catalog.product-created`, `catalog.product-updated`, `catalog.product-viewed`

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data JPA | 3.x | PostgreSQL |
| Spring Data Redis | 3.x | Cache com @Cacheable |
| Spring Cache | Spring Boot | Abstração de cache |
| Flyway | 9+ | Migrações |
| Spring Kafka | 3.x | Producer de eventos |
| Bean Validation | Jakarta | Validação de entrada |
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
version = '0.0.1-SNAPSHOT'

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
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.flywaydb:flyway-core'
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

## Endpoints

```
# Público
GET    /api/v1/catalog/products                       # Lista paginada
GET    /api/v1/catalog/products/{id}                  # Detalhes (publica product-viewed)
GET    /api/v1/catalog/categories                     # Lista categorias
GET    /api/v1/catalog/categories/{id}/products       # Produtos por categoria

# Admin (requer role ADMIN via JWT)
POST   /api/v1/catalog/products                       # Criar produto
PUT    /api/v1/catalog/products/{id}                  # Atualizar produto
DELETE /api/v1/catalog/products/{id}                  # Remover produto
POST   /api/v1/catalog/categories                     # Criar categoria
```

## Schema do Banco (Flyway)

### V1__create_catalog_schema.sql

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category_id UUID NOT NULL REFERENCES categories(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url VARCHAR(1024) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
```

## Eventos Kafka Publicados

**Tópico `catalog.product-created`** e **`catalog.product-updated`:**

```json
{
  "productId": "uuid",
  "name": "Filtro de Ar K&N",
  "description": "Filtro de alto desempenho...",
  "price": 299.90,
  "categoryId": "uuid",
  "categoryName": "Filtros",
  "status": "ACTIVE",
  "stockQuantity": 50
}
```

**Tópico `catalog.product-viewed`:**

```json
{
  "productId": "uuid",
  "productName": "Filtro de Ar K&N",
  "viewedAt": "2024-01-01T10:00:00Z"
}
```

## Cache Redis

| Chave | TTL | Conteúdo |
|---|---|---|
| `product:{id}` | 5 min | JSON do produto |
| `products:category:{id}:page:{n}` | 2 min | Lista paginada |

Usar `@CacheEvict` ao atualizar ou deletar produto.

## Estrutura de Pacotes (MVC)

```
com.autohubstore.catalogservice/
├── controller/
│   ├── ProductController.java          # CRUD produtos + listagem pública
│   └── CategoryController.java         # CRUD categorias
├── service/
│   ├── ProductService.java             # Lógica de negócio de produto + cache
│   └── CategoryService.java            # Lógica de negócio de categoria
├── repository/
│   ├── ProductRepository.java          # JpaRepository<Product, UUID>
│   └── CategoryRepository.java         # JpaRepository<Category, UUID>
├── model/
│   ├── Product.java                    # @Entity
│   ├── Category.java                   # @Entity
│   ├── ProductImage.java               # @Entity
│   ├── ProductStatus.java              # Enum: ACTIVE, INACTIVE, OUT_OF_STOCK
│   ├── CreateProductRequest.java       # DTO entrada
│   ├── UpdateProductRequest.java       # DTO entrada
│   └── ProductResponse.java            # DTO saída
├── messaging/
│   └── CatalogEventPublisher.java      # KafkaTemplate producer
├── exception/
│   └── GlobalExceptionHandler.java     # @ControllerAdvice
└── config/
    ├── RedisConfig.java                # CacheManager Redis
    └── KafkaProducerConfig.java
```

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-catalog:5434/autohubstore_catalog
DB_USERNAME=catalog_svc
DB_PASSWORD=<secret>
REDIS_HOST=redis
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY build/libs/catalog-service.jar app.jar
EXPOSE 8004
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Checkstyle

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
    sourceSets = [sourceSets.main] // não aplica nos testes
}
```

## Estratégia de Testes

- **Unitários:** `ProductService` (CRUD, lógica de cache hit/miss), `CategoryService`
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka); criar produto → verificar cache e evento publicado
- **Cache:** Testar que segunda leitura do mesmo produto vem do Redis (sem hit no banco)
