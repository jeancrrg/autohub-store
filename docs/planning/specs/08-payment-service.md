# Payment Service

**Build Tool:** Maven | **Arquitetura:** MVC | **Porta:** 8008 | **Status:** Planejado

**DER:** [docs/planning/der/payment-service.mmd](../der/payment-service.mmd)

## Objetivo

Simular processamento de pagamentos com distribuição 70% aprovado / 30% rejeitado. Garante idempotência por `orderId` e publica o resultado via eventos Kafka para o Order Service e Notification Service.

## PRD Resumido

Sem um serviço próprio de pagamento, a lógica de aprovação/rejeição ficaria misturada ao domínio de
pedidos, dificultando trocar futuramente a simulação por um gateway de pagamento real. O Payment
Service resolve isso isolando a decisão de aprovação/rejeição e garantindo que o mesmo pedido nunca
seja processado duas vezes simultaneamente. Não é usado diretamente pelo cliente final — é acionado
pelo Order Service (indiretamente, via fluxo de checkout) e seu resultado é consumido por Order
Service e Notification Service via Kafka. Valor de negócio: sustenta o fluxo de aprovação/rejeição
que decide se o pedido segue para `PAID` ou `CANCELLED`, com garantia de idempotência que evita
cobrança duplicada.

## Use Cases

- Como Order Service (em nome do cliente final), quero iniciar uma tentativa de pagamento vinculada
  a um pedido, para decidir se a compra será aprovada ou rejeitada.
- Como cliente final (indiretamente), quero que meu pagamento seja processado uma única vez por
  pedido, para nunca ser cobrado em duplicidade.
- Como Order Service, quero ser avisado do resultado do pagamento via evento Kafka
  (`payment.approved`/`payment.rejected`), para transicionar o status do pedido corretamente.
- Como Notification Service, quero ser avisado do resultado do pagamento, para enviar o e-mail de
  confirmação ou rejeição ao cliente.
- Como suporte/administrador, quero consultar o histórico de pagamentos de um pedido, para
  investigar divergências ou reclamações.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Pagamento aprovado | `approvalRate` configurado como `1.0` (determinístico para teste) | Cliente (via Order Service) chama `POST /api/v1/payments` com `orderId`, `userId` e `amount` | Serviço cria pagamento com status `APPROVED`, publica `payment.approved` com os dados corretos |
| Pagamento rejeitado | `approvalRate` configurado como `0.0` (determinístico para teste) | Cliente chama `POST /api/v1/payments` | Serviço cria pagamento com status `REJECTED` e `rejectionReason` preenchido, publica `payment.rejected` |
| Idempotência de pedido duplicado | Já existe pagamento `PENDING` ou `APPROVED` para o mesmo `orderId` | Cliente chama `POST /api/v1/payments` novamente com o mesmo `orderId` | Serviço retorna 409 Conflict, sem criar segundo registro nem publicar novo evento |
| Consulta de pagamento por ID | Pagamento já processado existe | Cliente chama `GET /api/v1/payments/{id}` | Serviço retorna os dados do pagamento, incluindo status e `processedAt` |
| Histórico por pedido | Pedido possui uma ou mais tentativas de pagamento | Cliente chama `GET /api/v1/payments/order/{orderId}` | Serviço retorna todas as tentativas de pagamento vinculadas a esse `orderId` |

## Banco de Dados: PostgreSQL (`payment_db`)

## Responsabilidades

- Criar tentativa de pagamento vinculada a um pedido
- Simular aprovação (70%) ou rejeição (30%)
- Garantir idempotência: mesmo `orderId` não processa dois pagamentos simultâneos
- Publicar `payment.approved` ou `payment.rejected` no Kafka
- Expor histórico de pagamentos por pedido

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data JPA | 3.x | PostgreSQL |
| Flyway | 9+ | Migrações |
| Spring Kafka | 3.x | Producer de resultados |
| Bean Validation | Jakarta | Validação de entrada |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração |

## Dependências Maven (pom.xml)

```xml
<!-- <project> -->
<version>1.0.0</version>

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
POST /api/v1/payments                    # Iniciar pagamento { orderId, userId, amount }
GET  /api/v1/payments/{id}               # Consultar pagamento por ID
GET  /api/v1/payments/order/{orderId}    # Histórico de pagamentos de um pedido
```

## Schema do Banco (Flyway)

### V1__create_payments_schema.sql

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Idempotência: impede dois pagamentos ativos para o mesmo pedido
CREATE UNIQUE INDEX idx_payments_order_unique ON payments(order_id)
    WHERE status IN ('PENDING', 'APPROVED');

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
```

## Lógica de Simulação

```java
// PaymentSimulatorService.java
@Service
public class PaymentSimulatorService {

    @Value("${payment.approval-rate:0.70}")
    private double approvalRate;

    public PaymentStatus simulate() {
        return Math.random() <= approvalRate
            ? PaymentStatus.APPROVED
            : PaymentStatus.REJECTED;
    }
}
```

Para testes determinísticos, injetar `approvalRate = 1.0` (sempre aprova) ou `0.0` (sempre rejeita).

## Eventos Kafka Publicados

**`payment.approved`:**

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "amount": 599.80,
  "processedAt": "2024-01-01T10:00:01Z"
}
```

**`payment.rejected`:**

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "amount": 599.80,
  "reason": "Insufficient funds (simulated)",
  "processedAt": "2024-01-01T10:00:01Z"
}
```

## Estrutura de Pacotes (MVC)

```
com.autohubstore.paymentservice/
├── controller/
│   └── PaymentController.java              # POST + GET payments
├── service/
│   ├── PaymentService.java                 # Orquestra criação, simulação e publicação
│   └── PaymentSimulatorService.java        # Lógica de simulação 70/30
├── repository/
│   └── PaymentRepository.java              # JpaRepository<Payment, UUID>
├── model/
│   ├── Payment.java                        # @Entity
│   ├── PaymentStatus.java                  # Enum: PENDING, APPROVED, REJECTED
│   ├── CreatePaymentRequest.java           # DTO entrada
│   └── PaymentResponse.java                # DTO saída
├── messaging/
│   └── PaymentEventPublisher.java          # KafkaTemplate producer
├── exception/
│   └── GlobalExceptionHandler.java         # @ControllerAdvice (409 para duplicado)
└── config/
    └── KafkaProducerConfig.java
```

## Variáveis de Ambiente

```
DB_URL=jdbc:postgresql://postgres-payment:5436/payment_db
DB_USERNAME=payment_user
DB_PASSWORD=<secret>
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
PAYMENT_APPROVAL_RATE=0.70
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
COPY target/payment-service.jar app.jar
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

- **Unitários:** `PaymentSimulatorService` com taxa fixada via `@Value` (resultado determinístico); `PaymentService` verificando publicação do evento correto
- **Integração:** Testcontainers (PostgreSQL + Kafka); fluxo completo POST → evento publicado
- **Idempotência:** Segunda chamada com mesmo `orderId` → HTTP 409 Conflict
- **Aprovação/Rejeição:** Testar ambos os caminhos com taxa `1.0` e `0.0` respectivamente

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/payment-service.md`.
