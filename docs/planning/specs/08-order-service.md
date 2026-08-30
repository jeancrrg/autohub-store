# Order Service

**Build Tool:** Maven | **Arquitetura:** Hexagonal (Ports & Adapters) | **Porta:** 8008 | **Status:** Planejado

## Objetivo

Gerenciar o ciclo de vida completo de pedidos com máquina de estados. A arquitetura Hexagonal isola o domínio de pedidos dos detalhes de infraestrutura (REST, Kafka, JPA), tornando a lógica de negócio testável de forma independente.

## Banco de Dados: PostgreSQL (`order_db`)

## Responsabilidades

- Criar pedido a partir do carrinho (lê via port Cart, limpa após criação)
- Validar endereço de entrega (lê via port User)
- Máquina de estados: `PENDING → WAITING_PAYMENT → PAID → CANCELLED`
- Publicar `order.created` no Kafka (driven port out) — consumido pelo Inventory Service para reserva de estoque
- Consumir `payment.approved` → PAID (driving port in via Kafka)
- Consumir `payment.rejected` → CANCELLED (driving port in via Kafka)
- Consumir `inventory.stock-insufficient` → CANCELLED (compensação Saga — estoque insuficiente na reserva)
- Manter histórico de todas as transições de status

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data JPA | 3.x | PostgreSQL |
| Flyway | 9+ | Migrações |
| Spring Kafka | 3.x | Producer + Consumer |
| OpenFeign | Spring Cloud | Cart Service + User Service |
| Resilience4j | 2.x | Circuit Breaker |
| Bean Validation | Jakarta | Validação de entrada |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Maven (pom.xml)

```xml
<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
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
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
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

## Endpoints

Todos requerem JWT.

```
POST /api/v1/orders          # Criar pedido { addressId }
GET  /api/v1/orders          # Histórico do usuário autenticado
GET  /api/v1/orders/{id}     # Detalhes de um pedido
```

## Schema do Banco (Flyway)

### V1__create_orders_schema.sql

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(10,2) NOT NULL,
    delivery_street VARCHAR(255),
    delivery_number VARCHAR(20),
    delivery_city VARCHAR(100),
    delivery_state VARCHAR(2),
    delivery_zip_code VARCHAR(9),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal NUMERIC(10,2) GENERATED ALWAYS AS (unit_price * quantity) STORED
);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    note TEXT
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
```

## Máquina de Estados

```
PENDING
  └─→ WAITING_PAYMENT   (ao publicar order.created no Kafka)
        ├─→ PAID         (ao consumir payment.approved)
        └─→ CANCELLED    (ao consumir payment.rejected OU inventory.stock-insufficient)
```

Cada transição registra entrada em `order_status_history`.

## Eventos Kafka

### Publicado — `order.created`

```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "productName": "Filtro de Ar K&N",
      "unitPrice": 299.90,
      "quantity": 2
    }
  ],
  "totalAmount": 599.80,
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Consumidos

- `payment.approved` → transiciona pedido para `PAID`
- `payment.rejected` → transiciona pedido para `CANCELLED`
- `inventory.stock-insufficient` → transiciona pedido para `CANCELLED` (compensação Saga)

## Estrutura de Pacotes (Hexagonal — Ports & Adapters)

```
com.autohubstore.orderservice/
├── domain/
│   ├── model/
│   │   ├── Order.java                          # Aggregate root
│   │   ├── OrderItem.java                      # Entidade de item
│   │   ├── OrderStatus.java                    # Enum da máquina de estados
│   │   └── DeliveryAddress.java               # Value Object (snapshot)
│   ├── service/
│   │   └── OrderDomainService.java             # Lógica da máquina de estados
│   └── port/
│       ├── in/
│       │   ├── CreateOrderUseCase.java         # Driving port: criar pedido
│       │   ├── GetOrderUseCase.java            # Driving port: consultar pedido
│       │   └── UpdateOrderStatusUseCase.java   # Driving port: atualizar status
│       └── out/
│           ├── OrderRepository.java            # Driven port: persistência
│           ├── OrderEventPublisher.java        # Driven port: publicar eventos
│           ├── CartServicePort.java            # Driven port: ler/limpar carrinho
│           └── UserServicePort.java            # Driven port: buscar endereço
└── adapter/
    ├── in/
    │   ├── web/
    │   │   └── OrderController.java            # @RestController → chama use cases
    │   └── messaging/
    │       ├── PaymentEventConsumer.java       # @KafkaListener payment.approved/rejected → chama UpdateOrderStatusUseCase
    │       └── InventoryEventConsumer.java     # @KafkaListener inventory.stock-insufficient → chama UpdateOrderStatusUseCase
    └── out/
        ├── persistence/
        │   ├── OrderJpaEntity.java             # @Entity JPA
        │   ├── OrderItemJpaEntity.java
        │   └── OrderJpaRepository.java         # Implementa OrderRepository
        ├── messaging/
        │   └── OrderKafkaPublisher.java        # Implementa OrderEventPublisher
        └── external/
            ├── CartServiceFeignAdapter.java    # Implementa CartServicePort
            └── UserServiceFeignAdapter.java    # Implementa UserServicePort
```

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-order:5435/order_db
DB_USERNAME=order_user
DB_PASSWORD=<secret>
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_GROUP_ID=order-service-group
CART_SERVICE_URL=http://cart-service:8006
USER_SERVICE_URL=http://user-service:8003
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY target/order-service.jar app.jar
EXPOSE 8008
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

- **Unitários:** `OrderDomainService` (todas as transições válidas e inválidas da máquina de estados); `CreateOrderUseCase` com mocks das portas de saída
- **Integração:** Testcontainers (PostgreSQL + Kafka); criar pedido → publicar `order.created` → consumir `payment.approved` → verificar status PAID
- **Idempotência:** Consumir dois `payment.approved` para o mesmo orderId deve ser idempotente
- **Hexagonal:** Os use cases devem ser testáveis sem Spring context (apenas com mocks das portas)
