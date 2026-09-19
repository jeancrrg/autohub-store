# Catalog Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8004 | **Status:** Em implementação

**Código:** `backend/catalog-service/` | **Spec:** [docs/planning/specs/04-catalog-service.md](../planning/specs/04-catalog-service.md)
| **DER:** [docs/planning/der/catalog-service.mmd](../planning/der/catalog-service.mmd)

## Objetivo

O Catalog Service é a fonte de verdade dos produtos vendidos no AutoHubStore: cadastro e consulta
de produtos, categorias e marcas, além do upload das imagens de cada produto. É o serviço que o
frontend consulta para montar vitrine, página de detalhe de produto e filtros por categoria/marca.

Três responsabilidades centrais:

- CRUD de produtos, categorias e marcas, com geração automática de SKU e slug únicos.
- Cache de leitura em Redis (produto por id/slug, listagem por categoria) para reduzir carga no
  PostgreSQL nas consultas mais frequentes do e-commerce.
- Upload/remoção de imagens de produto no MinIO (S3-compatible), com validação de tipo e tamanho.
- Publicação de eventos de domínio no Kafka (`catalog.product-created`, `catalog.product-updated`,
  `catalog.product-viewed`) para os demais serviços do MVP reagirem (hoje, sem consumer ativo no
  MVP — ver `docs/planning/action-plan.md`).

O Catalog Service **não guarda estoque**. `stock_quantity` é responsabilidade do Inventory
Service (Fase 5, ainda não criado no MVP) — decisão registrada no próprio DER do serviço
(`docs/planning/der/catalog-service.mmd`) e reforçada pelo cenário de aceitação "Resposta de
produto sem estoque", que garante que `ProductResponse` nunca expõe campo de quantidade.

## Por que é didaticamente relevante

É o segundo serviço do MVP a usar **cache declarativo do Spring** (`@Cacheable`/`@CacheEvict`/
`@Caching`) sobre Redis com serialização JSON tipada (`GenericJackson2JsonRedisSerializer` +
`activateDefaultTyping`), e o primeiro a integrar um object storage S3-compatible (MinIO) via
`MinioClient`. Também demonstra, na prática, a regra do CLAUDE.md de nunca usar
`@ManyToOne`/`@OneToMany` entre entidades JPA: `Product` referencia `Category`/`Brand` só pelo
`UUID` (`categoryId`, `brandId`), e quem precisa do nome/slug da categoria ou marca busca
explicitamente via `CategoryService`/`BrandService` — nunca via join automático do Hibernate.

## Arquitetura (MVC)

```
com.autohubstore.catalogservice/
├── CatalogServiceApplication.java
├── controller/
│   ├── ProductController.java             # CRUD de produtos + upload/remoção de imagem
│   ├── CategoryController.java            # CRUD de categorias + listagem de produtos por categoria
│   ├── BrandController.java               # listagem de marcas
│   └── docs/                              # *ControllerDocs — anotações Springdoc separadas do controller
├── service/
│   ├── ProductService.java                # regra de negócio de produto: SKU/slug, cache, eventos Kafka
│   ├── ProductImageService.java           # upload/remoção de imagem (MinIO) + validação
│   ├── CategoryService.java               # CRUD de categoria (lista plana, sem hierarquia)
│   └── BrandService.java                  # listagem de marca
├── repository/
│   ├── ProductRepository.java
│   ├── ProductImageRepository.java
│   ├── CategoryRepository.java
│   └── BrandRepository.java
├── domain/
│   ├── entity/                            # Product, Category, Brand, ProductImage — JPA, sem relação direta
│   ├── enums/ProductStatus.java           # ACTIVE, INACTIVE, OUT_OF_STOCK
│   ├── dto/request/                       # CreateProductRequest, UpdateProductRequest, CreateCategoryRequest
│   ├── dto/response/                      # ProductResponse, CategoryResponse, BrandResponse, ProductImageResponse
│   ├── mapper/                            # ProductMapper, CategoryMapper, BrandMapper, ProductImageMapper (MapStruct)
│   └── projection/CategoryProductCountProjection.java  # projeção de query para contagem de produtos por categoria
├── messaging/
│   ├── CatalogEventPublisher.java         # publica nos 3 tópicos Kafka do serviço
│   ├── ProductChangedEvent.java           # payload de catalog.product-created / catalog.product-updated
│   └── ProductViewedEvent.java            # payload de catalog.product-viewed
├── exception/
│   ├── ProductNotFoundException, CategoryNotFoundException, BrandNotFoundException
│   ├── ProductSkuAlreadyExistsException, CategorySlugAlreadyExistsException
│   ├── UnsupportedImageTypeException, ImageTooLargeException, ImageStorageException
│   ├── SecurityInitializationException
│   └── handler/GlobalExceptionHandler.java  # @RestControllerAdvice — ProblemDetail
└── config/
    ├── SecurityConfig.java                # permitAll — validação de JWT/role já é feita no Gateway
    ├── RedisConfig.java                   # RedisCacheManager + nomes/TTL dos 2 caches
    ├── MinioConfig.java                   # bean MinioClient
    ├── KafkaProducerConfig.java
    └── OpenApiConfig.java                 # Springdoc
```

