package com.enotessa.ui.utils;

import com.enotessa.ui.dto.ChatRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;

@Component
public class RequestUtil {
    private final HttpRequestBuilder httpRequestBuilder;
    private final TokenUtil tokenUtil;

    public RequestUtil(HttpRequestBuilder httpRequestBuilder, TokenUtil tokenUtil) {
        this.httpRequestBuilder = httpRequestBuilder;
        this.tokenUtil = tokenUtil;
    }

    public String convertToJSON(ChatRequest request) throws JsonProcessingException {
        return httpRequestBuilder.convertToJSON(request);
    }

    public HttpRequest buildPostHttpRequestWithBody(String uri, String requestBody, VaadinSession session) {
        String token = tokenUtil.getAccessSessionJwtToken(session);
        return httpRequestBuilder.buildPostHttpRequestWithBody(uri, requestBody, token);
    }

    public HttpRequest buildDeleteHttpRequest(String uri, VaadinSession session) {
        String token = tokenUtil.getAccessSessionJwtToken(session);
        return httpRequestBuilder.buildDeleteHttpRequest(uri, token);
    }

    public String buildUri(String path) {
        return httpRequestBuilder.buildUri(path);
    }
}