# Inventory Service

**Build Tool:** Maven | **Arquitetura:** Hexagonal (Ports & Adapters) | **Porta:** 8006 | **Status:** Planejado

## Objetivo

Controlar estoque de produtos com semântica de reserva atômica, evitando overselling em checkout
concorrente. Extraído do Catalog Service — estoque é um recurso de escrita contenciosa que exige
forte consistência e controle de concorrência, incompatível com o papel de leitura/cache do
Catalog. A arquitetura Hexagonal isola a máquina de estados de reserva dos detalhes de
infraestrutura (REST, Kafka, JPA), assim como o Order Service. Ver decisão em
[docs/planning/action-plan.md](../action-plan.md#decisões-de-consolidação).

## Banco de Dados: PostgreSQL (`inventory_db`)

## Responsabilidades

- Manter `stock_quantity` por `productId` (dono exclusivo do dado)
- Reservar estoque ao consumir `order.created` (decrementa disponível; se insuficiente, publica
  `inventory.stock-insufficient` para o Order Service cancelar o pedido — padrão Saga)
- Confirmar reserva (dedução definitiva) ao consumir `payment.approved`
- Liberar reserva (repõe estoque) ao consumir `payment.rejected` ou cancelamento de pedido
- Expor consulta de disponibilidade para Catalog Service e Cart Service
- Ajuste manual de estoque via endpoint admin (entrada de mercadoria, correção)

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data JPA | 3.x | PostgreSQL |
| Flyway | 9+ | Migrações |
| Spring Kafka | 3.x | Producer + Consumer |
| Bean Validation | Jakarta | Validação de entrada |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Maven (pom.xml)

```xml
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
GET  /api/v1/inventory/{productId}              # Consultar disponibilidade (público)
PUT  /api/v1/inventory/{productId}               # Ajustar estoque (ADMIN) { quantity }
```

## Schema do Banco (Flyway)

### V1__create_inventory_schema.sql

```sql
CREATE TABLE stock_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE,
    available_quantity INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_stock_items_product_id ON stock_items(product_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
```

## Máquina de Estados da Reserva

```
RESERVED
  ├─→ CONFIRMED   (ao consumir payment.approved — dedução definitiva)
  └─→ RELEASED    (ao consumir payment.rejected / order cancelado — repõe available_quantity)
```

`RESERVED` sem estoque suficiente não é criado — publica `inventory.stock-insufficient` no lugar.

## Eventos Kafka

### Consumidos

- `order.created` → tenta reservar cada item; sucesso não publica nada (silencioso); falha publica `inventory.stock-insufficient`
- `payment.approved` → confirma reservas do pedido (`RESERVED → CONFIRMED`)
- `payment.rejected` → libera reservas do pedido (`RESERVED → RELEASED`)

### Publicado — `inventory.stock-insufficient`

```json
{
  "orderId": "uuid",
  "productId": "uuid",
  "requestedQuantity": 3,
  "availableQuantity": 1,
  "occurredAt": "2024-01-01T10:00:00Z"
}
```

Consumido pelo **Order Service** → transiciona pedido para `CANCELLED` (compensação Saga).

## Estrutura de Pacotes (Hexagonal — Ports & Adapters)

```
com.autohubstore.inventoryservice/
├── domain/
│   ├── model/
│   │   ├── StockItem.java                        # Aggregate root
│   │   ├── StockReservation.java                 # Entidade
│   │   └── ReservationStatus.java                # Enum da máquina de estados
│   ├── service/
│   │   └── InventoryDomainService.java           # Lógica de reserva/confirmação/liberação
│   └── port/
│       ├── in/
│       │   ├── ReserveStockUseCase.java          # Driving port
│       │   ├── ConfirmReservationUseCase.java    # Driving port
│       │   ├── ReleaseReservationUseCase.java     # Driving port
│       │   └── GetStockUseCase.java              # Driving port
│       └── out/
│           ├── StockItemRepository.java           # Driven port: persistência
│           ├── StockReservationRepository.java     # Driven port: persistência
│           └── InventoryEventPublisher.java        # Driven port: publicar eventos
└── adapter/
    ├── in/
    │   ├── web/
    │   │   └── InventoryController.java           # @RestController → chama use cases
    │   └── messaging/
    │       ├── OrderEventConsumer.java             # @KafkaListener order.created
    │       └── PaymentEventConsumer.java           # @KafkaListener payment.approved/rejected
    └── out/
        ├── persistence/
        │   ├── StockItemJpaEntity.java             # @Entity JPA
        │   ├── StockReservationJpaEntity.java       # @Entity JPA
        │   └── StockItemJpaRepository.java          # Implementa StockItemRepository
        └── messaging/
            └── InventoryKafkaPublisher.java         # Implementa InventoryEventPublisher
```

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-inventory:5437/inventory_db
DB_USERNAME=inventory_user
DB_PASSWORD=<secret>
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_GROUP_ID=inventory-service-group
```

> **Infra:** container `postgres-inventory` (porta `5437`) já disponível em `infra/docker-compose.yml`.

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/inventory-service.jar app.jar
EXPOSE 8006
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
        <includeTestSourceDirectory>false</includeTestSourceDirectory>
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

- **Unitários:** `InventoryDomainService` (reserva com estoque suficiente/insuficiente, confirmação, liberação) — testável sem Spring context (apenas mocks das portas)
- **Integração:** Testcontainers (PostgreSQL + Kafka); publicar `order.created` → verificar reserva criada e `available_quantity` decrementado; publicar `payment.approved` → reserva `CONFIRMED`; publicar `payment.rejected` → estoque reposto
- **Concorrência:** Duas reservas simultâneas para o último item em estoque → apenas uma reserva bem-sucedida (lock otimista/`SELECT FOR UPDATE`)
- **Saga:** Reserva insuficiente → `inventory.stock-insufficient` publicado com dados corretos