`Product`, `Category`, `Brand` e `ProductImage` não têm nenhuma anotação de relacionamento JPA
entre si — `Product.categoryId`/`Product.brandId` e `ProductImage.productId` são colunas `UUID`
simples. Quem precisa do dado relacionado (nome da categoria, nome/slug da marca, lista de
imagens de um produto) busca explicitamente pelo `Service` do domínio dono
(`CategoryService.findEntityOrThrow`, `BrandService.findEntityOrThrow`,
`ProductImageService.findImages`), nunca acessando o `Repository` de outro domínio direto.

## Endpoints

### `ProductController` — `/api/v1/catalog/products`

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/` | Lista produtos paginados; `categoryId` (query, opcional) filtra por categoria |
| `GET` | `/{id}` | Busca produto por id; publica `catalog.product-viewed` |
| `GET` | `/slug/{slug}` | Busca produto por slug; publica `catalog.product-viewed` |
| `POST` | `/` | Cria produto (`CreateProductRequest`) |
| `PUT` | `/{id}` | Atualiza produto (`UpdateProductRequest`) |
| `DELETE` | `/{id}` | Remove produto |
| `POST` | `/images/{id}` | Upload de uma ou mais imagens (`multipart/form-data`, campo `files`) |
| `DELETE` | `/images/{id}/{imageId}` | Remove uma imagem do produto |

### `CategoryController` — `/api/v1/catalog/categories`

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/` | Lista categorias (lista plana) com contagem de produtos por categoria |
| `POST` | `/` | Cria categoria (`CreateCategoryRequest`) |
| `GET` | `/{id}/products` | Lista produtos paginados daquela categoria |

