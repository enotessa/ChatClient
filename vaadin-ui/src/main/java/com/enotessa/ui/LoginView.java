package com.enotessa.ui;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.enotessa.ui.dto.LoginRequest;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
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

@Route("login")
@PageTitle("Login | RealTimeChat")
public class LoginView extends StyledVerticalLayout {
    @Value("${backChat.host}")
    private String backHost;
    @Value("${backChat.port}")
    private String backPort;

    @Autowired
    TokenUtil tokenUtil;
    @Autowired
    RequestUtil requestUtil;

    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(false);
        setSpacing(false);

        // Фоновый слой
        Div background = new Div();
        background.addClassName("simple-background");

        Div backgroundWrapper = new Div(background);
        background.addClassName("background-wrapper");

        // Основной контейнер формы
        Div formContainer = new Div();
        formContainer.addClassNames("content-container", "form-container-width");

        // Заголовок
        H1 header = new H1("Sign In Form");
        header.addClassName("h1-title");

        // Поля ввода
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");

        // Контейнер для полей (для левого выравнивания)
        Div fieldsContainer = new Div(username, password);
        fieldsContainer.addClassName("fields-container");

        // Ссылка "Forgot password"
        Anchor forgotLink = new Anchor("#", "Forgot your password? Click here");
        forgotLink.addClassName("forgot-link");

        // Кнопка входа
        Button loginButton = new Button(("Sign In"), e ->
                handleLogin(username, password));
        loginButton.addClassName("button");

        formContainer.add(header, fieldsContainer, forgotLink, loginButton);
        add(backgroundWrapper, formContainer);
        setSizeFull();
    }

    private void handleLogin(TextField username, PasswordField password) {
        if (username.getValue().isEmpty() || password.getValue().isEmpty()) {
            Notification.show("Please fill in all fields", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            LoginRequest request = new LoginRequest(
                    username.getValue(),
                    password.getValue()
            );

            String requestBody = requestUtil.convertToJSON(request);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = requestUtil.buildHttpRequest(requestUtil.buildUri(backHost, backPort, "/api/auth/login"), requestBody);

            sendRequest(client, httpRequest);

        } catch (Exception e) {
            Notification.show("Ошибка подключения: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void sendRequest(HttpClient client, HttpRequest httpRequest) {
        UI ui = UI.getCurrent();
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    ui.access(() -> {
                        if (response.statusCode() == 200) {
                            tokenUtil.saveTokenToSession(response);

                            Notification.show("Успешный вход!");
                            UI.getCurrent().navigate("chatList");
                        } else {
                            HandleErrorUtil.handleError(response);
                        }
                    });
                });
    }
}