package com.autohubstore.gateway.acceptance.steps;

import com.autohubstore.gateway.acceptance.config.WireMockSupport;
import com.autohubstore.gateway.acceptance.util.HttpTestUtil;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

public class ApiGatewayAcceptanceSteps {

    private static final String ACCESS_TOKEN_JWT_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0zMmJ5dGVz";
    private static final String OTHER_JWT_SECRET = "b3V0cm8tc2VncmVkby1kaWZlcmVudGUtcGFyYS10ZXN0ZS0zMmJ5dGVz";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String USERS_ENDPOINT_PATH = "/api/v1/users/11111111-1111-1111-1111-111111111111";
    private static final String RATE_LIMIT_ENDPOINT_PATH = "/api/v1/catalog/products/rate-limit-scenario";
    private static final String CORS_ENDPOINT_PATH = "/api/v1/catalog/products";
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final int RATE_LIMIT_WINDOW_REQUESTS = 100;

    @Autowired
    private HttpTestUtil httpAcceptanceTestUtil;

    private WebTestClient.ResponseSpec lastResponse;
    private String accessTokenCookieValue;
    private String corsOrigin;

    @Dado("que o user-service estar saudavel e retornar o usuario {string}")
    public void givenUserServiceIsHealthyAndReturnsUser(String userId) {
        WireMockSupport.server().stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/users/" + userId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + userId + "\",\"name\":\"Cliente Teste\"}")));
    }

    @Dado("que o cliente possuir um cookie de acesso valido")
    public void givenTheClientHasAValidAccessCookie() {
        accessTokenCookieValue = buildToken(signingKey(ACCESS_TOKEN_JWT_SECRET), ONE_HOUR_MS);
    }

    @Dado("que o cliente nao possuir cookie de acesso")
    public void givenTheClientDoesNotHaveAnAccessCookie() {
        accessTokenCookieValue = null;
    }

    @Dado("que o cliente possuir um cookie de acesso {string}")
    public void givenTheClientHasAnAccessCookieInSituation(String situation) {
        if ("expirado".equals(situation)) {
            accessTokenCookieValue = buildToken(signingKey(ACCESS_TOKEN_JWT_SECRET), -ONE_HOUR_MS);
            return;
        }
        accessTokenCookieValue = buildToken(signingKey(OTHER_JWT_SECRET), ONE_HOUR_MS);
    }

    @Quando("o cliente chamar o endpoint de usuarios com o cookie de acesso")
    public void whenTheClientCallsWithTheAccessCookie() {
        lastResponse = httpAcceptanceTestUtil.executeGetWithCookie(
                USERS_ENDPOINT_PATH, ACCESS_TOKEN_COOKIE, accessTokenCookieValue);
    }

    @Quando("o cliente chamar o endpoint de usuarios sem o cookie de acesso")
    public void whenTheClientCallsNotWithTheAccessCookie() {
        lastResponse = httpAcceptanceTestUtil.executeGetWithoutCookie(USERS_ENDPOINT_PATH);
    }

    @Dado("que o cliente ja ter realizado 100 requisicoes ao endpoint publico do catalogo na janela atual")
    public void givenTheClientHasAlreadyMadeRequestsToThePublicEndpointInCurrentWindow() {
        httpAcceptanceTestUtil.executeGetRepeatedly(RATE_LIMIT_ENDPOINT_PATH, RATE_LIMIT_WINDOW_REQUESTS);
    }

    @Quando("o cliente realizar mais uma requisicao a esse endpoint")
    public void whenTheClientMakesOneMoreRequestToTheEndpoint() {
        lastResponse = httpAcceptanceTestUtil.executeGetWithoutCookie(RATE_LIMIT_ENDPOINT_PATH);
    }

    @Dado("que a origem {string} nao estar na lista de origens permitidas")
    public void givenOriginIsNotInTheAllowedOriginsList(String origin) {
        corsOrigin = origin;
    }

    @Quando("o gateway receber uma requisicao de preflight CORS do catalogo com essa origem")
    public void whenTheGatewayReceivesACorsPreflightRequestWithThatOrigin() {
        lastResponse = httpAcceptanceTestUtil.executeCorsPreflight(CORS_ENDPOINT_PATH, corsOrigin);
    }

    @Entao("o cliente deve receber resposta com status {int}")
    public void thenTheClientReceivesResponseWithStatus(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @Entao("a resposta nao deve conter o header Access-Control-Allow-Origin correspondente")
    public void thenResponseDoesNotContainTheAccessControlAllowOriginHeader() {
        lastResponse.expectHeader().doesNotExist(ALLOW_ORIGIN_HEADER);
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
