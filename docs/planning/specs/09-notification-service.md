# Notification Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8009 | **Status:** Planejado

## Objetivo

Consumir eventos Kafka e enviar e-mails com templates Thymeleaf. Completamente stateless — sem banco de dados. Retry automático com backoff exponencial e Dead Letter Topic para falhas persistentes.

## Banco de Dados: Nenhum (stateless)

## Responsabilidades

- Consumir 5 tópicos Kafka e disparar e-mail para cada evento
- Templates HTML com Thymeleaf para cada tipo de comunicação
- Retry automático (3 tentativas com backoff exponencial 1s → 2s → 4s)
- Enviar mensagens falhadas para Dead Letter Topic (`.DLT`)
- Integração com MailHog em dev, SMTP real em produção

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Kafka | 3.x | Consumer de eventos |
| JavaMailSender | Spring Boot Starter Mail | Envio SMTP |
| Thymeleaf | 3.x | Templates HTML de e-mail |
| Springdoc OpenAPI | 2.x | Swagger (apenas health) |
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
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}
```

## Eventos Consumidos e Templates

| Tópico | Consumer Group | Template | Assunto |
|---|---|---|---|
| `user.created` | `notification-user-group` | `welcome.html` | Bem-vindo ao AutoHubStore! |
| `auth.password-reset` | `notification-auth-group` | `password-reset.html` | Redefinição de senha |
| `order.created` | `notification-order-group` | `order-confirmed.html` | Pedido confirmado |
| `payment.approved` | `notification-payment-group` | `payment-approved.html` | Pagamento aprovado |
| `payment.rejected` | `notification-payment-group` | `payment-rejected.html` | Pagamento recusado |

## Templates Thymeleaf

Local: `src/main/resources/templates/email/`

```
email/
├── welcome.html           # Boas-vindas com nome do usuário
├── password-reset.html    # Link de reset com token (TTL 15min)
├── order-confirmed.html   # Lista de itens + total do pedido
├── payment-approved.html  # Confirmação do pagamento + número do pedido
└── payment-rejected.html  # Motivo da rejeição + orientações
```

Cada template recebe um `Map<String, Object>` com os dados do evento via `Context` do Thymeleaf.

## Configuração de Retry e DLT

```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),
    dltTopicSuffix = ".DLT",
    dltStrategy = DltStrategy.FAIL_ON_ERROR
)
@KafkaListener(topics = "user.created", groupId = "notification-user-group")
public void onUserCreated(UserCreatedEvent event) {
    emailService.send(event.email(), "welcome", Map.of("name", event.fullName()));
}
```

DLTs criados automaticamente: `user.created.DLT`, `auth.password-reset.DLT`, etc.

## Estrutura de Pacotes (MVC)

```
com.autohubstore.notificationservice/
├── controller/
│   └── HealthController.java              # Apenas para documentação Swagger
├── service/
│   └── EmailService.java                  # JavaMailSender + Thymeleaf rendering
├── messaging/
│   ├── UserEventConsumer.java             # @KafkaListener user.created
│   ├── AuthEventConsumer.java             # @KafkaListener auth.password-reset
│   ├── OrderEventConsumer.java            # @KafkaListener order.created
│   └── PaymentEventConsumer.java          # @KafkaListener payment.approved + payment.rejected
├── model/
│   ├── UserCreatedEvent.java              # Record (Java) para deserialização
│   ├── PasswordResetEvent.java
│   ├── OrderCreatedEvent.java
│   ├── PaymentApprovedEvent.java
│   └── PaymentRejectedEvent.java
└── config/
    ├── KafkaConsumerConfig.java
    └── MailConfig.java
```

## Variáveis de Ambiente

```
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_FROM=noreply@autohubstore.com
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false
# Produção:
# MAIL_HOST=smtp.sendgrid.net
# MAIL_PORT=587
# MAIL_SMTP_AUTH=true
# MAIL_SMTP_STARTTLS=true
# MAIL_USERNAME=apikey
# MAIL_PASSWORD=<sendgrid-api-key>
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY build/libs/notification-service.jar app.jar
EXPOSE 8009
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

- **Unitários:** `EmailService` com mock do `JavaMailSender`; verificar template correto selecionado por tipo de evento
- **Integração:** Testcontainers Kafka; publicar evento → verificar `EmailService.send()` chamado com dados corretos
- **DLT:** Testar que após 3 falhas de envio (exception lançada) a mensagem vai para o DLT
- **Templates:** Testar rendering Thymeleaf com dados de exemplo → verificar conteúdo HTML gerado
