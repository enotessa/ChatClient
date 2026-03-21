package com.enotessa.ui.views;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.enotessa.ui.dto.AuthResponse;
import com.enotessa.ui.dto.LoginRequest;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

@Route("login")
@PageTitle("Вход | RealTimeChat")
public class LoginView extends StyledVerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);
    private static final String LOGIN_ENDPOINT = "/auth/login";
    private static final int NOTIFICATION_DURATION_MS = 5000;

    private final TokenUtil tokenUtil;
    private final RequestUtil requestUtil;
    private final HandleErrorUtil handleErrorUtil;
    private final HttpClient httpClient;

    public LoginView(TokenUtil tokenUtil, RequestUtil requestUtil,
                     HandleErrorUtil handleErrorUtil, HttpClient httpClient) {
        this.tokenUtil = tokenUtil;
        this.requestUtil = requestUtil;
        this.handleErrorUtil = handleErrorUtil;
        this.httpClient = httpClient;

        configureLayout();
        Div formContainer = createFormContainer();
        add(createBackgroundWrapper(), formContainer);
    }

    private void configureLayout() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(false);
        setSpacing(false);
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

        H1 header = new H1("Вход в систему");
        header.addClassName("h1-title");

        TextField username = new TextField("Имя пользователя");
        PasswordField password = new PasswordField("Пароль");

        Div fieldsContainer = new Div(username, password);
        fieldsContainer.addClassName("fields-container");

        Anchor forgotLink = new Anchor("#", "Забыли пароль? Нажмите здесь");
        forgotLink.addClassName("forgot-link");

        Button loginButton = new Button("Войти", e -> handleLogin(username, password));
        loginButton.addClassName("button");

        formContainer.add(backButtonLayout, header, fieldsContainer, forgotLink, loginButton);
        return formContainer;
    }

    private Button createBackButton() {
        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addClassName("round-back-button");
        backButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));
        return backButton;
    }

    private void handleLogin(TextField username, PasswordField password) {
        if (!validateInputs(username, password)) {
            return;
        }
        try {
            LoginRequest request = new LoginRequest(username.getValue(), password.getValue());
            String requestBody = requestUtil.convertToJSON(request);
            VaadinSession session = VaadinSession.getCurrent();
            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(
                    requestUtil.buildUri(LOGIN_ENDPOINT),
                    requestBody,
                    session
            );
            logger.debug("Отправка запроса на вход: {}", httpRequest.uri());
            sendRequest(httpRequest, session);
        } catch (Exception e) {
            logger.error("Ошибка при входе: {}", e.getMessage(), e);
            showErrorNotification("Ошибка подключения: " + e.getMessage());
        }
    }

    private void sendRequest(HttpRequest httpRequest, VaadinSession session) {
        UI ui = UI.getCurrent();
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(response -> ui.access(() -> handleResponse(response, session, ui)))
                .exceptionally(throwable -> {
                    ui.access(() -> {
                        logger.error("Ошибка отправки запроса на вход: {}", throwable.getMessage(), throwable);
                        showErrorNotification("Ошибка входа: " + throwable.getMessage());
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
                showSuccessNotification("Успешный вход!");
                ui.navigate("chatList");
            } else {
                showErrorNotification("Ошибка обработки ответа сервера");
            }
        } else {
            handleErrorUtil.handleError(response);
        }
    }

    private boolean validateInputs(TextField username, PasswordField password) {
        if (username.getValue().isEmpty() || password.getValue().isEmpty()) {
            showErrorNotification("Заполните все поля!");
            return false;
        }
        return true;
    }

    private void showErrorNotification(String message) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE);
    }

    private void showSuccessNotification(String message) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.TOP_CENTER);
    }
}