### `BrandController` — `/api/v1/catalog/brands`

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/` | Lista marcas ordenadas alfabeticamente por nome |

Todos os endpoints estão liberados no `SecurityConfig` do próprio serviço (`permitAll`) — a
validação de JWT/role (ex.: exigir role `ADMIN` para criação/atualização/remoção) é feita
centralizadamente no API Gateway antes de rotear a requisição para `/api/v1/catalog/**`, não aqui.

## Regras de negócio

### Geração de SKU

`ProductService.resolveSku`: se o `CreateProductRequest.sku()` vier preenchido, valida unicidade
(`ProductRepository.existsBySku`) e lança `ProductSkuAlreadyExistsException` (409) em caso de
conflito. Se vier vazio, `generateSku` monta um SKU automático: prefixo de até 3 letras derivado
do nome da categoria (sem acentos/caracteres não alfanuméricos, maiúsculo) + sufixo aleatório de 8
caracteres hexadecimais (derivado de `UUID.randomUUID()`), repetindo a geração em loop até
encontrar um valor ainda não existente na base.

### Geração de slug

`ProductService.generateUniqueSlug`/`slugify`: normaliza o nome do produto (remove diacríticos via
`Normalizer.normalize(NFD)`), converte para minúsculo, substitui sequências de caracteres não
alfanuméricos por hífen e remove hífen nas extremidades. Se o slug candidato já existir
(`ProductRepository.existsBySlug`), acrescenta sufixo numérico incremental (`-2`, `-3`, ...) até
achar um valor livre. Slug é regenerado na atualização somente quando o nome do produto muda.

### Cache Redis (`RedisConfig`)

Dois caches nomeados, com TTL próprio, configurados em `RedisCacheManager`:

| Cache | Constante | TTL | Uso |
|---|---|---|---|
| `products` | `RedisConfig.CACHE_PRODUCTS` | 5 min | `findProduct(id)` (chave `#id`) e `findProductBySlug(slug)` (chave `'slug:' + #slug`) |
| `products-by-category` | `RedisConfig.CACHE_PRODUCTS_BY_CATEGORY` | 2 min | `findProductsByCategory` (chave `categoryId:pageNumber:pageSize`) |

`createProduct` não invalida cache (produto novo não está cacheado ainda); `updateProduct` e
`deleteProduct` usam `@Caching(evict = {...})` para limpar `allEntries = true` dos dois caches —
invalidação ampla, não seletiva por id, para não deixar `products-by-category` (que soma múltiplos
produtos por chave) inconsistente. Serialização usa `GenericJackson2JsonRedisSerializer` com
`ObjectMapper` próprio (`activateDefaultTyping`), separado do `ObjectMapper` HTTP da aplicação —
necessário para o Redis conseguir desserializar de volta o tipo concreto (`ProductResponse`,
`Page<ProductResponse>`) sem informação de tipo do payload JSON puro.

### Upload de imagem (MinIO) — `ProductImageService`

- Tipos permitidos: `image/jpeg`, `image/png`, `image/webp` — outro `Content-Type` lança
  `UnsupportedImageTypeException` (415).
- Tamanho máximo: 5 MB (`5L * 1024 * 1024` bytes) — acima disso lança `ImageTooLargeException`
  (413).
- A primeira imagem enviada para um produto que ainda não tem nenhuma imagem marcada como
  principal (`isPrimary`) é automaticamente marcada como principal (`uploadImages`: calcula
  `hasExistingPrimary`/`noImagesYet` antes do loop de upload, e só a primeira do lote recebe
  `primary = true`, quando ainda não há nenhuma principal cadastrada).
- Objeto salvo no bucket com chave `{productId}/{uuid-aleatório}{extensão-do-arquivo-original}`;
  URL pública persistida no banco é `/{bucket}/{objectKey}`.
- Falha de I/O ou do cliente MinIO (`MinioException`/`IOException`) ao subir ou remover o arquivo
  vira `ImageStorageException`, mapeada para 500 pelo `GlobalExceptionHandler` (com log de erro,
  sem detalhar a causa raiz na resposta ao cliente).
- Remoção de imagem (`deleteImage`) primeiro remove o objeto do MinIO, depois a linha do banco —
  produto ou imagem inexistente (ou imagem de outro produto) lança `ProductNotFoundException`
  (404).

### Categoria em lista plana

Diferente de um catálogo hierárquico tradicional (categoria pai/filho), este serviço trata
categoria como lista plana — decisão registrada explicitamente no DER
(`docs/planning/der/catalog-service.mmd`) e na spec (`docs/planning/specs/04-catalog-service.md §
Schema do Banco`), como divergência assumida frente ao PRD original. `CategoryService.
findCategories` retorna todas as categorias ordenadas por `createdAt`, cada uma já com a contagem
de produtos vinculados (`CategoryProductCountProjection`, resolvida em uma query agregada única em
vez de N+1 chamadas).

## Eventos Kafka publicados (`CatalogEventPublisher`)

| Tópico | Payload | Disparado por |
|---|---|---|
| `catalog.product-created` | `ProductChangedEvent` | `ProductService.createProduct` |
| `catalog.product-updated` | `ProductChangedEvent` | `ProductService.updateProduct` |
| `catalog.product-viewed` | `ProductViewedEvent` | `ProductController.findProduct`/`findProductBySlug` (via `ProductService.publishProductViewed`) |

`ProductChangedEvent` (record) carrega `productId`, `sku`, `slug`, `name`, `description`, `price`,
`categoryId`, `categoryName` (já resolvido, evitando o consumer ter que buscar a categoria de
volta) e `status`. `ProductViewedEvent` carrega `productId`, `productName` e `viewedAt`.
Publicação é assíncrona (`KafkaTemplate.send(...).whenComplete(...)`), com log de erro em caso de
falha — a falha de publicação **não** propaga exceção para o chamador HTTP (a operação de negócio
já foi persistida no banco antes do evento ser publicado).

Segundo o mapa de tópicos do `CLAUDE.md`, nenhum desses três tópicos tem consumer ativo hoje no
MVP (Inventory Service, que consumiria `catalog.product-created`, ainda não foi implementado); os
eventos são publicados mesmo assim, prontos para quando o consumer existir.

## Tratamento de erros (`GlobalExceptionHandler`)

Todas as respostas de erro usam `ProblemDetail` (RFC 9457), montado manualmente por
`ExceptionHandler` (sem usar os atalhos `ResponseEntity.badRequest()`/etc, conforme convenção do
projeto):

| Exceção | Status | Observação |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Bean Validation — mapa `campo → mensagem` (usa a `message` explícita de cada anotação) |
| `IllegalArgumentException` | 400 | — |
| `ProductNotFoundException` | 404 | — |
| `CategoryNotFoundException` | 404 | — |
| `BrandNotFoundException` | 404 | — |
| `CategorySlugAlreadyExistsException` | 409 | — |
| `ProductSkuAlreadyExistsException` | 409 | — |
| `UnsupportedImageTypeException` | 415 | — |
| `ImageTooLargeException` | 413 | — |
| `ImageStorageException` | 500 | Loga a causa raiz (`log.error`), mas não a expõe no corpo da resposta |
| `Exception` (genérica) | 500 | Fallback final — loga `Unhandled exception` |

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://localhost:5434/catalog_db
DB_USERNAME=catalog_user
DB_PASSWORD=catalog_pass
REDIS_HOST=localhost
REDIS_PORT=6379
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minio_admin
MINIO_SECRET_KEY=minio_pass
MINIO_BUCKET=catalog-images
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Todas com default local em `application.yml` (`${VARIAVEL:valor-default}`). Porta HTTP fixa em
`8004` (`server.port`). `spring.jackson.property-naming-strategy: SNAKE_CASE` garante que todo
JSON de resposta sai em `snake_case`, mesmo com o campo Java em `lowerCamelCase`
(`categoryId` → `category_id`, e assim por diante).

## Como rodar localmente

Infraestrutura necessária (fora do escopo de agente — subida pelo usuário):

```bash
cd infra
docker compose up -d postgres-catalog redis kafka zookeeper
# MinIO ainda não está no docker-compose.yml do projeto — subir um MinIO local
# (ex.: imagem minio/minio) apontando MINIO_ENDPOINT/MINIO_ACCESS_KEY/MINIO_SECRET_KEY
# para o mesmo endereço, ou usar o bucket já existente do ambiente de desenvolvimento.
```

Depois, abrir `backend/catalog-service/build.gradle` no IntelliJ (SDK JDK 25), rodar
`CatalogServiceApplication`. Health check: `curl http://localhost:8004/actuator/health`.

## Testes

Estrutura em `src/test/java/com/autohubstore/catalogservice/`:

### Unitários (`unit/service/`)

- `ProductServiceTest` — geração de SKU (informado vs. automático, conflito de SKU), geração de
  slug (novo, com conflito e sufixo incremental, mantido quando nome não muda), publicação dos
  eventos `catalog.product-created`/`catalog.product-updated`/`catalog.product-viewed`,
  `ProductNotFoundException` para id/slug inexistente.
- `ProductImageServiceTest` — upload válido marcando a primeira imagem como principal, rejeição de
  tipo de conteúdo não suportado, rejeição de arquivo acima de 5 MB, falha do MinIO convertida em
  `ImageStorageException`, remoção de imagem existente/inexistente.
- `CategoryServiceTest` — criação com slug único, conflito de slug (`CategorySlugAlreadyExistsException`),
  listagem com contagem de produtos por categoria.
- `BrandServiceTest` — listagem ordenada alfabeticamente, busca de marca inexistente
  (`BrandNotFoundException`).

Padrão JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) e AssertJ, conforme convenção do
CLAUDE.md.

### Aceitação (Cucumber — `src/test/resources/features/catalog-service.feature`)

Cenários cobertos (`acceptance/steps/CatalogServiceAcceptanceSteps.java`):

1. Listagem com cache — preço alterado direto no banco não aparece na segunda consulta enquanto o
   cache `products` estiver válido.
2. Criação de produto com sucesso — 201 e publicação do evento `catalog.product-created`.
3. Criação com SKU duplicado — 409.
4. Criação com marca existente — 201, resposta inclui nome/slug da marca resolvida.
5. Criação com marca inexistente — 404.
6. Upload de imagem válida — 201, primeira imagem marcada como principal.
7. Upload de imagem inválida (`Esquema do Cenario`) — PDF (415) e imagem acima de 5 MB (413).
8. Atualização com invalidação de cache — preço atualizado refletido na consulta seguinte, evento
   `catalog.product-updated` publicado.
9. Listagem de marcas — ordenação alfabética.
10. Atualização da marca do produto — refletida na consulta seguinte.
11. Resposta de produto sem estoque — `ProductResponse` nunca expõe campo de quantidade em estoque.
12. Criação de categoria em lista plana — 201.

Infraestrutura de teste (`acceptance/config/CucumberConfig.java`) sobe via Testcontainers:
PostgreSQL (`postgres:16-alpine`), Kafka (`apache/kafka:3.7.0`), Redis (`redis:7-alpine`) e MinIO
(`minio/minio:RELEASE.2024-01-16T16-07-38Z`, com bucket `catalog-images` criado no bloco `static`)
— nunca a infraestrutura real do `docker-compose.yml` do projeto. Cliente HTTP de teste é
`MockMvc` (serviço servlet/MVC, não reativo), disparado sempre via
`acceptance/util/HttpAcceptanceTestUtil.java`.

### Como rodar

```bash
cd backend/catalog-service
JAVA_HOME=C:\Users\jeanc\.jdks\ms-25.0.4
./gradlew checkstyleMain checkstyleTest   # checkstyle (main + test)
./gradlew test                           # unitários + aceitação Cucumber
./gradlew jacocoTestCoverageVerification # gate de cobertura (mínimo 70% de linha)
./gradlew jacocoTestReport                # relatório HTML em build/reports/jacoco/test/html/index.html
./gradlew build                           # build final (build/libs/catalog-service.jar)
snyk test --all-sub-projects --detection-depth=6
```

Pré-requisito para os testes de aceitação: Docker Desktop em execução (Testcontainers sobe
PostgreSQL, Kafka, Redis e MinIO efêmeros). Nenhum comando de teste depende de
`docker compose up` — a infra do `docker-compose.yml` do projeto não precisa estar de pé.

## Dependências relevantes (`build.gradle`)

Spring Boot `4.0.8` + `io.spring.dependency-management` `1.1.7`, Spring Data JPA, Spring Data
Redis, Spring Cache, Spring Security (permitAll — delega autenticação ao Gateway), Spring
Validation, Spring Kafka, Flyway (`flyway-core` + `flyway-database-postgresql`), driver
PostgreSQL, MinIO client `9.0.1` (bump a partir de `8.6.0` para zerar CVEs Snyk), Actuator +
Micrometer Prometheus, Springdoc OpenAPI `3.1.0` (WebMVC), MapStruct `1.6.3`, Lombok `1.18.38`.
Testes: JUnit 5/Spring Boot Test, Spring Security Test, Spring Kafka Test, Testcontainers `2.0.3`
(alinhado ao user-service — PostgreSQL, Kafka, MinIO), Cucumber `7.33.0`, JUnit Platform Suite.

`dependencyManagement` fixa versões de dependências transitivas sem CVE conhecida
(`httpclient5`, `commons-compress`, `lz4-java`, `scala-library`, `jackson-databind` nas duas
linhas — Jackson 2 clássico e Jackson 3 do Spring Boot 4 —, `zstd-jni`, `bcprov-jdk18on`, entre
outras), na mesma estratégia adotada pelo API Gateway e pelo User Service: preferir bump de BOM e
só pinar dependência transitiva isolada quando o bump de BOM não resolve por si só.

`version` do artefato: `1.0.0` (convenção obrigatória do CLAUDE.md para todo serviço do MVP).

## Limitações conhecidas

- Não há integração real (nem OpenFeign) com o Inventory Service — decisão registrada no DER do
  serviço. O campo `status` (`ACTIVE`/`INACTIVE`/`OUT_OF_STOCK`) existe na entidade `Product`, mas
  hoje não é atualizado automaticamente a partir de nenhum evento de estoque, porque o Inventory
  Service ainda não foi criado.
- MinIO não está incluído no `infra/docker-compose.yml` do projeto — só existe hoje via
  Testcontainers nos testes de aceitação; para rodar o serviço manualmente fora dos testes é
  necessário subir um MinIO local à parte.
- Categoria é lista plana (sem hierarquia pai/filho) — divergência assumida frente ao PRD
  original, documentada no DER e na spec do serviço.
