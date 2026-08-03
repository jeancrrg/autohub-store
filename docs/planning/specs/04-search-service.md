# Search Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8004 | **Status:** Planejado

## Objetivo

Busca textual full-text de produtos com filtros. Mantém o índice Elasticsearch sincronizado consumindo eventos Kafka do Catalog Service.

## Banco de Dados: Elasticsearch (índice `products`)

## Responsabilidades

- Busca textual com analyzer português
- Filtros: categoria, faixa de preço, disponibilidade (`status = ACTIVE`)
- Resultados paginados com total de registros
- Re-indexação automática ao consumir `catalog.product-created` e `catalog.product-updated`

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data Elasticsearch | 5.x | Índice e queries |
| Spring Kafka | 3.x | Consumer de eventos do Catalog |
| Bean Validation | Jakarta | Validação de parâmetros |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Gradle (build.gradle)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:elasticsearch'
    testImplementation 'org.testcontainers:kafka'
}
```

## Endpoints

```
GET /api/v1/search?q={query}&category={uuid}&minPrice={n}&maxPrice={n}&page={n}&size={n}
```

**Parâmetros:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `q` | string | Sim | Termo de busca |
| `category` | UUID | Não | Filtrar por categoria |
| `minPrice` | decimal | Não | Preço mínimo |
| `maxPrice` | decimal | Não | Preço máximo |
| `page` | int | Não | Página (default 0) |
| `size` | int | Não | Tamanho (default 10, max 50) |

**Resposta:**

```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Filtro de Ar K&N",
      "price": 299.90,
      "categoryId": "uuid",
      "categoryName": "Filtros",
      "status": "ACTIVE"
    }
  ],
  "totalElements": 42,
  "totalPages": 5,
  "page": 0,
  "size": 10
}
```

## Mapeamento do Índice Elasticsearch

```json
{
  "mappings": {
    "properties": {
      "id":            { "type": "keyword" },
      "name":          { "type": "text", "analyzer": "portuguese" },
      "description":   { "type": "text", "analyzer": "portuguese" },
      "price":         { "type": "float" },
      "categoryId":    { "type": "keyword" },
      "categoryName":  { "type": "keyword" },
      "status":        { "type": "keyword" },
      "createdAt":     { "type": "date" }
    }
  }
}
```

## Kafka — Tópicos Consumidos

| Tópico | Consumer Group | Ação |
|---|---|---|
| `catalog.product-created` | `search-service-group` | Indexar novo produto |
| `catalog.product-updated` | `search-service-group` | Re-indexar produto |

## Estrutura de Pacotes (MVC)

```
com.autohubstore.searchservice/
├── controller/
│   └── SearchController.java               # GET /api/v1/search
├── service/
│   └── SearchService.java                  # Monta query Elasticsearch com filtros
├── repository/
│   └── ProductSearchRepository.java        # ElasticsearchRepository<ProductDocument, String>
├── model/
│   ├── ProductDocument.java                # @Document(indexName = "products")
│   ├── SearchRequest.java                  # Parâmetros de busca
│   └── SearchResponse.java                 # Resposta paginada
├── messaging/
│   └── ProductIndexingConsumer.java        # @KafkaListener para product-created/updated
├── exception/
│   └── GlobalExceptionHandler.java         # @ControllerAdvice
└── config/
    ├── ElasticsearchConfig.java             # ElasticsearchClient + IndexCreator
    └── KafkaConsumerConfig.java
```

## Variáveis de Ambiente

```
ELASTICSEARCH_HOST=elasticsearch
ELASTICSEARCH_PORT=9200
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_GROUP_ID=search-service-group
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY build/libs/search-service.jar app.jar
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
    sourceSets = [sourceSets.main] // não aplica nos testes
}
```

## Estratégia de Testes

- **Unitários:** `SearchService` (construção de query com cada combinação de filtros)
- **Integração:** Testcontainers (Elasticsearch + Kafka); publicar `product-created` → buscar produto indexado
- **Busca:** Testar analyzer português (stemming), filtro de preço, filtro de categoria
