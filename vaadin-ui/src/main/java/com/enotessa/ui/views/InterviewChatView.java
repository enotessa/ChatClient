package com.enotessa.ui.views;

import com.enotessa.ui.dto.MessageDto;
import com.enotessa.ui.dto.ProfessionalPositionDto;
import com.enotessa.ui.enums.ProfessionEnum;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Route("interviewChat")
@PageTitle("Собеседование | Чат")
@CssImport("./styles/views/chat-view.css")
public class InterviewChatView extends VerticalLayout {
    private HorizontalLayout headerLayout;
    private ComboBox<String> optionsMenu;
    private Button backButton;
    private Div messagesContainer;
    private Div chatContainer;
    private TextField messageField;
    private Button sendButton;
    private HorizontalLayout inputLayout;
    private Button micButton;

    @Autowired
    private RequestUtil requestUtil;
    @Autowired
    private TokenUtil tokenUtil;

    @Value("${backChat.host}")
    private String backHost;
    @Value("${backChat.port}")
    private String backPort;

    private final String FIRST_DEFAULT_MESSAGE = """
            Здравствуйте! Давайте начнем собеседование.
            \nЕсли захотите начать собеседование сначала, напиши \"заново\".
            \nЕсли хотите, чтобы я задал следующий вопрос, напиши \"дальше\" 
            """;
    private static final String INTERVIEW_URL_PATH = "/interview/message";
    private static final String PROFESSION_URL_PATH = "/interview/interviewProfession";
    private static final String DELETE_MESSAGES_URL_PATH = "/interview/deleteMessages";

    public InterviewChatView() {
        configureMainLayout();
        configureHeader();
        configureMessagesArea();
        chatContainer.add(headerLayout, messagesContainer);
        configureInputArea();
        add(chatContainer, inputLayout);
        expand(chatContainer);
        applyFadingEffect();
    }

