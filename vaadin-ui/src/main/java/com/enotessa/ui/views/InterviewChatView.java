package com.enotessa.ui.views;

import com.enotessa.ui.dto.MessageDto;
import com.enotessa.ui.dto.ProfessionalPositionDto;
import com.enotessa.ui.enums.ProfessionEnum;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Route("interviewChat")
@PageTitle("Собеседование | Чат")
@CssImport("./styles/views/chat-view.css")
@Component
public class InterviewChatView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger logger = LoggerFactory.getLogger(InterviewChatView.class);
    private static final String INTERVIEW_URL_PATH = "/interview/message";
    private static final String PROFESSION_URL_PATH = "/interview/interviewProfession";
    private static final String DELETE_MESSAGES_URL_PATH = "/interview/deleteMessages";
    private static final String FIRST_DEFAULT_MESSAGE = """
            Здравствуйте! Давайте начнем собеседование.
            \nЕсли захотите начать собеседование сначала, напишите \"заново\".
            \nЕсли хотите, чтобы я задал следующий вопрос, напишите \"дальше\" 
            """;
    private static final int NOTIFICATION_DURATION_MS = 5000;

    private HorizontalLayout headerLayout;
    private ComboBox<String> optionsMenu;
    private Button backButton;
    private Div messagesContainer;
    private Div chatContainer;
    private TextField messageField;
    private Button sendButton;
    private HorizontalLayout inputLayout;
    private Button micButton;

    private final RequestUtil requestUtil;
    private final TokenUtil tokenUtil;
    private final String backHost;
    private final String backPort;

    public InterviewChatView(RequestUtil requestUtil, TokenUtil tokenUtil,
                             @Value("${backChat.host}") String backHost,
                             @Value("${backChat.port}") String backPort) {
        this.requestUtil = requestUtil;
        this.tokenUtil = tokenUtil;
        this.backHost = backHost;
        this.backPort = backPort;
        configureMainLayout();
        configureHeader();
        configureMessagesArea();
        chatContainer.add(headerLayout, messagesContainer);
        configureInputArea();
        add(chatContainer, inputLayout);
        expand(chatContainer);
    }

    @PostConstruct
    private void initialize() {
        changeProfessionalPosition(ProfessionEnum.JAVA_MIDDLE.getDisplayName());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        applyFadingEffect();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinSession session = VaadinSession.getCurrent();
        UI ui = UI.getCurrent();
        if (ui == null || session == null) {
            logger.warn("UI или сессия недоступны при входе в InterviewChatView");
            showErrorNotification("Ошибка: UI или сессия недоступны");
            event.forwardTo("");
            return;
        }
        tokenUtil.isTokenInvalidOrNonRefreshable(session)
                .thenAccept(invalid -> ui.access(() -> {
                    if (invalid) {
                        logger.warn("Токен недействителен или не может быть обновлен, перенаправление на логин");
                        showErrorNotification("Токен недействителен, пожалуйста, войдите снова");
                        event.forwardTo("");
                    }
                }))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка проверки токена: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка проверки токена: " + throwable.getMessage());
                            event.forwardTo("");
                        });
                    } else {
                        logger.error("UI недоступен, ошибка проверки токена: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void configureMainLayout() {
        addClassName("chat-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        chatContainer = new Div();
        chatContainer.addClassNames("chat-background");
    }

    private void configureHeader() {
        createBackButton();
        createProfessionDropdown();
        createHeader();
    }

    private void createHeader() {
        headerLayout = new HorizontalLayout(backButton, optionsMenu);
        headerLayout.addClassName("chat-header");
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
    }

    private void createBackButton() {
        backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addClassName("round-back-button");
        backButton.addClickListener(e -> {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.navigate("chatList");
            } else {
                logger.warn("UI недоступен при попытке навигации назад");
                showErrorNotification("Ошибка: UI недоступен");
            }
        });
    }

    private void createProfessionDropdown() {
        optionsMenu = new ComboBox<>();
        optionsMenu.addClassName("header-dropdown");
        optionsMenu.setItems(Arrays.stream(ProfessionEnum.values())
                .map(ProfessionEnum::getDisplayName)
                .collect(Collectors.toList()));
        optionsMenu.setPlaceholder(ProfessionEnum.JAVA_MIDDLE.getDisplayName());
        optionsMenu.addValueChangeListener(event -> {
            String selected = event.getValue();
            if (selected != null) {
                changeProfessionalPosition(selected);
            }
        });
    }

    private void configureMessagesArea() {
        messagesContainer = new Div();
        messagesContainer.addClassName("messages-container");
        messagesContainer.add(createMessage("HR", FIRST_DEFAULT_MESSAGE, false));
    }

    private void configureInputArea() {
        createMessageField();
        createSendButton();
        createMicButton();
        createInputLayout();
    }

    private void createMessageField() {
        messageField = new TextField();
        messageField.addClassName("message-input");
        messageField.setPlaceholder("Введите сообщение...");
        messageField.setValueChangeMode(ValueChangeMode.EAGER);
        messageField.addKeyPressListener(Key.ENTER, e -> actionSelection(messageField));
    }

    private void createSendButton() {
        sendButton = new Button(VaadinIcon.PAPERPLANE.create());
        sendButton.addClassName("send-button");
        sendButton.addClickListener(e -> actionSelection(messageField));
    }

    private void createMicButton() {
        micButton = new Button(VaadinIcon.MICROPHONE.create());
        micButton.addClassName("send-button");
        micButton.addClickListener(e -> startSpeechRecognition());
    }

    private void createInputLayout() {
        inputLayout = new HorizontalLayout(messageField, micButton, sendButton);
        inputLayout.addClassName("input-layout");
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, messageField);
    }

    private void applyFadingEffect() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            executeJsCodeForTheEffectOfFadingMessages();
        } else {
            logger.warn("UI недоступен при попытке применить эффект затухания");
            showErrorNotification("Ошибка: UI недоступен для эффекта затухания");
        }
    }

    private Div createMessage(String sender, String text, boolean isCurrentUser) {
        Div messageWrapper = createMessageWrapper(isCurrentUser);
        Div messageContent = createMessageContent(sender, text, isCurrentUser);
        messageWrapper.add(messageContent);
        return messageWrapper;
    }

    private Div createMessageWrapper(boolean isCurrentUser) {
        Div wrapper = new Div();
        wrapper.addClassName(isCurrentUser ? "message-wrapper-current" : "message-wrapper-other");
        return wrapper;
    }

    private Div createMessageContent(String sender, String text, boolean isCurrentUser) {
        Div message = new Div();
        message.addClassName("message");
        if (!isCurrentUser) {
            message.add(createSenderLabel(sender));
        }
        Div messageBubble = createMessageBubble(text, isCurrentUser);
        message.add(messageBubble);
        return message;
    }

    private Span createSenderLabel(String sender) {
        Span senderSpan = new Span(sender + ":");
        senderSpan.addClassName("message-sender");
        return senderSpan;
    }

    private Div createMessageBubble(String text, boolean isCurrentUser) {
        Div bubble = new Div();
        bubble.addClassName(isCurrentUser ? "bubble-current" : "bubble-other");
        bubble.setText(text);
        return bubble;
    }

    private void actionSelection(TextField messageField) {
        String text = messageField.getValue();
        if (text.isBlank()) {
            showErrorNotification("Сообщение пустое");
        } else if (text.equals("заново")) {
            resetMessages();
        } else {
            sendMessage(text);
        }
    }

    private void resetMessages() {
        VaadinSession session = VaadinSession.getCurrent();
        UI ui = UI.getCurrent();
        if (ui == null || session == null) {
            logger.warn("UI или сессия недоступны при сбросе сообщений");
            showErrorNotification("Ошибка: UI или сессия недоступны");
            if (ui != null) {
                ui.navigate("");
            }
            return;
        }
        tokenUtil.isTokenInvalidOrNonRefreshable(session)
                .thenAccept(invalid -> ui.access(() -> {
                    if (invalid) {
                        logger.warn("Токен недействителен или не может быть обновлен, перенаправление на логин");
                        showErrorNotification("Токен недействителен, пожалуйста, войдите снова");
                        ui.navigate("");
                    } else {
                        HttpRequest httpRequest = requestUtil.buildDeleteHttpRequest(
                                requestUtil.buildUri(backHost, backPort, DELETE_MESSAGES_URL_PATH), session);
                        deleteMessagesRequest(HttpClient.newHttpClient(), httpRequest, ui);
                    }
                }))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка проверки токена: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка проверки токена: " + throwable.getMessage());
                            ui.navigate("");
                        });
                    } else {
                        logger.error("UI недоступен, ошибка проверки токена: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void deleteMessagesRequest(HttpClient client, HttpRequest httpRequest, UI ui) {
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(response -> ui.access(() -> handleDeleteMessagesResponse(response, ui)))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка удаления сообщений: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка удаления сообщений: " + throwable.getMessage());
                        });
                    } else {
                        logger.error("UI недоступен, ошибка удаления сообщений: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void handleDeleteMessagesResponse(HttpResponse<String> response, UI ui) {
        if (response.statusCode() == 200) {
            deleteMessagesFromUI();
            addMessageToUI("HR", FIRST_DEFAULT_MESSAGE, false);
            safePush(ui);
        } else {
            HandleErrorUtil.handleError(response);
        }
    }

    private void startSpeechRecognition() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(
                    """
                            if (!('webkitSpeechRecognition' in window)) {
                                alert('Распознавание речи не поддерживается в этом браузере');
                            } else {
                                const recognition = new webkitSpeechRecognition();
                                recognition.lang = 'ru-RU';
                                recognition.interimResults = false;
                                recognition.maxAlternatives = 1;
                            
                                recognition.onresult = function(event) {
                                    const text = event.results[0][0].transcript;
                                    const field = document.querySelector('.message-input input');
                                    if (field && field.value && field.value.trim() !== '') {
                                        field.value += ' ' + text;
                                    } else if (field) {
                                        field.value = text;
                                    }
                                    if (field) {
                                        field.dispatchEvent(new Event('input', { bubbles: true }));
                                        field.dispatchEvent(new Event('change', { bubbles: true }));
                                    }
                                };
                            
                                recognition.onerror = function(event) {
                                    console.error('Speech recognition error:', event.error);
                                };
                            
                                recognition.start();
                            }
                            """
            );
        } else {
            logger.warn("UI недоступен при попытке запустить распознавание речи");
            showErrorNotification("Ошибка: UI недоступен для распознавания речи");
        }
    }

    private void sendMessage(String textMessage) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            logger.warn("UI недоступен при отправке сообщения");
            showErrorNotification("Ошибка: UI недоступен");
            return;
        }
        addMessageToUI("me", textMessage, true);
        messageField.clear();
        sendMessageToServer(new MessageDto("me", textMessage, LocalDateTime.now()));
        scrollMessagesToBottom(ui);
    }

    private void deleteMessagesFromUI() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> messagesContainer.removeAll());
        } else {
            logger.warn("UI недоступен при удалении сообщений из UI");
        }
    }

    private void addMessageToUI(String sender, String text, boolean isCurrentUser) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> messagesContainer.add(createMessage(sender, text, isCurrentUser)));
        } else {
            logger.warn("UI недоступен при добавлении сообщения в UI");
        }
    }

    private void sendMessageToServer(MessageDto messageDto) {
        VaadinSession session = VaadinSession.getCurrent();
        UI ui = UI.getCurrent();
        if (ui == null || session == null) {
            logger.warn("UI или сессия недоступны при отправке сообщения на сервер");
            showErrorNotification("Ошибка: UI или сессия недоступны");
            if (ui != null) {
                ui.navigate("");
            }
            return;
        }
        tokenUtil.isTokenInvalidOrNonRefreshable(session)
                .thenAccept(invalid -> ui.access(() -> {
                    if (invalid) {
                        logger.warn("Токен недействителен или не может быть обновлен, перенаправление на логин");
                        showErrorNotification("Токен недействителен, пожалуйста, войдите снова");
                        ui.navigate("");
                    } else {
                        try {
                            String requestBody = requestUtil.convertToJSON(messageDto);
                            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(
                                    requestUtil.buildUri(backHost, backPort, INTERVIEW_URL_PATH),
                                    requestBody, session
                            );
                            sendMessageRequest(HttpClient.newHttpClient(), httpRequest, ui);
                        } catch (JsonProcessingException e) {
                            logger.error("Ошибка отправки сообщения на сервер: {}", e.getMessage(), e);
                            showErrorNotification("Ошибка подключения: " + e.getMessage());
                        }
                    }
                }))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка проверки токена: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка проверки токена: " + throwable.getMessage());
                            ui.navigate("");
                        });
                    } else {
                        logger.error("UI недоступен, ошибка проверки токена: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void sendMessageRequest(HttpClient client, HttpRequest httpRequest, UI ui) {
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(response -> ui.access(() -> handleServerResponse(response, ui)))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка отправки сообщения на сервер: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка: " + throwable.getMessage());
                        });
                    } else {
                        logger.error("UI недоступен, ошибка отправки сообщения на сервер: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void handleServerResponse(HttpResponse<String> response, UI ui) {
        if (response.statusCode() == 200) {
            try {
                String message = new ObjectMapper()
                        .readTree(response.body())
                        .get("message")
                        .asText();
                addMessageToUI("HR", message, false);
                scrollMessagesToBottom(ui);
                safePush(ui);
            } catch (JsonProcessingException e) {
                logger.error("Ошибка обработки ответа сервера: {}", e.getMessage(), e);
                showErrorNotification("Ошибка при обработке ответа");
            }
        } else {
            HandleErrorUtil.handleError(response);
        }
    }

    private void scrollMessagesToBottom(UI ui) {
        if (ui != null) {
            ui.access(() -> ui.getPage().executeJs(
                    "const container = $0; container.scrollTop = container.scrollHeight;",
                    messagesContainer.getElement()
            ));
        } else {
            logger.warn("UI недоступен при попытке прокрутки сообщений");
        }
    }

    private void changeProfessionalPosition(String selected) {
        VaadinSession session = VaadinSession.getCurrent();
        UI ui = UI.getCurrent();
        if (ui == null || session == null) {
            logger.warn("UI или сессия недоступны при изменении профессиональной позиции");
            showErrorNotification("Ошибка: UI или сессия недоступны");
            if (ui != null) {
                ui.navigate("");
            }
            return;
        }
        tokenUtil.isTokenInvalidOrNonRefreshable(session)
                .thenAccept(invalid -> ui.access(() -> {
                    if (invalid) {
                        logger.warn("Токен недействителен или не может быть обновлен, перенаправление на логин");
                        showErrorNotification("Токен недействителен, пожалуйста, войдите снова");
                        ui.navigate("");
                    } else {
                        try {
                            String requestBody = requestUtil.convertToJSON(new ProfessionalPositionDto(selected));
                            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(
                                    requestUtil.buildUri(backHost, backPort, PROFESSION_URL_PATH),
                                    requestBody, session
                            );
                            sendProfessionRequest(HttpClient.newHttpClient(), httpRequest, ui);
                        } catch (JsonProcessingException e) {
                            logger.error("Ошибка изменения профессиональной позиции: {}", e.getMessage(), e);
                            showErrorNotification("Ошибка изменения позиции: " + e.getMessage());
                        }
                    }
                }))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка проверки токена: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка проверки токена: " + throwable.getMessage());
                            ui.navigate("");
                        });
                    } else {
                        logger.error("UI недоступен, ошибка проверки токена: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void sendProfessionRequest(HttpClient client, HttpRequest httpRequest, UI ui) {
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(response -> ui.access(() -> handleProfessionResponse(response, ui)))
                .exceptionally(throwable -> {
                    if (ui != null) {
                        ui.access(() -> {
                            logger.error("Ошибка отправки запроса на изменение позиции: {}", throwable.getMessage(), throwable);
                            showErrorNotification("Ошибка: " + throwable.getMessage());
                        });
                    } else {
                        logger.error("UI недоступен, ошибка отправки запроса на изменение позиции: {}", throwable.getMessage(), throwable);
                    }
                    return null;
                });
    }

    private void handleProfessionResponse(HttpResponse<String> response, UI ui) {
        if (response.statusCode() == 200) {
            deleteMessagesFromUI();
            addMessageToUI("HR", FIRST_DEFAULT_MESSAGE, false);
            showErrorNotification("Вы выбрали собеседование на позицию " + optionsMenu.getValue());
            safePush(ui);
        } else {
            HandleErrorUtil.handleError(response);
        }
    }

    private void executeJsCodeForTheEffectOfFadingMessages() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(
                    """
                            const container = $0;
                            function updateMessageOpacity() {
                                const messages = container.querySelectorAll('.message-wrapper-current, .message-wrapper-other');
                                const scrollTop = container.scrollTop;
                                const fadeHeight = 150;
                                messages.forEach(message => {
                                    if (scrollTop === 0) {
                                        message.style.opacity = 1;
                                    } else {
                                        const messageTop = message.getBoundingClientRect().top - container.getBoundingClientRect().top;
                                        const messageBottom = messageTop + message.clientHeight;
                                        if (messageBottom < fadeHeight) {
                                            const opacity = messageBottom / fadeHeight;
                                            message.style.opacity = Math.max(0, Math.min(1, opacity));
                                        } else {
                                            message.style.opacity = 1;
                                        }
                                    }
                                });
                            }
                            updateMessageOpacity();
                            container.addEventListener('scroll', updateMessageOpacity);
                            new MutationObserver(updateMessageOpacity).observe(container, { childList: true });
                            """,
                    messagesContainer.getElement()
            );
        } else {
            logger.warn("UI недоступен при попытке выполнить JavaScript для эффекта затухания");
        }
    }

    private void showErrorNotification(String message) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE));
        } else {
            logger.warn("UI недоступен для показа уведомления: {}", message);
        }
    }

    private void safePush(UI ui) {
        if (ui != null && ui.getSession() != null) {
            ui.access(ui::push);
        } else {
            logger.warn("UI или сессия недоступны для push-обновления");
        }
    }
}