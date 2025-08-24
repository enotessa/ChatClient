package com.enotessa.ui.views;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.enotessa.ui.dto.RegisterRequestUi;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Route("register")
@PageTitle("Register | RealTimeChat")
public class RegisterView extends StyledVerticalLayout {
    @Value("${backChat.host}")
    private String backHost;
    @Value("${backChat.port}")
    private String backPort;

    @Autowired
    TokenUtil tokenUtil;
    @Autowired
    RequestUtil requestUtil;

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
            RegisterRequestUi request = new RegisterRequestUi(
                    username.getValue(),
                    email.getValue(),
                    password.getValue()
            );

            String requestBody = requestUtil.convertToJSON(request);
            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(requestUtil.buildUri(backHost, backPort, "/auth/register"), requestBody);

            System.out.println("Sending uri: " + httpRequest.uri());

            HttpClient client = HttpClient.newHttpClient();
            sendRequest(client, httpRequest);

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Ошибка соединения: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void sendRequest(HttpClient client, HttpRequest httpRequest) {
        UI ui = UI.getCurrent();
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Response status: " + response.statusCode());
                    ui.access(() -> {
                        System.out.println("Response body: " + response.body());
                        if (response.statusCode() == 200) {
                            String token = tokenUtil.getTokenFromResponse(response);
                            tokenUtil.saveTokenToSession(token);

                            Notification.show("Регистрация успешна!");
                            ui.navigate("chatList");

                        } else {
                            HandleErrorUtil.handleError(response);
                        }
                    });
                });
    }

    private boolean validateInputs(TextField username, TextField email,
                                   PasswordField password, PasswordField confirmPassword) {

        if (!arePasswordsEqual(password, confirmPassword)) {
            Notification.show("Passwords don't match!", 3000, Notification.Position.MIDDLE);
            return false;
        }

        if (areFieldsFilledIn(username, email, password)) {
            Notification.show("Please fill all fields!", 3000, Notification.Position.MIDDLE);
            return false;
        }

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

    private boolean areFieldsFilledIn(TextField username, TextField email, PasswordField password) {
        return username.getValue().isEmpty() || email.getValue().isEmpty() ||
                password.getValue().isEmpty();
    }

    private boolean arePasswordsEqual(PasswordField password, PasswordField confirmPassword) {
        return password.getValue().equals(confirmPassword.getValue());
    }
}