    @PostConstruct
    private void initialize() {
        changeProfessionalPosition(ProfessionEnum.JAVA_MIDDLE.getDisplayName());
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
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN); // Кнопка слева, меню справа
        headerLayout.setAlignItems(Alignment.CENTER);
    }

    private void createBackButton() {
        backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addClassName("round-back-button");
        backButton.addClickListener(e -> UI.getCurrent().navigate("chatList"));
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
            changeProfessionalPosition(selected);
        });
    }

    private void configureMessagesArea() {
        messagesContainer = new Div();
        messagesContainer.addClassName("messages-container");
        // Пример сообщений TODO сделать загрузку сообщений с сервера
        messagesContainer.add(
                createMessage("HR", FIRST_DEFAULT_MESSAGE, false)
        );
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
        messageField.setValueChangeMode(ValueChangeMode.EAGER); // Синхронизация при каждом изменении
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
        executeJsCodeForTheEffectOfFadingMessages();

    }


    //-- CREATE MESSAGES ----------------------------------------------------------------------------

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

    //-- business logic --------------------------------------------------------------------------

    private void actionSelection(TextField messageField) {
        String text = messageField.getValue();
        if (text.isBlank()) {
            Notification.show("Сообщение пустое", 3000, Notification.Position.MIDDLE);
        } else if (text.equals("заново")) {
            resetMessages();
        } else sendMessage(text);
    }

    private void resetMessages() {
        HttpRequest httpRequest = requestUtil.buildDeleteHttpRequest(
                requestUtil.buildUri(backHost, backPort, DELETE_MESSAGES_URL_PATH));
        deleteMessagesRequest(HttpClient.newHttpClient(), httpRequest);
    }

    private void deleteMessagesRequest(HttpClient client, HttpRequest httpRequest) {
        UI ui = UI.getCurrent();
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> ui.access(() -> handleDeleteMessagesResponse(response, ui)));
    }

    private void handleDeleteMessagesResponse(HttpResponse<String> response, UI ui) {
        if (response.statusCode() == 200) {
            try {
                deleteMessagesFromUI();
                addMessageToUI("HR", FIRST_DEFAULT_MESSAGE, false);
                ui.push();
            } catch (Exception e) {
                Notification.show("Ошибка при обработке ответа");
            }
        } else {
            HandleErrorUtil.handleError(response);
        }
    }

    private void startSpeechRecognition() {
        UI.getCurrent().getPage().executeJs(
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
                                if (field.value && field.value.trim() !== '') {
                                    field.value += ' ' + text;
                                } else {
                                    field.value = text;
                                }
                                field.dispatchEvent(new Event('input', { bubbles: true }));
                                field.dispatchEvent(new Event('change', { bubbles: true })); // Добавляем событие change
                            };
                        
                            recognition.onerror = function(event) {
                                console.error('Speech recognition error:', event.error);
                            };
                        
                            recognition.start();
                        }
                        """
        );
    }


    //-- SEND MESSAGE ----------------------------------------------------------------------------

    private void sendMessage(String textMessage) {
        addMessageToUI("me", textMessage, true);
        messageField.clear();

        sendMessageToServer(new MessageDto("me", textMessage, LocalDateTime.now()));
        scrollMessagesToBottom();
    }

    private void deleteMessagesFromUI() {
        messagesContainer.removeAll();
    }

    private void addMessageToUI(String sender, String text, boolean isCurrentUser) {
        messagesContainer.add(createMessage(sender, text, isCurrentUser));
    }

    private void sendMessageToServer(MessageDto messageDto) {
        try {
            String requestBody = requestUtil.convertToJSON(messageDto);
            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(
                    requestUtil.buildUri(backHost, backPort, INTERVIEW_URL_PATH),
                    requestBody
            );
            sendMessageRequest(HttpClient.newHttpClient(), httpRequest);
        } catch (Exception e) {
            Notification.show("Ошибка подключения: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void sendMessageRequest(HttpClient client, HttpRequest httpRequest) {
        UI ui = UI.getCurrent();
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> ui.access(() -> handleServerResponse(response, ui)));
    }

    private void handleServerResponse(HttpResponse<String> response, UI ui) {
        if (response.statusCode() == 200) {
            try {
                String message = new ObjectMapper()
                        .readTree(response.body())
                        .get("message")
                        .asText();

                addMessageToUI("HR", message, false);
                scrollMessagesToBottom();
                ui.push();
            } catch (Exception e) {
                Notification.show("Ошибка при обработке ответа");
            }
        } else {
            HandleErrorUtil.handleError(response);
        }
    }

    private void scrollMessagesToBottom() {
        UI.getCurrent().beforeClientResponse(messagesContainer, ctx ->
                UI.getCurrent().getPage().executeJs(
                        "const container = $0; container.scrollTop = container.scrollHeight;",
                        messagesContainer.getElement()
                )
        );
    }


    //-- CHANGE PROFESSION ----------------------------------------------------------------------------

    private void changeProfessionalPosition(String selected) {
        try {
            String requestBody = requestUtil.convertToJSON(new ProfessionalPositionDto(selected));
            HttpRequest httpRequest = requestUtil.buildPostHttpRequestWithBody(
                    requestUtil.buildUri(backHost, backPort, PROFESSION_URL_PATH),
                    requestBody
            );
            sendProfessionRequest(HttpClient.newHttpClient(), httpRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendProfessionRequest(HttpClient client, HttpRequest httpRequest) {
        UI ui = UI.getCurrent();
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> ui.access(() -> handleProfessionResponse(response, ui)));
    }

    private void handleProfessionResponse(HttpResponse<String> response, UI ui) {
        if (response.statusCode() == 200) {
            messagesContainer.removeAll();
            addMessageToUI("HR", FIRST_DEFAULT_MESSAGE, false);
            Notification.show(
                    "Вы выбрали собеседование на позицию " + optionsMenu.getValue(),
                    3000,
                    Notification.Position.TOP_CENTER
            );
            ui.push();
        } else {
            HandleErrorUtil.handleError(response);
        }
    }


    //-- JS CODE ----------------------------------------------------------------------------

    private void executeJsCodeForTheEffectOfFadingMessages() {
        UI.getCurrent().getPage().executeJs(
                """
                        const container = $0;
                        function updateMessageOpacity() {
                            const messages = container.querySelectorAll('.message-wrapper-current, .message-wrapper-other');
                            const scrollTop = container.scrollTop;
                            const fadeHeight = 150; // Высота области затухания в пикселях
                            messages.forEach(message => {
                                if (scrollTop === 0) {
                                    // Если прокрутка в самом верху, все сообщения полностью непрозрачные
                                    message.style.opacity = 1;
                                } else {
                                    // Иначе применяем эффект затухания на основе нижней части сообщения
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
                        // Вызываем при загрузке и при прокрутке
                        updateMessageOpacity();
                        container.addEventListener('scroll', updateMessageOpacity);
                        // Вызываем при добавлении новых сообщений
                        new MutationObserver(updateMessageOpacity).observe(container, { childList: true });
                        """,
                messagesContainer.getElement()
        );
    }
}
