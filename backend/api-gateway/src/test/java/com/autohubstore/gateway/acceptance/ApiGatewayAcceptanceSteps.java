package com.autohubstore.gateway.acceptance;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ORIGIN;

@RequiredArgsConstructor
public class ApiGatewayAcceptanceSteps {

    private static final String ACCESS_TOKEN_JWT_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0zMmJ5dGVz";
    private static final String OTHER_JWT_SECRET = "b3V0cm8tc2VncmVkby1kaWZlcmVudGUtcGFyYS10ZXN0ZS0zMmJ5dGVz";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String CORS_PREFLIGHT_METHOD_HEADER = "Access-Control-Request-Method";
    private static final String ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final int RATE_LIMIT_WINDOW_REQUESTS = 100;

    private final WebTestClient webTestClient;

    private String accessTokenCookieValue;
    private WebTestClient.ResponseSpec lastResponse;

    @Given("que o user-service esta saudavel e retorna o usuario {string}")
    public void userServiceEstaSaudavelERetornaOUsuario(String userId) {
        WireMockSupport.server().stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/users/" + userId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + userId + "\",\"name\":\"Cliente Teste\"}")));
    }

    @Given("que eu possuo um cookie de acesso valido")
    public void possuoUmCookieDeAcessoValido() {
        accessTokenCookieValue = buildToken(signingKey(ACCESS_TOKEN_JWT_SECRET), ONE_HOUR_MS);
    }

    @Given("que eu nao possuo cookie de acesso")
    public void naoPossuoCookieDeAcesso() {
        accessTokenCookieValue = null;
    }

    @Given("que eu possuo um cookie de acesso {string}")
    public void possuoUmCookieDeAcessoComSituacao(String situacao) {
        if ("expirado".equals(situacao)) {
            accessTokenCookieValue = buildToken(signingKey(ACCESS_TOKEN_JWT_SECRET), -ONE_HOUR_MS);
            return;
        }
        accessTokenCookieValue = buildToken(signingKey(OTHER_JWT_SECRET), ONE_HOUR_MS);
    }

    @When("eu chamo {string} com o cookie de acesso")
    public void chamoComOCookieDeAcesso(String methodAndPath) {
        lastResponse = performRequest(methodAndPath, accessTokenCookieValue);
    }

    @When("eu chamo {string} sem o cookie de acesso")
    public void chamoSemOCookieDeAcesso(String methodAndPath) {
        lastResponse = performRequest(methodAndPath, null);
    }

    @Given("que eu ja realizei 100 requisicoes ao endpoint publico {string} pelo mesmo cliente na janela atual")
    public void jaRealizeiRequisicoesAoEndpointPublico(String path) {
        for (int i = 0; i < RATE_LIMIT_WINDOW_REQUESTS; i++) {
            webTestClient.get().uri(path).exchange();
        }
    }

    @When("eu realizo mais uma requisicao a {string}")
    public void realizoMaisUmaRequisicao(String path) {
        lastResponse = webTestClient.get().uri(path).exchange();
    }

    @Given("que a origem {string} nao esta na lista de origens permitidas")
    public void origemNaoEstaNaListaDeOrigensPermitidas(String origin) {
        accessTokenCookieValue = origin;
    }

    @When("eu envio uma requisicao de preflight CORS para {string} com essa origem")
    public void envioUmaRequisicaoDePreflightCorsComEssaOrigem(String path) {
        lastResponse = webTestClient.method(HttpMethod.OPTIONS)
                .uri(path)
                .header(ORIGIN, accessTokenCookieValue)
                .header(CORS_PREFLIGHT_METHOD_HEADER, HttpMethod.GET.name())
                .exchange();
    }

    @Then("a resposta possui status {int}")
    public void respostaPossuiStatus(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @Then("a resposta nao contem o header Access-Control-Allow-Origin correspondente")
    public void respostaNaoContemHeaderAccessControlAllowOrigin() {
        List<String> allowOriginHeaders = lastResponse.returnResult(Void.class).getResponseHeaders().get(ALLOW_ORIGIN_HEADER);
        assertThat(allowOriginHeaders).isNull();
    }

    private WebTestClient.ResponseSpec performRequest(String methodAndPath, String cookieValue) {
        String[] parts = methodAndPath.split(" ", 2);
        HttpMethod method = HttpMethod.valueOf(parts[0]);
        String path = parts[1];
        WebTestClient.RequestHeadersSpec<?> request = webTestClient.method(method).uri(path);
        if (cookieValue != null) {
            request = request.cookie(ACCESS_TOKEN_COOKIE, cookieValue);
        }
        return request.exchange();
    }

    private SecretKey signingKey(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private String buildToken(SecretKey key, long validityMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("email", "cliente@autohubstore.com")
                .claim("roles", List.of("CUSTOMER"))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

}
