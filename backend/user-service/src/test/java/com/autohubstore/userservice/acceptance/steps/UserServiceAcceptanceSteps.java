package com.autohubstore.userservice.acceptance.steps;

import com.autohubstore.userservice.acceptance.util.HttpAcceptanceTestException;
import com.autohubstore.userservice.acceptance.util.HttpTestUtil;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserServiceAcceptanceSteps {

    private static final String USERS_ENDPOINT_PATH = "/api/v1/users";
    private static final String INTERNAL_VERIFY_CREDENTIALS_ENDPOINT_PATH =
            "/internal/v1/users/verify-credentials";
    private static final String KNOWN_PASSWORD = "senha-correta-123";
    private static final String WRONG_PASSWORD = "senha-errada-456";
    private static final String UPDATED_FULL_NAME = "Nome Atualizado";
    private static final String DEFAULT_EMAIL_DOMAIN = "@autohubstore.com";

    @Autowired
    private HttpTestUtil httpTestUtil;

    private ResultActions lastResult;
    private Map<String, Object> requestBody;
    private String registeredEmail;
    private UUID registeredUserId;
    private UUID registeredAddressId;

    @Dado("que o cliente informar dados validos de cadastro")
    public void givenTheClientProvidesValidRegistrationData() {
        registeredEmail = "cliente-" + UUID.randomUUID() + DEFAULT_EMAIL_DOMAIN;
        requestBody = buildCreateUserBody(registeredEmail);
    }

    @Dado("que o cliente informar dados de cadastro sem o campo {string}")
    public void givenTheClientProvidesRegistrationDataWithoutField(String field) {
        requestBody = buildCreateUserBody("cliente-" + UUID.randomUUID() + DEFAULT_EMAIL_DOMAIN);
        requestBody.remove(field);
    }

    @Dado("que o cliente ja possuir uma conta cadastrada")
    public void givenTheClientAlreadyHasARegisteredAccount() {
        registeredEmail = "cliente-" + UUID.randomUUID() + DEFAULT_EMAIL_DOMAIN;
        httpTestUtil.executePost(USERS_ENDPOINT_PATH, buildCreateUserBody(registeredEmail));
    }

    @Quando("o cliente enviar a requisicao de cadastro de usuario")
    public void whenTheClientSendsTheUserRegistrationRequest() {
        lastResult = httpTestUtil.executePost(USERS_ENDPOINT_PATH, requestBody);
    }

    @Quando("o cliente enviar a requisicao de cadastro de usuario com o mesmo e-mail")
    public void whenTheClientSendsTheUserRegistrationRequestWithTheSameEmail() {
        lastResult = httpTestUtil.executePost(USERS_ENDPOINT_PATH, buildCreateUserBody(registeredEmail));
    }

    @Dado("que exista um usuario cadastrado")
    public void givenThereIsARegisteredUser() {
        registeredEmail = "cliente-" + UUID.randomUUID() + DEFAULT_EMAIL_DOMAIN;
        String responseBody = readBody(
                httpTestUtil.executePost(USERS_ENDPOINT_PATH, buildCreateUserBody(registeredEmail)));
        registeredUserId = UUID.fromString(extractJsonField(responseBody));
    }

    @Dado("que exista um usuario cadastrado com senha conhecida")
    public void givenThereIsARegisteredUserWithAKnownPassword() {
        registeredEmail = "cliente-" + UUID.randomUUID() + DEFAULT_EMAIL_DOMAIN;
        Map<String, Object> body = buildCreateUserBody(registeredEmail);
        body.put("password", KNOWN_PASSWORD);
        String responseBody = readBody(httpTestUtil.executePost(USERS_ENDPOINT_PATH, body));
        registeredUserId = UUID.fromString(extractJsonField(responseBody));
    }

    @Dado("que exista um usuario cadastrado com um endereco cadastrado")
    public void givenThereIsARegisteredUserWithARegisteredAddress() {
        givenThereIsARegisteredUser();
        Map<String, Object> addressBody = buildAddressBody();
        String responseBody = readBody(httpTestUtil
                .executePost(USERS_ENDPOINT_PATH + "/" + registeredUserId + "/addresses", addressBody));
        registeredAddressId = UUID.fromString(extractJsonField(responseBody));
    }

    @Quando("o cliente enviar a requisicao de atualizacao de perfil com um novo nome")
    public void whenTheClientSendsTheProfileUpdateRequestWithANewName() {
        Map<String, Object> body = new HashMap<>();
        body.put("full_name", UPDATED_FULL_NAME);
        lastResult = httpTestUtil.executePut(USERS_ENDPOINT_PATH + "/" + registeredUserId, body);
    }

    @Quando("o cliente enviar a requisicao de criacao de endereco para esse usuario")
    public void whenTheClientSendsTheAddressCreationRequestForThatUser() {
        lastResult = httpTestUtil.executePost(
                USERS_ENDPOINT_PATH + "/" + registeredUserId + "/addresses", buildAddressBody());
    }

    @Quando("o cliente enviar a requisicao de remocao desse endereco")
    public void whenTheClientSendsTheAddressRemovalRequest() {
        lastResult = httpTestUtil.executeDelete(
                USERS_ENDPOINT_PATH + "/" + registeredUserId + "/addresses/" + registeredAddressId);
    }

    @Quando("o auth-service enviar a requisicao interna de verificacao de credenciais com a senha correta")
    public void whenTheAuthServiceSendsTheInternalCredentialsVerificationRequestWithTheCorrectPassword() {
        lastResult = httpTestUtil.executePost(
                INTERNAL_VERIFY_CREDENTIALS_ENDPOINT_PATH, buildVerifyCredentialsBody(KNOWN_PASSWORD));
    }

    @Quando("o auth-service enviar a requisicao interna de verificacao de credenciais com a senha incorreta")
    public void whenTheAuthServiceSendsTheInternalCredentialsVerificationRequestWithTheWrongPassword() {
        lastResult = httpTestUtil.executePost(
                INTERNAL_VERIFY_CREDENTIALS_ENDPOINT_PATH, buildVerifyCredentialsBody(WRONG_PASSWORD));
    }

    @Quando("o auth-service enviar a requisicao interna de atualizacao de senha para esse usuario")
    public void whenTheAuthServiceSendsTheInternalPasswordUpdateRequestForThatUser() {
        Map<String, Object> body = new HashMap<>();
        body.put("new_password", "nova-senha-987");
        lastResult = httpTestUtil.executePut(
                "/internal/v1/users/" + registeredUserId + "/password", body);
    }

    @Entao("o cliente deve receber resposta com status {int}")
    public void thenTheClientReceivesResponseWithStatus(int statusCode) {
        try {
            lastResult.andExpect(status().is(statusCode));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao validar status da resposta", ex);
        }
    }

    @Entao("o corpo da resposta deve conter o e-mail cadastrado")
    public void thenTheResponseBodyContainsTheRegisteredEmail() {
        try {
            lastResult.andExpect(jsonPath("$.email").value(registeredEmail));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao validar e-mail da resposta", ex);
        }
    }

    @Entao("o corpo da resposta deve conter o nome atualizado")
    public void thenTheResponseBodyContainsTheUpdatedName() {
        try {
            lastResult.andExpect(jsonPath("$.full_name").value(UPDATED_FULL_NAME));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao validar nome atualizado na resposta", ex);
        }
    }

    private String readBody(ResultActions resultActions) {
        try {
            return resultActions.andReturn().getResponse().getContentAsString();
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao ler corpo da resposta", ex);
        }
    }

    private Map<String, Object> buildCreateUserBody(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("full_name", "Cliente de Teste");
        body.put("password", "senha-padrao-123");
        return body;
    }

    private Map<String, Object> buildAddressBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("street", "Rua de Teste");
        body.put("number", "100");
        body.put("city", "Sao Paulo");
        body.put("state", "SP");
        body.put("zip_code", "01234-567");
        body.put("is_default", false);
        return body;
    }

    private Map<String, Object> buildVerifyCredentialsBody(String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", registeredEmail);
        body.put("password", password);
        return body;
    }

    private String extractJsonField(String responseBody) {
        String marker = "\"" + "id" + "\":\"";
        int start = responseBody.indexOf(marker) + marker.length();
        int end = responseBody.indexOf('"', start);
        return responseBody.substring(start, end);
    }

}
