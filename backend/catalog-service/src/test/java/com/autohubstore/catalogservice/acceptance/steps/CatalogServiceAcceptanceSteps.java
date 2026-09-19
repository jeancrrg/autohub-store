package com.autohubstore.catalogservice.acceptance.steps;

import com.autohubstore.catalogservice.acceptance.util.HttpAcceptanceTestException;
import com.autohubstore.catalogservice.acceptance.util.HttpAcceptanceTestUtil;
import com.autohubstore.catalogservice.domain.entity.Brand;
import com.autohubstore.catalogservice.domain.entity.Category;
import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.repository.BrandRepository;
import com.autohubstore.catalogservice.repository.CategoryRepository;
import com.autohubstore.catalogservice.repository.ProductRepository;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CatalogServiceAcceptanceSteps {

    private static final String PRODUCTS_ENDPOINT_PATH = "/api/v1/catalog/products";
    private static final String CATEGORIES_ENDPOINT_PATH = "/api/v1/catalog/categories";
    private static final String BRANDS_ENDPOINT_PATH = "/api/v1/catalog/brands";
    private static final String PRODUCT_CREATED_TOPIC = "catalog.product-created";
    private static final String PRODUCT_UPDATED_TOPIC = "catalog.product-updated";
    private static final BigDecimal INITIAL_PRICE = BigDecimal.valueOf(100);
    private static final BigDecimal UPDATED_PRICE = BigDecimal.valueOf(250);
    private static final Duration KAFKA_POLL_TIMEOUT = Duration.ofSeconds(10);
    private static final int UNIQUE_SUFFIX_LENGTH = 8;

    @Autowired
    private HttpAcceptanceTestUtil httpAcceptanceTestUtil;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    private ResultActions lastResult;
    private UUID categoryId;
    private UUID brandId;
    private UUID otherBrandId;
    private UUID createdProductId;
    private String createdProductSku;

    @Dado("que uma categoria e uma marca existentes estejam cadastradas")
    public void givenAnExistingCategoryAndBrandAreRegistered() {
        Category category = categoryRepository.findAll().get(0);
        List<Brand> brands = brandRepository.findAllByOrderByNameAsc();
        categoryId = category.getId();
        brandId = brands.get(0).getId();
        otherBrandId = brands.get(1).getId();
    }

    @Dado("que um produto ja tenha sido criado no catalogo")
    public void givenAProductHasAlreadyBeenCreatedInTheCatalog() {
        givenAnExistingCategoryAndBrandAreRegistered();
        String responseBody = readBody(httpAcceptanceTestUtil.executePost(
                PRODUCTS_ENDPOINT_PATH, buildCreateProductBody(null)));
        createdProductId = extractUuidField(responseBody, "id");
        createdProductSku = extractStringField(responseBody, "sku");
    }

    @Dado("que o produto ja tenha sido consultado uma vez, populando o cache")
    public void givenTheProductHasAlreadyBeenQueriedOncePopulatingTheCache() {
        httpAcceptanceTestUtil.executeGet(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId);
    }

    @Quando("o preco do produto for alterado diretamente no banco, contornando o servico")
    public void whenTheProductPriceIsChangedDirectlyInTheDatabaseBypassingTheService() {
        Product product = productRepository.findById(createdProductId).orElseThrow();
        product.setPrice(UPDATED_PRICE);
        productRepository.save(product);
    }

    @Quando("o cliente consultar o produto novamente")
    public void whenTheClientQueriesTheProductAgain() {
        lastResult = httpAcceptanceTestUtil.executeGet(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId);
    }

    @Quando("o cliente consultar o produto criado")
    public void whenTheClientQueriesTheCreatedProduct() {
        lastResult = httpAcceptanceTestUtil.executeGet(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId);
    }

    @Entao("a resposta deve conter o preco anterior, obtido do cache")
    public void thenTheResponseContainsThePreviousPriceObtainedFromTheCache() {
        assertJsonPathValue(jsonPath("$.price").value(INITIAL_PRICE.doubleValue()));
    }

    @Quando("o administrador enviar a requisicao de criacao de produto com dados validos")
    public void whenTheAdministratorSendsTheProductCreationRequestWithValidData() {
        lastResult = httpAcceptanceTestUtil.executePost(PRODUCTS_ENDPOINT_PATH, buildCreateProductBody(null));
    }

    @Dado("que exista um produto cadastrado com sku conhecido")
    public void givenThereIsARegisteredProductWithAKnownSku() {
        givenAProductHasAlreadyBeenCreatedInTheCatalog();
    }

    @Quando("o administrador enviar a requisicao de criacao de produto reusando esse sku")
    public void whenTheAdministratorSendsTheProductCreationRequestReusingThatSku() {
        lastResult = httpAcceptanceTestUtil.executePost(
                PRODUCTS_ENDPOINT_PATH, buildCreateProductBody(createdProductSku));
    }

    @Quando("o administrador enviar a requisicao de criacao de produto informando um brandId inexistente")
    public void whenTheAdministratorSendsTheProductCreationRequestWithANonexistentBrandId() {
        Map<String, Object> body = buildCreateProductBody(null);
        body.put("brand_id", UUID.randomUUID().toString());
        lastResult = httpAcceptanceTestUtil.executePost(PRODUCTS_ENDPOINT_PATH, body);
    }

    @Quando("o admin enviar upload de arquivo {string} do tipo {string} com {int} bytes")
    public void whenTheAdminSendsFileUploadOfTypeWithBytes(String fileName, String contentType, int sizeInBytes) {
        byte[] content = new byte[sizeInBytes];
        MockMultipartFile file = new MockMultipartFile("files", fileName, contentType, content);
        lastResult = httpAcceptanceTestUtil.executeMultipartUpload(
                PRODUCTS_ENDPOINT_PATH + "/images/" + createdProductId, List.of(file));
    }

    @Quando("o admin enviar upload de uma imagem PNG valida")
    public void whenTheAdminSendsAValidPngImageUpload() {
        MockMultipartFile file = new MockMultipartFile("files", "foto.png", "image/png", "conteudo-imagem".getBytes());
        lastResult = httpAcceptanceTestUtil.executeMultipartUpload(
                PRODUCTS_ENDPOINT_PATH + "/images/" + createdProductId, List.of(file));
    }

    @Quando("o administrador enviar a requisicao de atualizacao alterando o preco do produto")
    public void whenTheAdministratorSendsTheUpdateRequestChangingTheProductPrice() {
        Map<String, Object> body = new HashMap<>();
        body.put("price", UPDATED_PRICE);
        lastResult = httpAcceptanceTestUtil.executePut(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId, body);
    }

    @Quando("o administrador enviar a requisicao de atualizacao alterando a marca do produto")
    public void whenTheAdministratorSendsTheUpdateRequestChangingTheProductBrand() {
        Map<String, Object> body = new HashMap<>();
        body.put("brand_id", otherBrandId.toString());
        lastResult = httpAcceptanceTestUtil.executePut(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId, body);
    }

    @Quando("o cliente consultar a lista de marcas")
    public void whenTheClientQueriesTheBrandList() {
        lastResult = httpAcceptanceTestUtil.executeGet(BRANDS_ENDPOINT_PATH);
    }

    @Quando("o administrador enviar a requisicao de criacao de categoria com nome e slug unicos")
    public void whenTheAdministratorSendsTheCategoryCreationRequestWithUniqueNameAndSlug() {
        String unique = UUID.randomUUID().toString().substring(0, UNIQUE_SUFFIX_LENGTH);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Categoria Teste " + unique);
        body.put("slug", "categoria-teste-" + unique);
        lastResult = httpAcceptanceTestUtil.executePost(CATEGORIES_ENDPOINT_PATH, body);
    }

    @Entao("o cliente deve receber resposta com status {int}")
    public void thenTheClientReceivesResponseWithStatus(int statusCode) {
        try {
            lastResult.andExpect(status().is(statusCode));
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao validar status da resposta", ex);
        }
    }

    @Entao("o evento de produto criado deve ser publicado no topico correspondente")
    public void thenTheProductCreatedEventIsPublishedToTheCorrespondingTopic() {
        String responseBody = readBody(lastResult);
        String productId = extractUuidField(responseBody, "id").toString();
        assertEventPublished(PRODUCT_CREATED_TOPIC, productId);
    }

    @Entao("o evento de produto atualizado deve ser publicado no topico correspondente")
    public void thenTheProductUpdatedEventIsPublishedToTheCorrespondingTopic() {
        assertEventPublished(PRODUCT_UPDATED_TOPIC, createdProductId.toString());
    }

    @Entao("a resposta deve conter a marca resolvida com nome e slug")
    public void thenTheResponseContainsTheResolvedBrandWithNameAndSlug() {
        Brand brand = brandRepository.findById(brandId).orElseThrow();
        assertJsonPathValue(jsonPath("$.brand_name").value(brand.getName()));
        assertJsonPathValue(jsonPath("$.brand_slug").value(brand.getSlug()));
    }

    @Entao("a consulta ao produto deve refletir o preco atualizado")
    public void thenQueryingTheProductReflectsTheUpdatedPrice() {
        lastResult = httpAcceptanceTestUtil.executeGet(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId);
        assertJsonPathValue(jsonPath("$.price").value(UPDATED_PRICE.doubleValue()));
    }

    @Entao("a consulta ao produto deve refletir a marca atualizada")
    public void thenQueryingTheProductReflectsTheUpdatedBrand() {
        Brand brand = brandRepository.findById(otherBrandId).orElseThrow();
        lastResult = httpAcceptanceTestUtil.executeGet(PRODUCTS_ENDPOINT_PATH + "/" + createdProductId);
        assertJsonPathValue(jsonPath("$.brand_id").value(otherBrandId.toString()));
        assertJsonPathValue(jsonPath("$.brand_name").value(brand.getName()));
    }

    @Entao("a lista de marcas deve estar ordenada alfabeticamente por nome")
    public void thenTheBrandListIsOrderedAlphabeticallyByName() {
        String responseBody = readBody(lastResult);
        List<String> names = extractBrandNames(responseBody);
        List<String> sorted = names.stream().sorted().toList();
        if (!names.equals(sorted)) {
            throw new HttpAcceptanceTestException("Lista de marcas nao esta ordenada alfabeticamente", null);
        }
    }

    @Entao("a primeira imagem cadastrada deve estar marcada como principal")
    public void thenTheFirstRegisteredImageIsMarkedAsPrimary() {
        assertJsonPathValue(jsonPath("$[0].primary").value(true));
    }

    @Entao("o corpo da resposta do produto nao deve conter o campo de quantidade em estoque")
    public void thenTheProductResponseBodyDoesNotContainTheStockQuantityField() {
        String responseBody = readBody(lastResult);
        if (responseBody.contains("stock_quantity")) {
            throw new HttpAcceptanceTestException("Resposta do produto contem o campo stock_quantity", null);
        }
    }

    private void assertEventPublished(String topic, String expectedProductId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "catalog-service-acceptance-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(topic));
            boolean found = false;
            long deadline = System.currentTimeMillis() + KAFKA_POLL_TIMEOUT.toMillis();
            while (!found && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.key() != null && record.key().equals(expectedProductId)) {
                        found = true;
                    }
                }
            }
            if (!found) {
                throw new HttpAcceptanceTestException(
                        "Evento nao encontrado no topico " + topic + " para productId " + expectedProductId, null);
            }
        }
    }

    private void assertJsonPathValue(ResultMatcher matcher) {
        try {
            lastResult.andExpect(matcher);
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao validar campo da resposta", ex);
        }
    }

    private String readBody(ResultActions resultActions) {
        try {
            return resultActions.andReturn().getResponse().getContentAsString();
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao ler corpo da resposta", ex);
        }
    }

    private Map<String, Object> buildCreateProductBody(String sku) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Produto Teste " + UUID.randomUUID());
        if (sku != null) {
            body.put("sku", sku);
        }
        body.put("description", "Descricao de teste");
        body.put("price", INITIAL_PRICE);
        body.put("category_id", categoryId.toString());
        body.put("brand_id", brandId.toString());
        return body;
    }

    private UUID extractUuidField(String responseBody, String field) {
        return UUID.fromString(extractStringField(responseBody, field));
    }

    private String extractStringField(String responseBody, String field) {
        String marker = "\"" + field + "\":\"";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            throw new HttpAcceptanceTestException("Campo " + field + " nao encontrado na resposta: " + responseBody,
                    null);
        }
        start += marker.length();
        int end = responseBody.indexOf('"', start);
        return responseBody.substring(start, end);
    }

    private List<String> extractBrandNames(String responseBody) {
        List<String> names = new ArrayList<>();
        String marker = "\"name\":\"";
        int index = 0;
        while (true) {
            int start = responseBody.indexOf(marker, index);
            if (start < 0) {
                break;
            }
            start += marker.length();
            int end = responseBody.indexOf('"', start);
            names.add(responseBody.substring(start, end));
            index = end;
        }
        return names;
    }

}
