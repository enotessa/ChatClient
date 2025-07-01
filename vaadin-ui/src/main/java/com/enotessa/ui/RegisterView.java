package com.enotessa.ui;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.enotessa.ui.dto.RegisterRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Route("register")
@PageTitle("Register | RealTimeChat")
public class RegisterView extends StyledVerticalLayout {

    public RegisterView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(false);
        setSpacing(false);

        // Фоновый слой
        Div background = new Div();
        background.addClassName("simple-background");

        Div backgroundWrapper = new Div(background);
        background.addClassName("background-wrapper");

        // Контент поверх
        getStyle().set("position", "relative");

        // Основной контейнер формы
        Div formContainer = new Div();
        formContainer.addClassNames("content-container", "form-container-width");

        // Заголовок (центрирован)
        H1 header = new H1("Create Account");
        header.addClassName("h1-titler");

        // Поля формы (прижаты к левому краю)
        TextField username = new TextField("Username");
        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Password");
        PasswordField confirmPassword = new PasswordField("Confirm Password");

        // Контейнер для полей (для левого выравнивания)
        Div fieldsContainer = new Div(username, email, password, confirmPassword);
        fieldsContainer.addClassName("fields-container");

        // Кнопка (центрирована, ширина по содержимому)
        Button registerButton = new Button("Register", e ->
                handleRegistration(username, email, password, confirmPassword));
        registerButton.addClassName("button");

        formContainer.add(header, fieldsContainer, registerButton);
        add(backgroundWrapper, formContainer);
        setSizeFull();
    }

    private void handleRegistration(TextField username, TextField email,
                                    PasswordField password, PasswordField confirmPassword) {
        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }
        try {
            RegisterRequest request = new RegisterRequest(
                    username.getValue(),
                    email.getValue(),
                    password.getValue()
            );

            // Конвертируем в JSON
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(request);

            // Формируем HTTP-запрос
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8082/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            System.out.println("Sending uri: " + httpRequest.uri());
            System.out.println("Sending request: " + requestBody);
            UI ui = UI.getCurrent();
            // Отправляем асинхронно
            client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("📥 Response status: " + response.statusCode());
                        ui.access(() -> {
                            System.out.println("📥 Response body: " + response.body());
                            if (response.statusCode() == 200) {
                                Notification.show("Регистрация успешна!");
                            } else {
                                try {
                                    Map<String, String> errorMap = mapper.readValue(response.body(), new TypeReference<>() {});
                                    String errorMessage = errorMap.getOrDefault("error", "Неизвестная ошибка");
                                    Notification.show("Ошибка: " + errorMessage, 5000, Notification.Position.MIDDLE);
                                } catch (JsonProcessingException e) {
                                    Notification.show("Ошибка при обработке ответа: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                                }
                            }
                        });
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Ошибка соединения: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private boolean validateInputs(TextField username, TextField email,
                                   PasswordField password, PasswordField confirmPassword) {
        // Проверка совпадения паролей
        if (!password.getValue().equals(confirmPassword.getValue())) {
            Notification.show("Passwords don't match!", 3000, Notification.Position.MIDDLE);
            return false;
        }

        // Проверка заполненности полей
        if (username.getValue().isEmpty() || email.getValue().isEmpty() ||
                password.getValue().isEmpty()) {
            Notification.show("Please fill all fields!", 3000, Notification.Position.MIDDLE);
            return false;
        }

        // Дополнительные проверки
        if (username.getValue().length() < 3) {
            Notification.show("Username must be at least 3 characters", 3000, Notification.Position.MIDDLE);
            return false;
        }

        if (!email.getValue().contains("@")) {
            Notification.show("Invalid email format", 3000, Notification.Position.MIDDLE);
            return false;
        }

        return true;
    }
}