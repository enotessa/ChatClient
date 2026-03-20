package com.enotessa.ui.views;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.enotessa.ui.dto.AuthResponse;
import com.enotessa.ui.dto.RegisterRequestUi;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

@Route("register")
@PageTitle("Регистрация | RealTimeChat")
public class RegisterView extends StyledVerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(RegisterView.class);
    private static final String REGISTER_ENDPOINT = "/auth/register";
    private static final int NOTIFICATION_DURATION_MS = 5000;

    private final TokenUtil tokenUtil;
    private final RequestUtil requestUtil;
    private final String backHost;
    private final String backPort;

    public RegisterView(TokenUtil tokenUtil, RequestUtil requestUtil,
                        @Value("${backChat.host}") String backHost,
                        @Value("${backChat.port}") String backPort) {
        this.tokenUtil = tokenUtil;
        this.requestUtil = requestUtil;
        this.backHost = backHost;
        this.backPort = backPort;

        configureLayout();
        Div formContainer = createFormContainer();
        add(createBackgroundWrapper(), formContainer);
    }

    private void configureLayout() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(false);
        setSpacing(false);
        getStyle().set("position", "relative");
    }

    private Div createBackgroundWrapper() {
        Div background = new Div();
        background.addClassName("simple-background");
        Div backgroundWrapper = new Div(background);
        backgroundWrapper.addClassName("background-wrapper");
        return backgroundWrapper;
    }

    private Div createFormContainer() {
        Div formContainer = new Div();
        formContainer.addClassNames("content-container", "form-container-width");
        HorizontalLayout backButtonLayout = new HorizontalLayout(createBackButton());
        backButtonLayout.setWidthFull();
        backButtonLayout.setPadding(false);
        backButtonLayout.setSpacing(false);

        H1 header = new H1("Создать аккаунт");
        header.addClassName("h1-titler");

        TextField username = new TextField("Имя пользователя");
        TextField email = new TextField("Электронная почта");
        PasswordField password = new PasswordField("Пароль");
        PasswordField confirmPassword = new PasswordField("Подтверждение пароля");

        Div fieldsContainer = new Div(username, email, password, confirmPassword);
        fieldsContainer.addClassName("fields-container");

        Button registerButton = new Button("Зарегистрироваться", e ->
                handleRegistration(username, email, password, confirmPassword));
        registerButton.addClassName("button");

        formContainer.add(backButtonLayout, header, fieldsContainer, registerButton);
        return formContainer;
    }

    private Button createBackButton() {
        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addClassName("round-back-button");
        backButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));
        return backButton;
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
            VaadinSession session = VaadinSession.getCurrent();
            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(
                    requestUtil.buildUri(backHost, backPort, REGISTER_ENDPOINT),
                    requestBody,
                    session
            );
            logger.debug("Отправка запроса на регистрацию: {}", httpRequest.uri());
            sendRequest(HttpClient.newHttpClient(), httpRequest, session);
        } catch (Exception e) {
            logger.error("Ошибка при регистрации: {}", e.getMessage(), e);
            showErrorNotification("Ошибка соединения: " + e.getMessage());
        }
    }

    private void sendRequest(HttpClient client, HttpRequest httpRequest, VaadinSession session) {
        UI ui = UI.getCurrent();
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(response -> ui.access(() -> handleResponse(response, session, ui)))
                .exceptionally(throwable -> {
                    ui.access(() -> {
                        logger.error("Ошибка отправки запроса на регистрацию: {}", throwable.getMessage(), throwable);
                        showErrorNotification("Ошибка регистрации: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void handleResponse(HttpResponse<String> response, VaadinSession session, UI ui) {
        logger.debug("Получен ответ с кодом статуса: {}", response.statusCode());
        if (response.statusCode() == 200) {
            AuthResponse authResponse = tokenUtil.getTokensFromResponse(response);
            if (authResponse != null) {
                tokenUtil.saveAccessTokenToSession(authResponse.getAccessToken(), session, ui);
                tokenUtil.saveRefreshTokenToSession(authResponse.getRefreshToken(), session, ui);
                showSuccessNotification("Регистрация успешна!");
                ui.navigate("chatList");
            } else {
                showErrorNotification("Ошибка обработки ответа сервера");
            }
        } else {
            HandleErrorUtil.handleError(response);
        }
    }

    private boolean validateInputs(TextField username, TextField email,
                                   PasswordField password, PasswordField confirmPassword) {
        if (!arePasswordsEqual(password, confirmPassword)) {
            showErrorNotification("Пароли не совпадают!");
            return false;
        }
        if (areFieldsEmpty(username, email, password)) {
            showErrorNotification("Заполните все поля!");
            return false;
        }
        if (username.getValue().length() < 3) {
            showErrorNotification("Имя пользователя должно содержать минимум 3 символа");
            return false;
        }
        if (!email.getValue().contains("@")) {
            showErrorNotification("Неверный формат электронной почты");
            return false;
        }
        return true;
    }

    private boolean areFieldsEmpty(TextField username, TextField email, PasswordField password) {
        return username.getValue().isEmpty() || email.getValue().isEmpty() || password.getValue().isEmpty();
    }

    private boolean arePasswordsEqual(PasswordField password, PasswordField confirmPassword) {
        return password.getValue().equals(confirmPassword.getValue());
    }

    private void showErrorNotification(String message) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE);
    }

    private void showSuccessNotification(String message) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.TOP_CENTER);
    }
}
