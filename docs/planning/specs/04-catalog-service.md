# Catalog Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8004 | **Status:** Em implementação

**DER:** [docs/planning/der/catalog-service.mmd](../der/catalog-service.mmd)

## Objetivo

Gerenciar produtos e categorias com cache Redis para reduzir latência e publicação de eventos
Kafka para sincronização com serviços consumidores. Search Service e Compatibility Service (que
consomem `catalog.product-created`/`updated` para busca full-text e fitment peça↔veículo) são
**pós-MVP** — não bloqueiam esta fase.

> **Estoque não é mais responsabilidade deste serviço.** `stock_quantity` foi extraído para o
> [Inventory Service](06-inventory-service.md) — Catalog é orientado a leitura/cache e não tem
> semântica de reserva/concorrência necessária para controlar quantidade de forma segura sob
> checkout simultâneo. Ver decisão em
> [docs/planning/action-plan.md](../action-plan.md#decisões-de-consolidação).

## PRD Resumido

Sem um catálogo centralizado, cada serviço precisaria manter sua própria cópia de nome, preço e
categoria de produto, gerando inconsistência entre o que o cliente vê e o que é cobrado. O Catalog
Service resolve isso concentrando os dados descritivos do produto com cache para leitura rápida.
Usado diretamente pelo cliente final (navegação e busca por categoria/marca) e pelo administrador
(CRUD de produtos/categorias/imagens); consumido também pelo Cart Service (OpenFeign, validação de
produto/preço). Nesta versão (Fase 3) o Catalog Service **não** depende do Inventory Service — essa
dependência (OpenFeign, para exibir disponibilidade) só entra na Fase 5, quando o Inventory Service
existir. Valor de negócio: reduz latência de navegação via cache Redis, mantém SKU e slug como
identificadores de negócio estáveis, e desacopla estoque (alta concorrência) da leitura de catálogo.

> **Decisão de escopo — `stockQuantity` fica fora do Catalog Service até a Fase 5.** O Inventory
> Service ainda não existe (Catalog é Fase 3, Inventory é Fase 5 do roadmap) — não há hoje nenhuma
> fonte real para resolver disponibilidade de estoque. Decisão confirmada: **remover o campo
> `stockQuantity`** de `CreateProductRequest`, `UpdateProductRequest`, `ProductResponse` e do evento
> `ProductChangedEvent` — nenhum destes DTOs/eventos carrega esse campo nesta versão do serviço. A
> chamada OpenFeign ao Inventory Service (e o campo de disponibilidade de estoque na resposta) só
> volta a existir quando o Inventory Service for criado na Fase 5; até lá, "disponibilidade em
> estoque" não é responsabilidade do Catalog Service. Detalhe da decisão e do estado do código no
> momento desta revalidação em
> [Estado real da integração com Inventory Service](#estado-real-da-integração-com-inventory-service).

> **Marca (Brand) como entidade de catálogo.** Produto tem `brandId` obrigatório (`NOT NULL`),
> referenciando uma marca (`brands`). Diferente de Categoria, **Marca é somente leitura nesta
> versão** — existe endpoint `GET /api/v1/catalog/brands` (listagem) e resolução por id ao
> criar/atualizar produto, mas não há `POST`/`PUT`/`DELETE` de marca; o catálogo de marcas é
> populado só via seed Flyway (`V2__seed_brands.sql`). Ver
> [Marca (Brand)](#marca-brand) para detalhe completo.

## Use Cases

- Como cliente final, quero listar produtos paginados e filtrar por categoria, para encontrar peças
  automotivas que atendam minha necessidade.
- Como cliente final, quero ver os detalhes de um produto pelo slug amigável, para acessar a página
  do produto por uma URL legível e compartilhável.
- Como administrador, quero cadastrar um novo produto com SKU e slug gerados/validados
  automaticamente, para publicá-lo no catálogo sem colisão de identificadores.
- Como administrador, quero vincular o produto a uma marca (`brandId`) já cadastrada no catálogo,
  para exibir a marca correta na ficha do produto e permitir navegação por marca.
- Como cliente final, quero listar as marcas cadastradas, para reconhecer fabricantes conhecidos
  antes de abrir a ficha do produto.
- Como administrador, quero fazer upload de uma ou mais imagens para um produto já criado, para
  completar a ficha do produto em um fluxo de dois passos.
- Como administrador, quero remover uma imagem específica de um produto, para corrigir a galeria
  sem recriar o produto inteiro.
- Como administrador, quero atualizar ou remover um produto existente, para manter o catálogo
  atualizado.
- Como administrador, quero criar categorias com nome e slug únicos, para estruturar a navegação do
  catálogo por categoria (lista plana nesta versão — sem hierarquia pai/filho; ver nota em
  [Schema do Banco](#schema-do-banco-flyway)).
- Como Inventory Service, quero ser avisado da criação de um produto via evento Kafka
  `catalog.product-created`, para inicializar o registro de estoque correspondente.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Listagem com cache | Produto já consultado uma vez (cache `product:{id}` populado) | Cliente chama `GET /api/v1/catalog/products/{id}` novamente dentro do TTL de 5 min | Serviço responde a partir do Redis, sem nova consulta ao PostgreSQL |
| Criação de produto com sucesso | Categoria existente e `sku`/`slug` únicos ou omitidos | Admin chama `POST /api/v1/catalog/products` com dados válidos | Serviço cria o produto, gera `slug` (e `sku` se omitido), retorna 201 e publica `catalog.product-created` |
| Criação com SKU duplicado | Já existe produto com o mesmo `sku` | Admin chama `POST /api/v1/catalog/products` reusando o SKU | Serviço retorna 409, sem criar produto nem publicar evento |
| Upload de imagem válida | Produto já criado, sem imagens | Admin chama `POST /api/v1/catalog/products/{id}/images` com arquivo `image/png` de até 5MB | Serviço grava o objeto no MinIO, persiste registro em `product_images` com `is_primary=true` na primeira imagem, e retorna 201 |
| Upload de imagem inválida | Produto já criado | Admin envia arquivo `application/pdf` ou maior que 5MB | Serviço rejeita com 415 (tipo) ou 413 (tamanho), sem gravar no MinIO |
| Atualização invalida cache | Produto já em cache | Admin chama `PUT /api/v1/catalog/products/{id}` alterando o preço | Serviço atualiza o registro, aplica `@CacheEvict` na chave `product:{id}` e evento `catalog.product-updated` é publicado com o novo preço |
| Listagem de marcas | Marcas seedadas via Flyway (`V2__seed_brands.sql`) | Cliente chama `GET /api/v1/catalog/brands` | Serviço retorna 200 com a lista de marcas ordenada alfabeticamente por `name` |
| Criação de produto com marca existente | Marca (`brandId`) e categoria existentes | Admin chama `POST /api/v1/catalog/products` informando `brandId` válido | Serviço cria o produto vinculado à marca e retorna `brandName`/`brandSlug` resolvidos no `ProductResponse` |
| Criação de produto com marca inexistente | `brandId` não corresponde a nenhuma marca cadastrada | Admin chama `POST /api/v1/catalog/products` com esse `brandId` | Serviço retorna 404 (`BrandNotFoundException`), sem criar produto nem publicar evento |
| Atualização de marca do produto | Produto já criado, nova marca (`brandId`) existente | Admin chama `PUT /api/v1/catalog/products/{id}` alterando `brandId` | Serviço resolve e persiste a nova marca, invalida cache do produto e publica `catalog.product-updated` |
| Resposta de produto sem estoque | Produto criado normalmente | Cliente chama `GET /api/v1/catalog/products/{id}` | `ProductResponse` **não** contém `stockQuantity` nem nenhum campo de disponibilidade de estoque — Catalog Service não resolve estoque nesta versão (ver [Estado real da integração com Inventory Service](#estado-real-da-integração-com-inventory-service)) |
| Criação de categoria (lista plana) | Nenhuma categoria com o mesmo `slug` cadastrada | Admin chama `POST /api/v1/catalog/categories` com `name` válido | Serviço cria a categoria sem nenhuma noção de categoria pai/filho, gera `slug` e retorna 201 |

## Banco de Dados: PostgreSQL (`catalog_db`) + Redis (cache) + MinIO (imagens)

## Responsabilidades

- CRUD completo de produtos (admin) — dados descritivos: nome, descrição, preço, categoria, marca, imagens
- CRUD de categorias — **lista plana nesta versão, sem hierarquia pai/filho**; ver nota em [Schema do Banco](#schema-do-banco-flyway)
- Listagem de marcas (admin/cliente final) — **somente leitura**, sem CRUD; ver [Marca (Brand)](#marca-brand)
- Gerar e validar identificadores de produto: `sku` (código de negócio) e `slug` (URL amigável) — ver [Identificadores de Produto](#identificadores-de-produto-sku-slug-e-id)
- Upload/remoção de imagens de produto (MinIO) — ver [Upload de Imagens](#upload-de-imagens-minio)
- Listagem paginada de produtos com filtro por categoria
- Cache de produtos no Redis (TTL 5 minutos)
- Publicar: `catalog.product-created`, `catalog.product-updated`, `catalog.product-viewed`

> **Fora de escopo nesta versão (Fase 3):** disponibilidade de estoque (`stockQuantity`) e a
> integração OpenFeign com o Inventory Service. Catalog Service não resolve nem expõe estoque —
> essa responsabilidade só entra na Fase 5, quando o Inventory Service existir. Ver
> [Estado real da integração com Inventory Service](#estado-real-da-integração-com-inventory-service).

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
GET    /api/v1/catalog/brands                         # Lista marcas (ordenada por name) — somente leitura

# Admin (requer role ADMIN via JWT)
POST   /api/v1/catalog/products                       # Criar produto (sem imagem)
PUT    /api/v1/catalog/products/{id}                  # Atualizar produto
DELETE /api/v1/catalog/products/{id}                  # Remover produto
POST   /api/v1/catalog/categories                     # Criar categoria
# Não existe POST/PUT/DELETE de marca nesta versão — ver "Marca (Brand)"

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

## Marca (Brand)

Entidade simples de catálogo (`brands`), independente de `Category`. Cada `Product` referencia
exatamente uma marca via `brand_id` (`NOT NULL`) — assim como `category_id`, resolvida no
`ProductService` (nunca via join automático) e montada no `ProductResponse` como `brandId`,
`brandName`, `brandSlug`.

**Escopo implementado — somente leitura:**
- `GET /api/v1/catalog/brands` → lista todas as marcas, ordenadas alfabeticamente por `name`
  (`findAllByOrderByNameAsc`). Não paginado (volume de marcas é baixo, mesmo padrão de
  `GET /categories`).
- Resolução por `id` ao criar (`POST /products`) ou atualizar (`PUT /products/{id}`) produto — se o
  `brandId` informado não existir, o serviço responde `404 Not Found` (`BrandNotFoundException`).
- **Não há** `POST`/`PUT`/`DELETE` de marca nesta versão — o catálogo de marcas é fechado, populado
  só via seed Flyway (`V2__seed_brands.sql`, ~19 marcas cobrindo as 10 categorias do MVP). Cadastro
  de marca via API é evolução futura, fora do escopo atual — não fazer sem antes confirmar com o
  software-architect/product-owner, para não abrir escrita descontrolada num identificador de
  negócio compartilhado por todos os produtos.

**Schema:** ver tabela `brands` em [Schema do Banco (Flyway)](#schema-do-banco-flyway).

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

> **Decisão de escopo — categoria é lista plana nesta versão.** `categories` **não tem** coluna
> `parent_id` — é lista plana, sem hierarquia pai/filho. Decisão confirmada: manter o schema como
> está (4 migrações, `V1`-`V4`, abaixo) e **não** adicionar `parent_id` nesta revalidação —
> hierarquia de categorias fica registrada como incremento futuro (evolução pós-conclusão do
> serviço), a ser especificado com o software-architect/product-owner só quando houver necessidade
> de negócio concreta (ex.: navegação por subcategoria). Use Cases e Critérios de Aceite desta spec
> já refletem essa decisão — nenhum cenário depende de `parent_id`.

### V1__create_catalog_schema.sql

```sql
CREATE TABLE brands (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_brands_slug ON brands(slug);


CREATE TABLE categories (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_slug ON categories(slug);


CREATE TABLE products (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255)   NOT NULL,
    sku            VARCHAR(50)    NOT NULL UNIQUE,
    slug           VARCHAR(255)   NOT NULL UNIQUE,
    description    TEXT,
    price          NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    brand_id       UUID NOT NULL REFERENCES brands(id),
    category_id    UUID           NOT NULL REFERENCES categories(id),
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status   ON products(status);
CREATE INDEX idx_products_sku      ON products(sku);
CREATE INDEX idx_products_slug     ON products(slug);

-- estoque (stock_quantity) vive no Inventory Service, chaveado por product_id (UUID desta tabela)

CREATE TABLE product_images (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url        VARCHAR(1024) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);
```

### V2__seed_brands.sql

Seed de ~19 marcas (`INSERT INTO brands (id, name, slug, created_at) VALUES ...`), cobrindo as
categorias do MVP (ex.: Sparco/Metal Horse em Acessórios, Brembo em Freios, NGK/Bosch/AFP em Motor,
K&N/FuelTech/Pro Line em Performance, Michelin/Goodyear em Pneus, BBS/Enkei/OZ Racing em Rodas,
Eibach/D2 Racing em Suspensão, OSRAM/Philips em Iluminação, Vonixx/Cadillac em Limpeza).

### V3__seed_categories.sql

Seed das 10 categorias do MVP (`INSERT INTO categories (id, name, slug, created_at) VALUES ...`):
Acessórios, Escapamento, Freios, Iluminação, Limpeza, Motor, Performance, Pneus, Rodas, Suspensão.

### V4__seed_products.sql

Seed de produtos de exemplo, cada um resolvendo `category_id`/`brand_id` via subquery por `slug`
(`(SELECT id FROM categories WHERE slug = '...')`, `(SELECT id FROM brands WHERE slug = '...')`) —
cobre os três valores de `status` (`ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`) e todas as combinações
categoria/marca seedadas em `V2`/`V3`.

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

> Quando o Inventory Service existir (Fase 5), ele passa a consumir `catalog.product-created` para
> criar o registro de estoque inicial correspondente ao `productId`. O payload publicado pelo
> Catalog Service **não carrega `stockQuantity`** — a quantidade inicial é responsabilidade do
> Inventory Service, não do Catalog Service.

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

> Estrutura abaixo reflete o código real (`backend/catalog-service/src/main/java/`), não a proposta
> original desta spec — divergências relevantes: pacote `domain/` (não `model/`) com subpastas
> `entity/`, `dto/request/`, `dto/response/`, `mapper/`, `enums/`, `projection/`; `controller/docs/`
> com as interfaces `*ControllerDocs` (Springdoc); `exception/handler/` para o
> `@ControllerAdvice`; entidade/serviço/controller de `Brand` (leitura); **não existe** pacote
> `external/` nem `InventoryServiceClient` — ver
> [Estado real da integração com Inventory Service](#estado-real-da-integração-com-inventory-service).

```
com.autohubstore.catalogservice/
├── controller/
│   ├── ProductController.java              # CRUD produtos + listagem pública
│   ├── CategoryController.java             # CRUD categorias + produtos por categoria
│   ├── BrandController.java                # GET /brands — somente leitura
│   └── docs/
│       ├── ProductControllerDocs.java      # Interface Springdoc (Operation/ApiResponses)
│       ├── CategoryControllerDocs.java
│       └── BrandControllerDocs.java
├── service/
│   ├── ProductService.java                 # Lógica de negócio de produto + cache + sku/slug
│   ├── ProductImageService.java            # Upload/remoção de imagens (MinIO)
│   ├── CategoryService.java                # Lógica de negócio de categoria
│   └── BrandService.java                   # findBrands, findEntityOrThrow — sem escrita
├── repository/
│   ├── ProductRepository.java              # JpaRepository<Product, UUID>
│   ├── ProductImageRepository.java         # JpaRepository<ProductImage, UUID>
│   ├── CategoryRepository.java             # JpaRepository<Category, UUID>
│   └── BrandRepository.java                # JpaRepository<Brand, UUID> — findAllByOrderByNameAsc
├── domain/
│   ├── entity/
│   │   ├── Product.java                    # @Entity — brandId, categoryId como UUID simples
│   │   ├── Category.java                   # @Entity — sem parent_id (lista plana, ver Schema)
│   │   ├── ProductImage.java               # @Entity
│   │   └── Brand.java                      # @Entity — id, name, slug, createdAt
│   ├── enums/
│   │   └── ProductStatus.java              # Enum: ACTIVE, INACTIVE, OUT_OF_STOCK
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateProductRequest.java   # sku opcional (gerado se vazio); brandId obrigatório
│   │   │   ├── UpdateProductRequest.java   # todos os campos opcionais, inclui brandId
│   │   │   └── CreateCategoryRequest.java
│   │   └── response/
│   │       ├── ProductResponse.java        # inclui brandId/brandName/brandSlug, images
│   │       ├── ProductImageResponse.java
│   │       ├── CategoryResponse.java       # inclui productCount
│   │       └── BrandResponse.java          # id, name, slug — sem write DTO (não há criação)
│   ├── mapper/
│   │   ├── ProductMapper.java              # MapStruct — toEntity/toResponse/updateEntityFromRequest
│   │   ├── CategoryMapper.java
│   │   ├── ProductImageMapper.java
│   │   └── BrandMapper.java                # MapStruct — só toResponse (sem toEntity)
│   └── projection/
│       └── CategoryProductCountProjection.java
├── messaging/
│   ├── CatalogEventPublisher.java          # KafkaTemplate producer
│   ├── ProductChangedEvent.java            # payload de product-created/updated
│   └── ProductViewedEvent.java             # payload de product-viewed
├── exception/
│   ├── ProductNotFoundException.java
│   ├── ProductSkuAlreadyExistsException.java
│   ├── CategoryNotFoundException.java
│   ├── CategorySlugAlreadyExistsException.java
│   ├── BrandNotFoundException.java
│   ├── UnsupportedImageTypeException.java
│   ├── SecurityInitializationException.java
│   └── handler/
│       └── GlobalExceptionHandler.java     # @RestControllerAdvice (ProblemDetail)
└── config/
    ├── RedisConfig.java                    # CacheManager Redis (nomes de cache)
    ├── KafkaProducerConfig.java
    ├── MinioConfig.java                    # Client MinIO (bucket catalog-images)
    ├── OpenApiConfig.java                  # Springdoc
    └── SecurityConfig.java                 # Spring Security (roles ADMIN em endpoints admin)
```

## Estado real da integração com Inventory Service

**Decisão confirmada:** opção (b) — **remover `stockQuantity` agora** de todo DTO/evento do Catalog
Service e reintroduzir a integração só quando o Inventory Service existir (Fase 5 do roadmap;
Catalog é Fase 3, criado antes do Inventory). Não faz sentido manter o campo ou criar
`InventoryServiceClient` (`@FeignClient`) hoje — apontaria para um serviço que ainda não existe.
Catalog Service não tem, e não deve ter nesta versão, nenhuma noção de disponibilidade de estoque.

**Ação concluída pelo backend-engineer — decisão aplicada de forma completa no código, verificada
por revalidação independente do quality-analyst.** Levantamento anterior havia apontado remoção só
parcial; numa rodada de correção de bugs de build/testes do catalog-service, o backend-engineer
resolveu a pendência (achado extra, fora do escopo original daquela correção) e removeu o campo dos
arquivos que ainda o mantinham:

| Arquivo | Estado do campo `stockQuantity` |
|---|---|
| `domain/dto/request/CreateProductRequest.java` | Removido |
| `domain/dto/request/UpdateProductRequest.java` | Removido |
| `domain/dto/response/ProductResponse.java` | Removido |
| `messaging/ProductChangedEvent.java` | Removido |

Na remoção, ficou confirmado que os pontos abaixo também não têm mais referência solta ao campo:

- `ProductMapper` (`updateEntityFromRequest`, `toResponse`) — sem leitura/atribuição de
  `stockQuantity`.
- `CatalogEventPublisher`/lugar que monta `ProductChangedEvent` — payload publicado em
  `catalog.product-created`/`catalog.product-updated` sem o campo.
- Testes unitários/aceitação — nenhum monta request/response/evento com `stockQuantity`.
- `Product` (entidade JPA) já **não tem** coluna `stock_quantity` — nada a fazer na entidade nem no
  schema Flyway (isso não mudou).

Nenhum cenário de teste (unitário ou aceitação) deve depender de `stockQuantity` — ver Critérios de
Aceite ("Resposta de produto sem estoque"). A integração real com o Inventory Service (client
OpenFeign, campo de disponibilidade na resposta) só volta à spec como use case novo na Fase 5.

## Exceção Snyk Aceita — CVE `snappy-java`

**Decisão registrada em ADR-009 ([CLAUDE.md § Decisões Arquiteturais](../../../CLAUDE.md#decisões-arquiteturais-adrs)).**
Item 5 (Snyk `ok: true`) do [Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
fica com uma exceção formal e documentada para o Catalog Service — não é vulnerabilidade
suprimida/ignorada, é risco residual aceito conscientemente pelo usuário.

- **CVE:** `SNYK-JAVA-ORGXERIALSNAPPY-19778376` — Out-of-bounds Write, severidade **High**.
- **Dependência afetada:** `org.xerial.snappy:snappy-java@1.1.10.8`.
- **Por que existe no projeto:** é dependência transitiva **obrigatória** de `io.minio:minio:9.0.1`
  — o client MinIO referencia a classe `SnappyFramedOutputStream` de forma incondicional dentro do
  seu `Builder` (`NoClassDefFoundError` se a lib for excluída, confirmado por decompilação de
  bytecode durante a revalidação do serviço). Não é dependência de conveniência que possa ser
  simplesmente removida.
- **Situação na data da decisão (2026-09-16):** `1.1.10.8` é a última versão publicada de
  `snappy-java`; o próprio Snyk reporta explicitamente "No upgrade or patch available" — não existe
  hoje nenhuma versão corrigida para pinar.
- **Decisão do usuário:** aceitar o risco residual, sem suprimir/ignorar a vulnerabilidade via
  `.snyk` policy nem qualquer outro mecanismo de mascaramento — ela continua aparecendo no relatório
  Snyk do serviço, visível e rastreável.
- **Gatilho de revisão:** reexecutar `snyk test --all-sub-projects --detection-depth=6` no
  catalog-service a cada revalidação/entrega subsequente do serviço. Assim que uma versão corrigida
  de `snappy-java` for publicada, ou o Snyk indicar patch disponível, atualizar a dependência
  imediatamente e remover esta exceção do ADR-009 e desta seção. Alternativa a considerar nessa
  revisão futura: avaliar troca do client MinIO (`io.minio:minio`) por versão/lib que não dependa
  incondicionalmente de `snappy-java`.
- **Enquanto a exceção estiver ativa:** o item 5 da [Tabela de evidência](../action-plan.md#tabela-de-evidência--obrigatória-ao-final)
  do Catalog Service deve ser reportado como "⚠️ Pendência aceita (ADR-009)", nunca como ✅ liso nem
  como ❌ sem contexto.

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
COPY build/libs/catalog-service.jar app.jar
EXPOSE 8004
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

- **Unitários:** `ProductService` (CRUD, lógica de cache hit/miss, resolução de `brandId`/`categoryId`), `CategoryService`, `BrandService` (listagem, `findEntityOrThrow`)
- **Integração:** Testcontainers (PostgreSQL + Redis + Kafka); criar produto → verificar cache e evento publicado
- **Cache:** Testar que segunda leitura do mesmo produto vem do Redis (sem hit no banco)

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/catalog-service.md`.
