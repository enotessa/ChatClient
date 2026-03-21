package com.enotessa.ui.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.notification.Notification;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.util.Map;

@Component
public class HandleErrorUtil {
    private final ObjectMapper objectMapper;

    public HandleErrorUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handleError(HttpResponse<String> response) {
        try {
            Map<String, String> errorMap = objectMapper.readValue(response.body(), new TypeReference<>() {});
            String errorMessage = errorMap.getOrDefault("error", "Неизвестная ошибка");
            Notification.show("Ошибка: " + errorMessage, 5000, Notification.Position.MIDDLE);
        } catch (JsonProcessingException e) {
            Notification.show("Ошибка при обработке ответа: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}
