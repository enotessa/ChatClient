package com.enotessa.ui.utils;

import com.enotessa.ui.dto.ChatRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;

public class RequestUtil {

    public static String convertToJSON(ChatRequest request) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(request);
    }

    public static HttpRequest buildHttpRequest(String uri, String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    public static String buildUri(String backHost, String backPort, String path) {
        return "http://" + backHost + ":" + backPort + path;
    }
}
