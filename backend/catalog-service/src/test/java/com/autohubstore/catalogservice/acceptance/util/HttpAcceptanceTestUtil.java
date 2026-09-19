package com.autohubstore.catalogservice.acceptance.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Component
public class HttpAcceptanceTestUtil {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    public ResultActions executeGet(String path) {
        try {
            return mockMvc.perform(get(path));
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar GET em " + path, ex);
        }
    }

    public ResultActions executePost(String path, Object body) {
        try {
            return mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(body)));
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar POST em " + path, ex);
        }
    }

    public ResultActions executePut(String path, Object body) {
        try {
            return mockMvc.perform(put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(body)));
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar PUT em " + path, ex);
        }
    }

    public ResultActions executeMultipartUpload(String path, List<MockMultipartFile> files) {
        try {
            MockMultipartHttpServletRequestBuilder request = multipart(path);
            files.forEach(request::file);
            return mockMvc.perform(request);
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar upload multipart em " + path, ex);
        }
    }

    public ResultActions executeDelete(String path) {
        try {
            return mockMvc.perform(delete(path));
        }
        catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar DELETE em " + path, ex);
        }
    }

}
