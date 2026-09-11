package com.autohubstore.gateway.acceptance.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.WebTestClient;

@Component
public class HttpTestUtil {

    private static final String CORS_REQUEST_METHOD_HEADER = "Access-Control-Request-Method";

    @Autowired
    private WebTestClient webTestClient;

    public WebTestClient.ResponseSpec executeGetWithCookie(String path, String cookieName, String cookieValue) {
        return webTestClient.get().uri(path).cookie(cookieName, cookieValue).exchange();
    }

    public WebTestClient.ResponseSpec executeGetWithoutCookie(String path) {
        return webTestClient.get().uri(path).exchange();
    }

    public void executeGetRepeatedly(String path, int times) {
        for (int i = 0; i < times; i++) {
            webTestClient.get().uri(path).exchange();
        }
    }

    public WebTestClient.ResponseSpec executeCorsPreflight(String path, String origin) {
        return webTestClient.method(HttpMethod.OPTIONS)
                .uri(path)
                .header(HttpHeaders.ORIGIN, origin)
                .header(CORS_REQUEST_METHOD_HEADER, HttpMethod.GET.name())
                .exchange();
    }

}
