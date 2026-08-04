# Catalog Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8003 | **Status:** Em implementação

## Objetivo

Gerenciar produtos e categorias com cache Redis para reduzir latência e publicação de eventos Kafka para sincronizar o Search Service e alimentar o Analytics Service.

> **Estoque não é mais responsabilidade deste serviço.** `stock_quantity` foi extraído para o
> [Inventory Service](06-inventory-service.md) — Catalog é orientado a leitura/cache e não tem
> semântica de reserva/concorrência necessária para controlar quantidade de forma segura sob
> checkout simultâneo. Ver decisão em
> [docs/planning/action-plan.md](../action-plan.md#decisões-de-consolidação).

## Banco de Dados: PostgreSQL (`catalog_db`) + Redis (cache) + MinIO (imagens)

## Responsabilidades

- CRUD completo de produtos (admin) — dados descritivos: nome, descrição, preço, categoria, imagens
- CRUD de categorias (hierarquia pai/filho)
- Gerar e validar identificadores de produto: `sku` (código de negócio) e `slug` (URL amigável) — ver [Identificadores de Produto](#identificadores-de-produto-sku-slug-e-id)
- Upload/remoção de imagens de produto (MinIO) — ver [Upload de Imagens](#upload-de-imagens-minio)
- Listagem paginada de produtos com filtro por categoria
- Cache de produtos no Redis (TTL 5 minutos)
- Consultar disponibilidade de estoque no Inventory Service (OpenFeign) para exibir badge "em estoque"
- Publicar: `catalog.product-created`, `catalog.product-updated`, `catalog.product-viewed`

> Contrato completo de integração com o frontend (paginação, erros, fluxo de upload) está
> detalhado em [docs/integration/frontend-backend-integration.md](../../integration/frontend-backend-integration.md).

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
    implementation 'io.minio:minio:8.5.11'
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
GET    /api/v1/catalog/products/{id}                  # Detalhes por id (UUID) — publica product-viewed
GET    /api/v1/catalog/products/slug/{slug}           # Detalhes por slug (usado pela URL pública do frontend)
GET    /api/v1/catalog/categories                     # Lista categorias
GET    /api/v1/catalog/categories/{id}/products       # Produtos por categoria

# Admin (requer role ADMIN via JWT)
POST   /api/v1/catalog/products                       # Criar produto (sem imagem)
PUT    /api/v1/catalog/products/{id}                  # Atualizar produto
DELETE /api/v1/catalog/products/{id}                  # Remover produto
POST   /api/v1/catalog/categories                     # Criar categoria

# Admin — imagens (fluxo em 2 passos, ver seção Upload de Imagens)
POST   /api/v1/catalog/products/{id}/images           # Upload multipart (1..N arquivos)
DELETE /api/v1/catalog/products/{id}/images/{imageId} # Remove imagem específica
```

## Identificadores de Produto — SKU, Slug e ID

Três identificadores com papéis distintos, nunca usados um pelo outro:

| Campo | Tipo | Papel | Exposto ao cliente final? |
|---|---|---|---|
| `id` | UUID (PK) | Chave técnica — FK usada por Cart, Order, Inventory. Nunca muda. | Só em payloads de API, como campo opaco — nunca na URL |
| `sku` | `VARCHAR(50)` legível (`FLT-KN-0042`) | Identidade de negócio — nota fiscal, suporte, estoque físico. Nunca muda após criado. | Sim — exibido na ficha técnica da PDP e no item do pedido |
| `slug` | `VARCHAR(255)` (`filtro-de-ar-kn-0042`) | URL amigável/SEO — muda se o nome do produto mudar | Sim — usado na URL pública (`/produto/{slug}`) |

**Geração:**
- `sku`: informado pelo admin no cadastro (validação de unicidade); se vazio, gerado como
  `{PREFIXO_CATEGORIA}-{SEQ}` (nunca puramente sequencial sozinho — evita expor volume de catálogo
  a concorrentes).
- `slug`: gerado automaticamente a partir do `name` (slugify: minúsculas, sem acento, `-` no lugar
  de espaço); sufixo numérico incremental se colidir (`filtro-de-ar-kn`, `filtro-de-ar-kn-2`, ...).

**Por que não usar o UUID como identificador visível:** não carrega semântica de negócio, não é
memorável, e URLs com UUID cru são ruins pra SEO. SKU serve pra operação (suporte/logística), slug
serve pra navegação/SEO — nunca a mesma string.

## Upload de Imagens (MinIO)

Fluxo de cadastro é sempre em **2 passos**: cria produto primeiro (sem imagem), depois faz upload
das imagens usando o `id` retornado. Evita transação mista multipart+JSON.

1. `POST /products` → cria produto, retorna `id`.
2. `POST /products/{id}/images` (multipart/form-data, campo `files`, 1..N arquivos, `ADMIN`) →
   Catalog Service envia cada arquivo pro MinIO (bucket `catalog-images`, chave `{productId}/{uuid}.{ext}`),
   grava `url` pública do objeto em `product_images` (primeira imagem enviada vira `is_primary=true`).
3. Bucket configurado com policy de leitura anônima (download público) — front consome a `url`
   direto, sem passar pelo Catalog Service.

**Variáveis de ambiente adicionais:**

```
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minio_admin
MINIO_SECRET_KEY=<secret>
MINIO_BUCKET=catalog-images
```

**Validação de upload:** tipos aceitos `image/jpeg`, `image/png`, `image/webp`; tamanho máx. 5MB
por arquivo — rejeitar com `413`/`415` (Problem Details) fora disso.

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
    sku VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    category_id UUID NOT NULL REFERENCES categories(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_slug ON products(slug);

-- estoque (stock_quantity) vive no Inventory Service, chaveado por product_id (UUID desta tabela)

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
  "sku": "FLT-KN-0042",
  "slug": "filtro-de-ar-kn-0042",
  "name": "Filtro de Ar K&N",
  "description": "Filtro de alto desempenho...",
  "price": 299.90,
  "categoryId": "uuid",
  "categoryName": "Filtros",
  "status": "ACTIVE"
}
```

> Ao criar produto (`catalog.product-created`), o Inventory Service consome o evento e cria o
> registro de estoque inicial (`stockQuantity = 0` até admin ajustar via Inventory Service).

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
| `product:slug:{slug}` | 5 min | JSON do produto (lookup por slug) |
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
│   ├── CategoryService.java            # Lógica de negócio de categoria
│   └── SlugGenerator.java              # Slugify de name + resolução de colisão
├── repository/
│   ├── ProductRepository.java          # JpaRepository<Product, UUID>
│   └── CategoryRepository.java         # JpaRepository<Category, UUID>
├── model/
│   ├── Product.java                    # @Entity
│   ├── Category.java                   # @Entity
│   ├── ProductImage.java               # @Entity
│   ├── ProductStatus.java              # Enum: ACTIVE, INACTIVE, OUT_OF_STOCK
│   ├── CreateProductRequest.java       # DTO entrada (sku opcional — gerado se vazio)
│   ├── UpdateProductRequest.java       # DTO entrada
│   └── ProductResponse.java            # DTO saída (sku, slug, stockQuantity lido do Inventory)
├── messaging/
│   └── CatalogEventPublisher.java      # KafkaTemplate producer
├── exception/
│   └── GlobalExceptionHandler.java     # @ControllerAdvice
├── external/
│   └── InventoryServiceClient.java     # @FeignClient(name = "inventory-service")
└── config/
    ├── RedisConfig.java                # CacheManager Redis
    └── KafkaProducerConfig.java
```

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-catalog:5434/catalog_db
DB_USERNAME=catalog_user
DB_PASSWORD=<secret>
REDIS_HOST=redis
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
INVENTORY_SERVICE_URL=http://inventory-service:8006
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY build/libs/catalog-service.jar app.jar
EXPOSE 8003
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
    sourceSets = [sourceSets.main] // não aplica nos testes
}
```

## Estratégia de Testes

- **Unitários:** `ProductService` (CRUD, lógica de cache hit/miss), `CategoryService`
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka); criar produto → verificar cache e evento publicado
- **Cache:** Testar que segunda leitura do mesmo produto vem do Redis (sem hit no banco)
