# Notification Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8009 | **Status:** Planejado

**DER:** [docs/planning/der/notification-service.mmd](../der/notification-service.mmd) (sem banco
próprio — diagrama documenta os records de evento Kafka consumidos)

## Objetivo

Consumir eventos Kafka e enviar e-mails com templates Thymeleaf. Completamente stateless — sem banco de dados. Retry automático com backoff exponencial e Dead Letter Topic para falhas persistentes.

## PRD Resumido

Sem um serviço dedicado de notificação, cada microsserviço precisaria implementar seu próprio envio
de e-mail, duplicando lógica de template e configuração de SMTP. O Notification Service resolve
isso consumindo os eventos de domínio relevantes (cadastro, reset de senha, pedido, pagamento) e
centralizando o envio de e-mail com retry e Dead Letter Topic. Não é chamado diretamente pelo
cliente final nem por outro serviço via API — reage exclusivamente a eventos Kafka publicados por
User Service, Auth Service, Order Service e Payment Service. Valor de negócio: garante que o
cliente final seja informado das etapas-chave da sua jornada (boas-vindas, redefinição de senha,
confirmação de pedido, resultado do pagamento) mesmo sob falhas transitórias de envio.

## Use Cases

- Como cliente final recém-cadastrado, quero receber um e-mail de boas-vindas, para confirmar que
  minha conta foi criada com sucesso.
- Como cliente final que solicitou redefinição de senha, quero receber um e-mail com o link/token de
  reset, para recuperar acesso à minha conta.
- Como cliente final que finalizou uma compra, quero receber um e-mail de confirmação do pedido,
  para ter um comprovante com os itens e o total.
- Como cliente final cujo pagamento foi aprovado, quero receber um e-mail de confirmação, para saber
  que a compra foi concluída com sucesso.
- Como cliente final cujo pagamento foi recusado, quero receber um e-mail com o motivo e
  orientações, para entender o que houve e tentar novamente.
- Como operador da plataforma, quero que e-mails com falha de envio sejam reenviados
  automaticamente (retry com backoff) e, se persistirem, enviados a um Dead Letter Topic, para não
  perder silenciosamente uma notificação importante.

## Critérios de Aceite

| Cenário | Dado | Quando | Então |
|---|---|---|---|
| Envio de boas-vindas | Evento `user.created` publicado pelo User Service | Notification Service consome o evento | `EmailService` é chamado com o template `welcome.html` e o nome do usuário, e o e-mail chega no MailHog |
| Envio de reset de senha | Evento `user.password-reset` publicado pelo Auth Service | Notification Service consome o evento | `EmailService` é chamado com o template `password-reset.html` contendo o token e TTL de 15 min |
| Envio de confirmação de pedido | Evento `order.created` publicado pelo Order Service | Notification Service consome o evento | `EmailService` é chamado com o template `order-confirmed.html` contendo itens e total do pedido |
| Envio de pagamento aprovado/rejeitado | Evento `payment.approved` ou `payment.rejected` publicado pelo Payment Service | Notification Service consome o evento correspondente | `EmailService` é chamado com o template `payment-approved.html` ou `payment-rejected.html`, respectivamente |
| Retry e Dead Letter Topic | `JavaMailSender` configurado para lançar exceção nas 3 tentativas | Notification Service consome um evento e todas as tentativas de envio falham (backoff 1s → 2s → 4s) | Mensagem é publicada no tópico `.DLT` correspondente após a 3ª falha |
| Template renderiza dados corretos | Evento com dados de exemplo (nome, itens, valores) | Thymeleaf renderiza o template do tipo de evento | HTML gerado contém os dados do evento (nome do usuário, itens do pedido, valor) corretamente interpolados |

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

group = 'com.autohubstore'
version = '1.0.0'

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
| `user.password-reset` | `notification-user-group` | `password-reset.html` | Redefinição de senha |
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

DLTs criados automaticamente: `user.created.DLT`, `user.password-reset.DLT`, etc.

## Estrutura de Pacotes (MVC)

```
com.autohubstore.notificationservice/
├── controller/
│   └── HealthController.java              # Apenas para documentação Swagger
├── service/
│   └── EmailService.java                  # JavaMailSender + Thymeleaf rendering
├── messaging/
│   ├── UserEventConsumer.java             # @KafkaListener user.created + user.password-reset
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

**Resposta JSON em `snake_case`:** adicionar em `application.yml` (campo Java continua
`lowerCamelCase`, só a serialização de saída HTTP vira `snake_case` — ver
[CLAUDE.md § Convenções de Código](../../../CLAUDE.md#convenções-de-código)). Vale para o
endpoint de health/documentação Swagger deste serviço, já que ele não expõe payload de domínio
próprio (é stateless, só consumidor Kafka):

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
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
    sourceSets = [sourceSets.main, sourceSets.test] // valida também src/test/
}
```

## Estratégia de Testes

- **Unitários:** `EmailService` com mock do `JavaMailSender`; verificar template correto selecionado por tipo de evento
- **Integração:** Testcontainers Kafka; publicar evento → verificar `EmailService.send()` chamado com dados corretos
- **DLT:** Testar que após 3 falhas de envio (exception lançada) a mensagem vai para o DLT
- **Templates:** Testar rendering Thymeleaf com dados de exemplo → verificar conteúdo HTML gerado

**Critério de conclusão:** serviço só é considerado pronto quando atender aos 8 itens do
[action-plan.md § Critério de Conclusão de Microsserviço](../action-plan.md#critério-de-conclusão-de-microsserviço)
— unitários, aceitação (Cucumber), cobertura ≥ 70%, checkstyle sem violação, Snyk `ok: true`, build
com sucesso, DER em `docs/planning/der/` atualizado e documentação publicada em
`docs/apps/notification-service.md`.
