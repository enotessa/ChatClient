package com.enotessa.ui.utils;

import com.enotessa.ui.dto.ChatRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

@Component
public class RequestUtil {
    @Autowired
    TokenUtil tokenUtil;

    public String convertToJSON(ChatRequest request) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.writeValueAsString(request);
    }

    public HttpRequest buildPostHttpRequestWithBody(String uri, String requestBody) {
        String token = tokenUtil.getSessionJwtToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpRequest httpRequest = builder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        System.out.println("httpRequest: " + httpRequest.uri().toString());
        return httpRequest;
    }

    public HttpRequest buildDeleteHttpRequest(String uri) {
        String token = tokenUtil.getSessionJwtToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpRequest httpRequest = builder
                .DELETE()
                .build();

        System.out.println("httpRequest: " + httpRequest.uri().toString());
        return httpRequest;
    }


    public String buildUri(String backHost, String backPort, String path) {
        return "http://" + backHost + ":" + backPort + path;
    }
}
