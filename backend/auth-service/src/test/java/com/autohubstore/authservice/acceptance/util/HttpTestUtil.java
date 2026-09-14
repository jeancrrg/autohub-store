package com.autohubstore.authservice.acceptance.util;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Component
public class HttpTestUtil {

    @Autowired
    private MockMvc mockMvc;

    public ResultActions executePost(String path, String jsonBody) {
        try {
            return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(jsonBody));
        } catch (Exception e) {
            throw new AcceptanceTestExecutionException("Falha ao executar POST de teste em " + path, e);
        }
    }

    public ResultActions executePostWithCookiesOnly(String path, Cookie... cookies) {
        try {
            return mockMvc.perform(post(path).cookie(cookies));
        } catch (Exception e) {
            throw new AcceptanceTestExecutionException("Falha ao executar POST com cookies de teste em " + path, e);
        }
    }

}
