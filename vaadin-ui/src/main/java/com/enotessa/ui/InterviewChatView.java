package com.enotessa.ui;

import com.enotessa.ui.dto.MessageDto;
import com.enotessa.ui.utils.HandleErrorUtil;
import com.enotessa.ui.utils.RequestUtil;
import com.enotessa.ui.utils.TokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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

@Route("interviewChat")
@PageTitle("Собеседование | Чат")
@CssImport("./styles/views/chat-view.css")
public class InterviewChatView extends VerticalLayout {

    Div messagesContainer;
    @Autowired
    private RequestUtil requestUtil;
    @Autowired
    private TokenUtil tokenUtil;

    @Value("${backChat.host}")
    private String backHost;
    @Value("${backChat.port}")
    private String backPort;

    public InterviewChatView() {
        addClassName("chat-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        Div chatContainer = new Div();
        chatContainer.addClassNames("chat-background");

        // 1. Верхняя панель с круглой кнопкой назад
        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addClassName("round-back-button");
        backButton.addClickListener(e -> UI.getCurrent().navigate("chatList"));

        Div header = new Div(backButton);
        header.addClassName("chat-header");

        // 2. Область сообщений с прокруткой
        messagesContainer = new Div();
        messagesContainer.addClassName("messages-container");

        // Пример сообщений TODO сделать загрузку сообщений с сервера
        messagesContainer.add(
                createMessage("HR", "Здравствуйте! На какую позицию хотите пройти собеседование?", false)
        );
        chatContainer.add(header, messagesContainer);

        // 3. Панель ввода сообщения
        TextField messageField = new TextField();
        messageField.addClassName("message-input");
        messageField.setPlaceholder("Введите сообщение...");
        messageField.addKeyPressListener(Key.ENTER, e -> sendMessage(messageField));

        Button sendButton = new Button(VaadinIcon.PAPERPLANE.create());
        sendButton.addClassName("send-button");
        sendButton.addClickListener(e -> sendMessage(messageField));

        HorizontalLayout inputLayout = new HorizontalLayout(messageField, sendButton);
        inputLayout.addClassName("input-layout");
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, messageField);

        // Компоновка
        add(chatContainer, inputLayout);
        expand(chatContainer);

        // Добавление JavaScript для эффекта затухания сообщений
        executeJsCodeForTheEffectOfFadingMessages();
    }

    private Div createMessage(String sender, String text, boolean isCurrentUser) {
        Div messageWrapper = new Div();
        messageWrapper.addClassName(isCurrentUser ? "message-wrapper-current" : "message-wrapper-other");

        Div message = new Div();
        message.addClassName("message");

        if (!isCurrentUser) {
            Span senderSpan = new Span(sender + ":");
            senderSpan.addClassName("message-sender");
            message.add(senderSpan);
        }

        Div bubble = new Div();
        bubble.addClassName(isCurrentUser ? "bubble-current" : "bubble-other");
        bubble.setText(text);

        message.add(bubble);
        messageWrapper.add(message);
        return messageWrapper;
    }

    private void sendMessage(TextField messageField) {
        String text = messageField.getValue();
        if (!text.isEmpty()) {
            Div myMessage = createMessage("Я", text, true);
            messagesContainer.add(myMessage);
            messageField.clear();

            MessageDto messageDto = new MessageDto(
                    1,
                    text,
                    LocalDateTime.now()
            );
            try {
                String requestBody = requestUtil.convertToJSON(messageDto);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest httpRequest = requestUtil.buildHttpRequest(requestUtil.buildUri(backHost, backPort, "/api/chats/interview"), requestBody);

                sendRequest(client, httpRequest);
            } catch (Exception e) {
                Notification.show("Ошибка подключения: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }

            // Прокрутка вниз
            UI.getCurrent().beforeClientResponse(messagesContainer, ctx -> {
                UI.getCurrent().getPage().executeJs(
                        "const container = $0; container.scrollTop = container.scrollHeight;",
                        messagesContainer.getElement()
                );
            });
        }
    }

    private void sendRequest(HttpClient client, HttpRequest httpRequest) {
        UI ui = UI.getCurrent();
        System.out.println("отправка сообщения на сервер");
        client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    ui.access(() -> {
                        if (response.statusCode() == 200) {
                            try {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonNode = objectMapper.readTree(response.body());
                                String message = jsonNode.get("message").asText();
                                messagesContainer.add(createMessage("HR", message, false));
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("Ошибка при обработке токена");
                                Notification.show("Ошибка при обработке токена");
                            }
                        } else {
                            System.out.println("неудачный запрос. status: "+ response.statusCode());
                            HandleErrorUtil.handleError(response);
                        }
                    });
                });
    }

    private void executeJsCodeForTheEffectOfFadingMessages(){
        UI.getCurrent().getPage().executeJs(
                """
                const container = $0;
                function updateMessageOpacity() {
                    const messages = container.querySelectorAll('.message-wrapper-current, .message-wrapper-other');
                    const containerTop = container.getBoundingClientRect().top;
                    const scrollTop = container.scrollTop;
                    const fadeHeight = 100; // Высота области затухания в пикселях
                    messages.forEach(message => {
                        if (scrollTop === 0) {
                            // Если прокрутка в самом верху, все сообщения полностью непрозрачные
                            message.style.opacity = 1;
                        } else {
                            // Иначе применяем эффект затухания
                            const messageTop = message.getBoundingClientRect().top - containerTop;
                            if (messageTop < fadeHeight) {
                                const opacity = messageTop / fadeHeight;
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
