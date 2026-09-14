package com.autohubstore.authservice.acceptance.steps;

import com.autohubstore.authservice.acceptance.config.WireMockSupport;
import com.autohubstore.authservice.acceptance.util.AcceptanceTestExecutionException;
import com.autohubstore.authservice.acceptance.util.HttpTestUtil;
import com.autohubstore.authservice.domain.entity.PasswordResetToken;
import com.autohubstore.authservice.domain.mapper.TokenMapper;
import com.autohubstore.authservice.repository.PasswordResetTokenRepository;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthAcceptanceSteps {

    private static final String LOGIN_ENDPOINT_PATH = "/api/v1/auth/login";
    private static final String LOGOUT_ENDPOINT_PATH = "/api/v1/auth/logout";
    private static final String REFRESH_ENDPOINT_PATH = "/api/v1/auth/refresh";
    private static final String RESET_PASSWORD_ENDPOINT_PATH = "/api/v1/auth/reset-password";
    private static final String VERIFY_CREDENTIALS_PATH = "/internal/v1/users/verify-credentials";
    private static final String USER_PASSWORD_PATH_TEMPLATE = "/internal/v1/users/%s/password";
    private static final String USER_BY_ID_PATH_TEMPLATE = "/internal/v1/users/%s";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String TEST_EMAIL = "cliente@autohubstore.com";
    private static final String TEST_PASSWORD = "senha-correta-123";
    private static final String TEST_ROLE = "CUSTOMER";
    private static final long PASSWORD_RESET_TTL_MINUTES = 15L;
    private static final int CIRCUIT_BREAKER_ATTEMPTS = 12;
    private static final String JTI_JSON_MARKER = "\"jti\":\"";
    private static final String USER_SERVICE_CIRCUIT_BREAKER = "userService";

    @Autowired
    private HttpTestUtil httpTestUtil;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TokenMapper tokenMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private ResultActions lastResponse;
    private String accessTokenCookieValue;
    private String refreshTokenCookieValue;
    private String previousRefreshTokenCookieValue;
    private UUID testUserId;
    private String passwordResetTokenValue;

    @Before
    public void resetCircuitBreakerState() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(USER_SERVICE_CIRCUIT_BREAKER);
        circuitBreaker.reset();
    }

    @Dado("que o user-service estar saudavel e aceitar a credencial do cliente")
    public void givenUserServiceIsHealthyAndAcceptsTheClientCredential() {
        testUserId = UUID.randomUUID();
        WireMockSupport.server().stubFor(WireMock.post(WireMock.urlEqualTo(VERIFY_CREDENTIALS_PATH))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(userVerificationJson(testUserId))));
        stubFindUserById(testUserId);
    }

    @Dado("que o user-service estar saudavel e rejeitar a credencial do cliente")
    public void givenUserServiceIsHealthyAndRejectsTheClientCredential() {
        WireMockSupport.server().stubFor(WireMock.post(WireMock.urlEqualTo(VERIFY_CREDENTIALS_PATH))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));
    }

    @Dado("que o user-service estar indisponivel de forma consecutiva")
    public void givenUserServiceIsConsistentlyUnavailable() {
        WireMockSupport.server().stubFor(WireMock.post(WireMock.urlEqualTo(VERIFY_CREDENTIALS_PATH))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    @Dado("que o cliente possuir um refresh token valido emitido em um login anterior")
    public void givenTheClientHasAValidRefreshTokenFromAPreviousLogin() {
        givenUserServiceIsHealthyAndAcceptsTheClientCredential();
        performLogin();
        captureSessionCookiesFromLastResponse();
    }

    @Dado("que o cliente estar autenticado com access token e refresh token validos")
    public void givenTheClientIsAuthenticatedWithValidAccessAndRefreshTokens() {
        givenUserServiceIsHealthyAndAcceptsTheClientCredential();
        performLogin();
        captureSessionCookiesFromLastResponse();
    }

    @Dado("que o cliente possuir um token de redefinicao de senha valido")
    public void givenTheClientHasAValidPasswordResetToken() {
        testUserId = UUID.randomUUID();
        passwordResetTokenValue = UUID.randomUUID().toString();
        PasswordResetToken token =
                tokenMapper.toPasswordResetToken(testUserId, passwordResetTokenValue, PASSWORD_RESET_TTL_MINUTES);
        passwordResetTokenRepository.save(token);
    }

    @Dado("que o user-service aceitar a atualizacao de senha do usuario do token")
    public void givenUserServiceAcceptsThePasswordUpdateForTheTokenUser() {
        WireMockSupport.server().stubFor(WireMock.put(
                        WireMock.urlEqualTo(String.format(USER_PASSWORD_PATH_TEMPLATE, testUserId)))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.OK.value())));
    }

    @Quando("o cliente chamar o endpoint de login com e-mail e senha corretos")
    public void whenTheClientCallsLoginWithCorrectEmailAndPassword() {
        performLogin();
    }

    @Quando("o cliente chamar o endpoint de refresh com esse refresh token")
    public void whenTheClientCallsRefreshWithThatRefreshToken() {
        previousRefreshTokenCookieValue = refreshTokenCookieValue;
        lastResponse = httpTestUtil.executePostWithCookiesOnly(
                REFRESH_ENDPOINT_PATH, new Cookie(REFRESH_TOKEN_COOKIE, refreshTokenCookieValue));
        captureSessionCookiesFromLastResponse();
    }

    @Quando("o cliente chamar o endpoint de refresh novamente com o refresh token anterior")
    public void whenTheClientCallsRefreshAgainWithThePreviousRefreshToken() {
        lastResponse = httpTestUtil.executePostWithCookiesOnly(
                REFRESH_ENDPOINT_PATH, new Cookie(REFRESH_TOKEN_COOKIE, previousRefreshTokenCookieValue));
    }

    @Quando("o cliente chamar o endpoint de logout")
    public void whenTheClientCallsLogout() {
        lastResponse = httpTestUtil.executePostWithCookiesOnly(
                LOGOUT_ENDPOINT_PATH,
                new Cookie(ACCESS_TOKEN_COOKIE, accessTokenCookieValue),
                new Cookie(REFRESH_TOKEN_COOKIE, refreshTokenCookieValue));
    }

    @Quando("o cliente chamar o endpoint de refresh com o refresh token revogado pelo logout")
    public void whenTheClientCallsRefreshWithTheRefreshTokenRevokedByLogout() {
        lastResponse = httpTestUtil.executePostWithCookiesOnly(
                REFRESH_ENDPOINT_PATH, new Cookie(REFRESH_TOKEN_COOKIE, refreshTokenCookieValue));
    }

    @Quando("o cliente chamar o endpoint de redefinicao de senha com esse token e uma nova senha")
    public void whenTheClientCallsResetPasswordWithThatTokenAndANewPassword() {
        String jsonBody = String.format("{\"token\":\"%s\",\"new_password\":\"nova-senha-forte-123\"}",
                passwordResetTokenValue);
        lastResponse = httpTestUtil.executePost(RESET_PASSWORD_ENDPOINT_PATH, jsonBody);
    }

    @Quando("o cliente chamar o endpoint de login repetidamente ate abrir o circuit breaker")
    public void whenTheClientCallsLoginRepeatedlyUntilTheCircuitBreakerOpens() {
        for (int attempt = 0; attempt < CIRCUIT_BREAKER_ATTEMPTS; attempt++) {
            performLogin();
        }
    }

    @Entao("o cliente deve receber resposta com status {int}")
    public void thenTheClientReceivesResponseWithStatus(int status) {
        try {
            lastResponse.andExpect(MockMvcResultMatchers.status().is(status));
        } catch (Exception e) {
            throw new AcceptanceTestExecutionException("Falha ao validar status da resposta", e);
        }
    }

    @Entao("a resposta deve conter cookies de acesso e de refresh httpOnly")
    public void thenTheResponseContainsHttpOnlyAccessAndRefreshCookies() {
        List<String> cookieHeaders = lastResponse.andReturn().getResponse().getHeaders(SET_COOKIE_HEADER);
        assertThat(cookieHeaders).hasSize(2);
        assertThat(cookieHeaders).allMatch(header -> header.contains("HttpOnly"));
    }

    @Entao("o access token utilizado deve estar registrado na blacklist do Redis")
    public void thenTheAccessTokenUsedMustBeRegisteredInTheRedisBlacklist() {
        String jti = extractJtiFromAccessToken(accessTokenCookieValue);
        String value = redisTemplate.opsForValue().get(BLACKLIST_KEY_PREFIX + jti);
        assertThat(value).isEqualTo("revoked");
    }

    private void performLogin() {
        String jsonBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", TEST_EMAIL, TEST_PASSWORD);
        lastResponse = httpTestUtil.executePost(LOGIN_ENDPOINT_PATH, jsonBody);
    }

    private void captureSessionCookiesFromLastResponse() {
        Cookie accessCookie = lastResponse.andReturn().getResponse().getCookie(ACCESS_TOKEN_COOKIE);
        Cookie refreshCookie = lastResponse.andReturn().getResponse().getCookie(REFRESH_TOKEN_COOKIE);
        if (accessCookie != null) {
            accessTokenCookieValue = accessCookie.getValue();
        }
        if (refreshCookie != null) {
            refreshTokenCookieValue = refreshCookie.getValue();
        }
    }

    private void stubFindUserById(UUID userId) {
        WireMockSupport.server().stubFor(WireMock.get(
                        WireMock.urlEqualTo(String.format(USER_BY_ID_PATH_TEMPLATE, userId)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(userVerificationJson(userId))));
    }

    private String userVerificationJson(UUID userId) {
        return String.format("{\"id\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}", userId, TEST_EMAIL, TEST_ROLE);
    }

    private String extractJtiFromAccessToken(String accessToken) {
        String[] parts = accessToken.split("\\.");
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        String payload = new String(decoded, StandardCharsets.UTF_8);
        int jtiIndex = payload.indexOf(JTI_JSON_MARKER);
        String afterJti = payload.substring(jtiIndex + JTI_JSON_MARKER.length());
        return afterJti.substring(0, afterJti.indexOf('"'));
    }

}
