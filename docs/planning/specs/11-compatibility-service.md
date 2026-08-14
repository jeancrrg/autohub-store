# Compatibility Service

**Build Tool:** Gradle | **Arquitetura:** MVC | **Porta:** 8011 | **Status:** Planejado

## Objetivo

Responder "essa peça é compatível com quais veículos?" (e a pergunta inversa: "quais peças servem
nesse veículo?"). Responsabilidade extraída do Catalog Service — ver decisão em
[docs/planning/action-plan.md § Decisões de Consolidação](../action-plan.md#decisões-de-consolidação).

> **Por que MongoDB e não PostgreSQL:** aplicação de compatibilidade (fitment) varia por categoria
> de peça — um filtro de óleo tem atributos de aplicação diferentes de uma pastilha de freio ou de
> um farol. Modelar isso em relacional gera schema esparso (colunas nulas em massa) ou explosão de
> tabelas por categoria. Documento por aplicação, com campos livres por categoria, resolve sem
> normalizar demais um dado que não precisa de JOIN nem de transação multi-tabela. Não há
> integridade referencial forte a proteger aqui (ao contrário de Order/Payment/Inventory) — pior
> caso de inconsistência é uma aplicação órfã, resolvido de forma assíncrona pelo consumer de
> `catalog.product-created`.

## Banco de Dados: MongoDB (`compatibility_db`)

## Responsabilidades

- CRUD de aplicações (peça × veículo): `productId` + marca + modelo + intervalo de ano + motorização
- Consulta direta: dado um `productId`, listar veículos compatíveis (usada pelo Catalog na PDP)
- Consulta inversa: dado marca/modelo/ano, listar produtos compatíveis (usada em filtro de busca por veículo)
- Consumir `catalog.product-created` (Kafka) para validar que `productId` referenciado existe no Catalog

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring Data MongoDB | 3.x | Persistência documental |
| Spring Kafka | 3.x | Consumer de `catalog.product-created` |
| Bean Validation | Jakarta | Validação de entrada |
| Springdoc OpenAPI | 2.x | Swagger |
| Testcontainers | 1.19+ | Testes de integração (MongoDB + Kafka) |

## Dependências Gradle (build.gradle)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.autohubstore'
version = '0.0.1-SNAPSHOT'

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
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:mongodb'
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
# Público / consumido pelo Catalog via OpenFeign
GET    /api/v1/compatibility/products/{productId}/vehicles   # Veículos compatíveis com a peça
GET    /api/v1/compatibility/vehicles                        # Peças compatíveis com um veículo
                                                               # query params: brand, model, year, engine

# Admin (requer role ADMIN via JWT)
POST   /api/v1/compatibility                                 # Cria aplicação peça x veículo
PUT    /api/v1/compatibility/{id}                             # Atualiza aplicação
DELETE /api/v1/compatibility/{id}                             # Remove aplicação
GET    /api/v1/compatibility/products/{productId}             # Lista todas aplicações de uma peça (admin, sem agregação)
```

## Modelo de Dados (MongoDB)

Coleção `vehicle_compatibilities` — um documento por aplicação (peça × combinação de veículo).
Sem referência embutida de produto: apenas `productId` (UUID como `String`), resolvido no Catalog
Service — mesma regra de "nunca join automático" usada nas entidades JPA dos demais serviços
(ver [CLAUDE.md § Padrões de Implementação](../../CLAUDE.md#padrões-de-implementação--obrigatórios-em-todo-código-gerado)).

```json
{
  "_id": "ObjectId",
  "productId": "uuid-do-catalog-service",
  "productSku": "FLT-KN-0042",
  "brand": "Volkswagen",
  "model": "Gol",
  "yearStart": 2013,
  "yearEnd": 2019,
  "engine": "1.6 8V",
  "fuelType": "FLEX",
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z"
}
```

> `productSku` é denormalizado a partir do evento `catalog.product-created` só pra exibição em tela
> admin sem round-trip ao Catalog — não é fonte de verdade; `productId` é o único campo usado em
> lookup/consulta.

**Índices:**

```javascript
db.vehicle_compatibilities.createIndex({ productId: 1 })
db.vehicle_compatibilities.createIndex({ brand: 1, model: 1, yearStart: 1, yearEnd: 1 })
```

## Eventos Kafka Consumidos

**Tópico `catalog.product-created`** (consumer group `compatibility-service`):

```json
{
  "productId": "uuid",
  "sku": "FLT-KN-0042",
  "slug": "filtro-de-ar-kn-0042",
  "name": "Filtro de Ar K&N",
  "categoryId": "uuid",
  "status": "ACTIVE"
}
```

Consumer grava `productId`/`sku` num cache local (coleção `known_products` ou lookup direto se
`productId` já existir em alguma aplicação pendente) — usado só pra alertar em log quando admin
cadastra aplicação com `productId` desconhecido; não bloqueia a escrita (evento pode chegar depois
da chamada de cadastro, é validação best-effort, não constraint de integridade).

## Estrutura de Pacotes (MVC)

```
com.autohubstore.compatibilityservice/
├── controller/
│   └── CompatibilityController.java        # CRUD admin + lookup direto/inverso
├── service/
│   └── CompatibilityService.java           # Lógica de negócio, validação de intervalo de ano
├── repository/
│   └── VehicleCompatibilityRepository.java # MongoRepository<VehicleCompatibility, String>
├── model/
│   ├── VehicleCompatibility.java           # @Document(collection = "vehicle_compatibilities")
│   ├── FuelType.java                       # Enum: FLEX, GASOLINE, DIESEL, ELECTRIC, HYBRID
│   ├── CreateCompatibilityRequest.java     # DTO entrada
│   ├── UpdateCompatibilityRequest.java     # DTO entrada
│   └── CompatibilityResponse.java          # DTO saída
├── messaging/
│   └── ProductEventConsumer.java           # @KafkaListener catalog.product-created
├── exception/
│   └── GlobalExceptionHandler.java         # @ControllerAdvice
└── config/
    └── MongoConfig.java                    # índices, conversores customizados se necessário
```

## Variáveis de Ambiente

```
MONGO_URI=mongodb://mongo-compatibility:27017/compatibility_db
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

## Docker

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
COPY build/libs/compatibility-service.jar app.jar
EXPOSE 8011
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Checkstyle

> **Código deve nascer conforme:** escrever classes já seguindo `infra/checkstyle/checkstyle.xml`
> (linha em branco após `{` de abertura e antes do `}` de fechamento da classe, sem números mágicos,
> sem exceções/catches genéricos, campos `private`, etc. — resumo em
> [CLAUDE.md § Checkstyle](../../CLAUDE.md#checkstyle--obrigatório-em-todo-código-gerado)).
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

- **Unitários:** `CompatibilityService` (validação de intervalo de ano, montagem de query inversa)
- **Integração:** Testcontainers (MongoDB + Kafka); criar aplicação → consultar por `productId` →
  consultar por veículo (marca/modelo/ano) → publicar `catalog.product-created` → verificar log de
  reconciliação
