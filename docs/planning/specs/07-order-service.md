# Order Service

**Build Tool:** Maven | **Arquitetura:** Hexagonal (Ports & Adapters) | **Porta:** 8007 | **Status:** Planejado

**DER:** [docs/planning/der/order-service.mmd](../der/order-service.mmd)

## Objetivo

Gerenciar o ciclo de vida completo de pedidos com máquina de estados. A arquitetura Hexagonal isola o domínio de pedidos dos detalhes de infraestrutura (REST, Kafka, JPA), tornando a lógica de negócio testável de forma independente.

## PRD Resumido

Sem um agregado transacional que orquestre carrinho, endereço, estoque e pagamento, não haveria um
registro único e auditável do que foi comprado, por quem, e em qual estado do fluxo de compra. O
Order Service resolve isso mantendo a máquina de estados do pedido e coordenando (via Kafka) a
Saga entre Inventory Service e Payment Service. Usado diretamente pelo cliente final autenticado
(criar pedido, consultar histórico) e depende do Cart Service e User Service via OpenFeign. Valor
de negócio: é o registro de verdade da venda, garante que nenhum pedido seja confirmado sem
reserva de estoque e pagamento aprovado, e sustenta o rastreamento completo do fluxo de compra.

## Use Cases

- Como cliente autenticado, quero criar um pedido a partir do meu carrinho e de um endereço de
  entrega, para iniciar o processo de compra.
- Como cliente autenticado, quero consultar meu histórico de pedidos, para acompanhar compras
  anteriores.
- Como cliente autenticado, quero consultar os detalhes de um pedido específico, para ver itens,
  status e valor total.
- Como Order Service, quero publicar `order.created` ao criar um pedido, para que o Inventory
  Service tente reservar o estoque dos itens.
- Como Order Service, quero transicionar o pedido para `PAID` ao consumir `payment.approved`, para
  refletir que o pagamento foi confirmado.
- Como Order Service, quero transicionar o pedido para `CANCELLED` ao consumir `payment.rejected`
  ou `inventory.stock-insufficient`, para interromper um pedido que não pode ser concluído
  (compensação Saga).
- Como Order Service, quero manter histórico de todas as transições de status, para auditoria e
  suporte ao cliente.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Criação de pedido com sucesso | Carrinho do usuário com itens e endereço (`addressId`) válido no User Service | Cliente chama `POST /api/v1/orders` | Serviço busca o endereço via `UserServicePort`, grava snapshot imutável em `order_delivery_addresses`, cria pedido em `PENDING` referenciando esse snapshot (`delivery_address_id`), transiciona para `WAITING_PAYMENT`, publica `order.created` no Kafka e limpa o carrinho via Cart Service |
| Criação sem itens no carrinho | Carrinho do usuário está vazio | Cliente chama `POST /api/v1/orders` | Serviço retorna erro de validação (4xx) sem criar pedido nem publicar evento |
| Confirmação de pagamento aprovado | Pedido em `WAITING_PAYMENT` | Serviço consome `payment.approved` para esse `orderId` | Pedido transiciona para `PAID` e nova entrada é registrada em `order_status_history` |
| Cancelamento por pagamento rejeitado | Pedido em `WAITING_PAYMENT` | Serviço consome `payment.rejected` para esse `orderId` | Pedido transiciona para `CANCELLED` e nova entrada é registrada em `order_status_history` |
| Cancelamento por estoque insuficiente | Pedido em `WAITING_PAYMENT` | Serviço consome `inventory.stock-insufficient` para esse `orderId` | Pedido transiciona para `CANCELLED` sem que pagamento tenha sido cobrado (compensação Saga) |
| Idempotência de evento duplicado | Pedido já está em `PAID` | Serviço consome um segundo `payment.approved` para o mesmo `orderId` | Pedido permanece em `PAID`, sem nova entrada duplicada de transição inválida |
| Consulta de histórico | Usuário autenticado com pedidos anteriores | Cliente chama `GET /api/v1/orders` | Serviço retorna apenas os pedidos pertencentes a esse `userId` |

## Banco de Dados: PostgreSQL (`order_db`)

## Responsabilidades

- Criar pedido a partir do carrinho (lê via port Cart, limpa após criação)
- Validar endereço de entrega (lê via port User, buscando pelo `addressId` informado) e gravar
  snapshot imutável dos campos em `order_delivery_addresses` — pedido referencia esse snapshot via
  `delivery_address_id` (FK), nunca o `addressId` original do User Service; edição/exclusão
  posterior do endereço no User Service não altera o histórico do pedido
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
<!-- <project> -->
<version>1.0.0</version>

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
CREATE TABLE order_delivery_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(9) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    delivery_address_id UUID NOT NULL REFERENCES order_delivery_addresses(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(10,2) NOT NULL,
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
│   │   ├── Order.java                          # Aggregate root — referencia deliveryAddressId
│   │   ├── OrderItem.java                      # Entidade de item
│   │   ├── OrderDeliveryAddress.java           # Entidade — snapshot imutável, tabela própria
│   │   └── OrderStatus.java                    # Enum da máquina de estados
│   ├── service/
│   │   └── OrderDomainService.java             # Lógica da máquina de estados
│   └── port/
│       ├── in/
│       │   ├── CreateOrderUseCase.java         # Driving port: criar pedido
│       │   ├── GetOrderUseCase.java            # Driving port: consultar pedido
│       │   └── UpdateOrderStatusUseCase.java   # Driving port: atualizar status
│       └── out/
│           ├── OrderRepository.java            # Driven port: persistência
│           ├── OrderDeliveryAddressRepository.java # Driven port: persistência do snapshot de endereço
│           ├── OrderEventPublisher.java        # Driven port: publicar eventos
│           ├── CartServicePort.java            # Driven port: ler/limpar carrinho
│           └── UserServicePort.java            # Driven port: buscar endereço original (addressId) para snapshot
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
        │   ├── OrderDeliveryAddressJpaEntity.java # @Entity JPA — tabela order_delivery_addresses
        │   ├── OrderJpaRepository.java         # Implementa OrderRepository
        │   └── OrderDeliveryAddressJpaRepository.java # Implementa OrderDeliveryAddressRepository
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
CART_SERVICE_URL=http://cart-service:8005
USER_SERVICE_URL=http://user-service:8003
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
COPY target/order-service.jar app.jar
EXPOSE 8007
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
        <includeTestSourceDirectory>true</includeTestSourceDirectory>
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

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/order-service.md`.
