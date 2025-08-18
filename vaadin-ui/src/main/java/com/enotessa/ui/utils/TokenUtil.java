package com.enotessa.ui.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.util.Optional;

@Component
public class TokenUtil {
    public void saveTokenToSession(String token) {
        VaadinSession.getCurrent().setAttribute("jwt-token", token);
        System.out.println("Токен сохранён в сессии: " + token);
    }

    public String getSessionJwtToken() {
        return Optional.ofNullable(VaadinSession.getCurrent().getAttribute("jwt-token"))
                .map(Object::toString)
                .orElse(null);
    }

    public String getTokenFromResponse(HttpResponse<String> response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body());
            return jsonNode.get("token").asText();
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Ошибка при обработке токена");
        }
        return null;
    }
}
