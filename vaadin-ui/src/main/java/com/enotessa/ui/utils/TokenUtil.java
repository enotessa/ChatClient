package com.enotessa.ui.utils;

import com.enotessa.ui.dto.AuthResponse;
import com.enotessa.ui.dto.RefreshRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(TokenUtil.class);
    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String REFRESH_TOKEN_KEY = "refreshToken";
    private static final String REFRESH_ENDPOINT = "/auth/refresh";
    private static final int REQUEST_TIMEOUT_SECONDS = 10;
    private static final int NOTIFICATION_DURATION_MS = 5000;

    private final HttpRequestBuilder httpRequestBuilder;
    private final String backHost;
    private final String backPort;

    public TokenUtil(HttpRequestBuilder httpRequestBuilder,
                     @Value("${backChat.host}") String backHost,
                     @Value("${backChat.port}") String backPort) {
        this.httpRequestBuilder = httpRequestBuilder;
        this.backHost = backHost;
        this.backPort = backPort;
    }

    public CompletableFuture<Boolean> isTokenInvalidOrNonRefreshable(VaadinSession session) {
        return hasValidAccessToken(session)
                .thenCompose(valid -> valid ? CompletableFuture.completedFuture(false) : attemptTokenRefresh(session));
    }

    public void saveAccessTokenToSession(String token, VaadinSession session, UI ui) {
        saveTokenToSession(ACCESS_TOKEN_KEY, token, "access", session, ui);
    }

    public void saveRefreshTokenToSession(String token, VaadinSession session, UI ui) {
        saveTokenToSession(REFRESH_TOKEN_KEY, token, "refresh", session, ui);
    }

    public String getAccessSessionJwtToken(VaadinSession session) {
        return getTokenFromSession(ACCESS_TOKEN_KEY, session);
    }

    public String getRefreshSessionJwtToken(VaadinSession session) {
        return getTokenFromSession(REFRESH_TOKEN_KEY, session);
    }

    public AuthResponse getTokensFromResponse(HttpResponse<String> response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body());
            if (!jsonNode.hasNonNull("accessToken") || !jsonNode.hasNonNull("refreshToken")) {
                logger.error("Ответ сервера не содержит accessToken или refreshToken: {}", response.body());
                showErrorNotification("Некорректный ответ сервера с токенами");
                return null;
            }
            String accessToken = jsonNode.get("accessToken").asText();
            String refreshToken = jsonNode.get("refreshToken").asText();
            return new AuthResponse(accessToken, refreshToken);
        } catch (JsonProcessingException e) {
            logger.error("Ошибка парсинга ответа с токенами: {}", e.getMessage(), e);
            showErrorNotification("Ошибка обработки ответа сервера");
            return null;
        }
    }

    private CompletableFuture<Boolean> hasValidAccessToken(VaadinSession session) {
        String accessToken = getAccessSessionJwtToken(session);
        return CompletableFuture.completedFuture(accessToken != null && !isTokenExpired(accessToken));
    }

    private CompletableFuture<Boolean> attemptTokenRefresh(VaadinSession session) {
        String refreshToken = getRefreshSessionJwtToken(session);
        if (refreshToken == null || isTokenExpired(refreshToken)) {
            logger.warn("Отсутствует действительный refresh-токен");
            return CompletableFuture.completedFuture(true);
        }
        return refreshAccessToken(refreshToken, session);
    }

    private CompletableFuture<Boolean> refreshAccessToken(String refreshToken, VaadinSession session) {
        try {
            RefreshRequest request = new RefreshRequest(refreshToken);
            String requestBody = httpRequestBuilder.convertToJSON(request);
            String uri = httpRequestBuilder.buildUri(backHost, backPort, REFRESH_ENDPOINT);
            HttpRequest httpRequest = httpRequestBuilder.buildPostHttpRequestWithBody(uri, requestBody, null);
            UI ui = UI.getCurrent();
            return sendRequest(HttpClient.newHttpClient(), httpRequest, session, ui);
        } catch (JsonProcessingException e) {
            logger.error("Ошибка подготовки запроса на обновление токена: {}", e.getMessage(), e);
            showErrorNotification("Ошибка подготовки запроса на обновление токена");
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> sendRequest(HttpClient client, HttpRequest httpRequest, VaadinSession session, UI ui) {
        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (ui != null) {
                        boolean[] result = {false};
                        ui.accessSynchronously(() -> result[0] = handleResponse(response, session));
                        return result[0];
                    }
                    logger.warn("UI недоступен для обработки ответа на обновление токена");
                    return false;
                })
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка отправки запроса на обновление токена: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка обновления токена: " + throwable.getMessage());
                        });
                    } else {
                        logger.error("UI недоступен, ошибка отправки запроса на обновление токена: {}", throwable.getMessage(), throwable);
                    }
                    return false;
                });
    }

    private boolean handleResponse(HttpResponse<String> response, VaadinSession session) {
        if (response.statusCode() == 200) {
            AuthResponse authResponse = getTokensFromResponse(response);
            if (authResponse != null) {
                UI ui = UI.getCurrent();
                if (ui != null) {
                    saveAccessTokenToSession(authResponse.getAccessToken(), session, ui);
                    saveRefreshTokenToSession(authResponse.getRefreshToken(), session, ui);
                    return true;
                }
                logger.warn("UI недоступен при сохранении токенов");
                return false;
            }
        }
        HandleErrorUtil.handleError(response);
        return false;
    }

    private void saveTokenToSession(String key, String token, String tokenType, VaadinSession session, UI ui) {
        if (token == null || token.isBlank()) {
            logger.warn("Попытка сохранить пустой или null {} токен", tokenType);
            return;
        }
        if (session == null) {
            logger.warn("VaadinSession равен null при сохранении {} токена", tokenType);
            return;
        }
        session.setAttribute(key, token);
        logger.debug("{} токен сохранен в сессии", tokenType);
    }

    private String getTokenFromSession(String key, VaadinSession session) {
        if (session == null) {
            logger.warn("VaadinSession равен null при получении {} токена", key);
            return null;
        }
        return Optional.ofNullable(session.getAttribute(key))
                .map(Object::toString)
                .orElse(null);
    }

    private void showErrorNotification(String message) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE));
        } else {
            logger.warn("UI недоступен для показа уведомления: {}", message);
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                logger.warn("Неверный формат JWT-токена");
                return true;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = mapper.readTree(payloadJson);
            if (!payload.hasNonNull("exp")) {
                logger.warn("Токен не содержит поле срока действия");
                return true;
            }
            long exp = payload.get("exp").asLong() * 1000;
            return exp < System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("Ошибка декодирования токена: {}", e.getMessage(), e);
            return true;
        }
    }
}
