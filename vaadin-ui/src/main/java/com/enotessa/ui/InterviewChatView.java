package com.enotessa.ui;

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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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

    @Autowired
    private RequestUtil requestUtil;
    @Autowired
    private TokenUtil tokenUtil;

    @Value("${backChat.host}")
    private String backHost;
    @Value("${backChat.port}")
    private String backPort;

    private final String FIRST_DEFAULT_MESSAGE = "Здравствуйте! Давайте начнем собеседование";
    private static final String INTERVIEW_URL_PATH = "/api/chats/interview";
    private static final String PROFESSION_URL_PATH = "/api/chats/interviewProfession";

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
        createInputLayout();
    }

    private void createMessageField() {
        messageField = new TextField();
        messageField.addClassName("message-input");
        messageField.setPlaceholder("Введите сообщение...");
        messageField.addKeyPressListener(Key.ENTER, e -> sendMessage(messageField));
    }

    private void createSendButton() {
        sendButton = new Button(VaadinIcon.PAPERPLANE.create());
        sendButton.addClassName("send-button");
        sendButton.addClickListener(e -> sendMessage(messageField));
    }

    private void createInputLayout() {
        inputLayout = new HorizontalLayout(messageField, sendButton);
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

    //-- SEND MESSAGE ----------------------------------------------------------------------------

    private void sendMessage(TextField messageField) {
        String text = messageField.getValue();
        if (text.isBlank()) return;

        addMessageToUI("me", text, true);
        messageField.clear();

        sendMessageToServer(new MessageDto("me", text, LocalDateTime.now()));
        scrollMessagesToBottom();
    }

    private void addMessageToUI(String sender, String text, boolean isCurrentUser) {
        messagesContainer.add(createMessage(sender, text, isCurrentUser));
    }

    private void sendMessageToServer(MessageDto messageDto) {
        try {
            String requestBody = requestUtil.convertToJSON(messageDto);
            HttpRequest httpRequest = requestUtil.buildHttpRequest(
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
            HttpRequest httpRequest = requestUtil.buildHttpRequest(
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
