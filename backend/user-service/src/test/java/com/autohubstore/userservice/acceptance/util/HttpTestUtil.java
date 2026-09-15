package com.autohubstore.userservice.acceptance.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Component
public class HttpTestUtil {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    public ResultActions executePost(String path, Object body) {
        try {
            return mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar POST em " + path, ex);
        }
    }

    public ResultActions executePut(String path, Object body) {
        try {
            return mockMvc.perform(put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar PUT em " + path, ex);
        }
    }

    public ResultActions executeGet(String path) {
        try {
            return mockMvc.perform(get(path));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar GET em " + path, ex);
        }
    }

    public ResultActions executeDelete(String path) {
        try {
            return mockMvc.perform(delete(path));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar DELETE em " + path, ex);
        }
    }

}
