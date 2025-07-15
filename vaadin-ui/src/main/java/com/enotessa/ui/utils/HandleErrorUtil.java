package com.enotessa.ui.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.notification.Notification;

import java.net.http.HttpResponse;
import java.util.Map;

public class HandleErrorUtil {
    public static void handleError(HttpResponse<String> response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> errorMap = mapper.readValue(response.body(), new TypeReference<>() {});
            String errorMessage = errorMap.getOrDefault("error", "Неизвестная ошибка");
            Notification.show("Ошибка: " + errorMessage, 5000, Notification.Position.MIDDLE);
        } catch (JsonProcessingException e) {
            Notification.show("Ошибка при обработке ответа: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}
