# Analytics Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8010

## Objetivo

Coletar e agregar eventos de visualização e compra em Cassandra para expor ranking de produtos e dashboard administrativo com métricas consolidadas.

## Banco de Dados: Cassandra (keyspace `autohubstore_analytics`)

## Responsabilidades

- Consumir `catalog.product-viewed` → incrementar contador de visualizações
- Consumir `order.created` → incrementar contadores de vendas e receita por produto
- Agregar por dois períodos simultaneamente: diário (`2024-01-15`) e mensal (`2024-01`)
- Expor ranking de produtos mais vistos e mais vendidos por período
- Endpoint de dashboard administrativo com métricas consolidadas

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data Cassandra | 4.x | Acesso ao Cassandra (COUNTER) |
| Spring Kafka | 3.x | Consumer de eventos |
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
    implementation 'org.springframework.boot:spring-boot-starter-data-cassandra'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:cassandra'
    testImplementation 'org.testcontainers:kafka'
}
```

## Endpoints

```
GET /api/v1/analytics/products/most-viewed?period={period}&limit={n}
GET /api/v1/analytics/products/most-sold?period={period}&limit={n}
GET /api/v1/analytics/dashboard
```

**Parâmetros:**

| Parâmetro | Formato | Exemplos |
|---|---|---|
| `period` | Mensal: `YYYY-MM` / Diário: `YYYY-MM-DD` | `2024-01`, `2024-01-15` |
| `limit` | int (default 10, max 50) | `5`, `10` |

**Resposta GET /dashboard:**

```json
{
  "period": "2024-01",
  "mostViewedProducts": [
    { "productId": "uuid", "productName": "Filtro de Ar K&N", "viewCount": 1523 }
  ],
  "mostSoldProducts": [
    { "productId": "uuid", "productName": "Filtro de Ar K&N", "saleCount": 87, "revenue": 26073.00 }
  ]
}
```

## Schema Cassandra

```cql
CREATE KEYSPACE autohubstore_analytics
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE autohubstore_analytics;

-- Produtos mais visualizados por período
CREATE TABLE product_views_by_period (
    period       TEXT,
    product_id   UUID,
    product_name TEXT STATIC,
    view_count   COUNTER,
    PRIMARY KEY (period, product_id)
) WITH CLUSTERING ORDER BY (product_id ASC);

-- Produtos mais vendidos por período
CREATE TABLE product_sales_by_period (
    period         TEXT,
    product_id     UUID,
    product_name   TEXT STATIC,
    sale_count     COUNTER,
    revenue_cents  COUNTER,
    PRIMARY KEY (period, product_id)
);
```

> `revenue_cents` armazena o valor em centavos (inteiro) para usar COUNTER do Cassandra. Dividir por 100 ao expor na API.

## Kafka — Tópicos Consumidos

| Tópico | Consumer Group | Ação |
|---|---|---|
| `catalog.product-viewed` | `analytics-views-group` | Incrementar `view_count` (diário + mensal) |
| `order.created` | `analytics-orders-group` | Incrementar `sale_count` e `revenue_cents` para cada item |

## Lógica de Período

```java
// AnalyticsService.java
private void recordView(String productId, String productName, Instant viewedAt) {
    LocalDate date = viewedAt.atZone(ZoneOffset.UTC).toLocalDate();
    String daily   = date.toString();                           // "2024-01-15"
    String monthly = YearMonth.from(date).toString();           // "2024-01"

    // Incrementar COUNTER em ambas as linhas
    viewsRepository.incrementViewCount(daily, productId, productName);
    viewsRepository.incrementViewCount(monthly, productId, productName);
}
```

## Estrutura de Pacotes (MVC)

```
com.autohubstore.analyticsservice/
├── controller/
│   └── AnalyticsController.java              # GET most-viewed, most-sold, dashboard
├── service/
│   └── AnalyticsService.java                 # Lógica de período, agregação
├── repository/
│   ├── ProductViewsRepository.java           # CassandraRepository + UPDATE COUNTER
│   └── ProductSalesRepository.java           # CassandraRepository + UPDATE COUNTER
├── model/
│   ├── ProductViewsByPeriod.java             # @Table("product_views_by_period")
│   ├── ProductSalesByPeriod.java             # @Table("product_sales_by_period")
│   └── DashboardResponse.java               # DTO resposta do dashboard
├── messaging/
│   ├── ProductViewedConsumer.java            # @KafkaListener catalog.product-viewed
│   └── OrderCreatedConsumer.java             # @KafkaListener order.created
├── exception/
│   └── GlobalExceptionHandler.java           # @ControllerAdvice
└── config/
    ├── CassandraConfig.java                  # CqlSession + keyspace config
    └── KafkaConsumerConfig.java
```

## Variáveis de Ambiente

```
CASSANDRA_CONTACT_POINTS=cassandra
CASSANDRA_PORT=9042
CASSANDRA_LOCAL_DATACENTER=datacenter1
CASSANDRA_KEYSPACE=autohubstore_analytics
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_GROUP_ID_VIEWS=analytics-views-group
KAFKA_GROUP_ID_ORDERS=analytics-orders-group
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY build/libs/analytics-service.jar app.jar
EXPOSE 8010
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Estratégia de Testes

- **Unitários:** `AnalyticsService` (cálculo correto de período diário e mensal a partir de timestamp), montagem do `DashboardResponse`
- **Integração:** Testcontainers (Cassandra + Kafka); publicar `product-viewed` → verificar COUNTER incrementado nas duas linhas (diária e mensal)
- **Order:** Publicar `order.created` com múltiplos itens → verificar que cada produto teve `sale_count` e `revenue_cents` incrementados
