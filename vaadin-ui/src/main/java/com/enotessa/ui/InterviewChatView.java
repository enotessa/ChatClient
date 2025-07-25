package com.enotessa.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("interviewChat")
@PageTitle("Собеседование | Чат")
@CssImport("./styles/views/chat-view.css")
public class InterviewChatView extends VerticalLayout {

    Div messagesContainer;

    public InterviewChatView() {
        addClassName("chat-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

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
        add(header, messagesContainer, inputLayout);
        expand(messagesContainer);
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
            //TODO сделать отправку сообщений на сервер и получение ответа
            messagesContainer.add(createMessage("Я", text, true));
            messageField.clear();

            // Прокрутка вниз после обновления DOM
            UI.getCurrent().beforeClientResponse(messagesContainer, ctx -> {
                UI.getCurrent().getPage().executeJs(
                        "const container = $0; container.scrollTop = container.scrollHeight;",
                        messagesContainer.getElement()
                );
            });
        }
    }
}
