package com.enotessa.ui.utils;

import com.enotessa.ui.configurations.BackendProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

@Component
public class HttpRequestBuilder {
    private final ObjectMapper objectMapper;
    private final BackendProperties backendProperties;

    public HttpRequestBuilder(ObjectMapper objectMapper, BackendProperties backendProperties) {
        this.objectMapper = objectMapper;
        this.backendProperties = backendProperties;
    }

    public String convertToJSON(Object request) throws JsonProcessingException {
        return objectMapper.writeValueAsString(request);
    }

    public HttpRequest buildPostHttpRequestWithBody(String uri, String requestBody, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
    }

    public HttpRequest buildDeleteHttpRequest(String uri, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.DELETE().build();
    }

    public String buildUri(String path) {
        return "http://" + backendProperties.getHost() + ":" + backendProperties.getPort() + path;
    }
}
