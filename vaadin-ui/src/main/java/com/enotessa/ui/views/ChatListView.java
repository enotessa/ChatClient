package com.enotessa.ui.views;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.enotessa.ui.utils.TokenUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("chatList")
@PageTitle("ChatList | Chat App")
@CssImport("./styles/views/chat-list-styles.css")
public class ChatListView extends StyledVerticalLayout implements BeforeEnterObserver {
    private static final Logger logger = LoggerFactory.getLogger(ChatListView.class);
    private static final int NOTIFICATION_DURATION_MS = 5000;
    private final TokenUtil tokenUtil;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinSession session = VaadinSession.getCurrent();
        UI ui = UI.getCurrent();
        tokenUtil.isTokenInvalidOrNonRefreshable(session)
                .thenAccept(invalid -> ui.access(() -> {
                    if (invalid) {
                        logger.warn("Token is invalid or non-refreshable, redirecting to login");
                        showErrorNotification("Токен недействителен, пожалуйста, войдите снова");
                        event.forwardTo("");
                    }
                }))
                .exceptionally(throwable -> {
                    ui.access(() -> {
                        logger.error("Error checking token validity: {}", throwable.getMessage());
                        showErrorNotification("Ошибка проверки токена: " + throwable.getMessage());
                        event.forwardTo("");
                    });
                    return null;
                });
    }

    public ChatListView(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
        addClassName("chat-list-view");
        addClassName("simple-background");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Контейнер для поиска и кнопки
        HorizontalLayout searchAndButtonLayout = new HorizontalLayout();
        searchAndButtonLayout.addClassName("search-button-layout");
        searchAndButtonLayout.setWidthFull();
        searchAndButtonLayout.setAlignItems(Alignment.CENTER);

        // Поле поиска
        TextField searchField = new TextField();
        searchField.addClassName("rounded-search-field");
        searchField.setPlaceholder("Поиск чатов...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setWidth("100%");

        // Кнопка нового чата
        Button newChatButton = new Button(VaadinIcon.PLUS.create());
        newChatButton.addClassName("icon-button");
        newChatButton.addThemeVariants(ButtonVariant.LUMO_ICON);

        searchAndButtonLayout.add(searchField, newChatButton);
        searchAndButtonLayout.setFlexGrow(1, searchField);

        // Список чатов
        Div chatListContainer = new Div();
        chatListContainer.addClassName("chat-list-container");

        for (Chat chat : getSampleChats()) {
            chatListContainer.add(createChatItem(chat));
        }

        // Компоновка
        add(searchAndButtonLayout, chatListContainer);
        expand(chatListContainer);
    }

    private Div createChatItem(Chat chat) {
        Div chatItem = new Div();
        chatItem.addClassName("chat-item");

        // Аватарка
        Div avatar = new Div();
        avatar.addClassName("chat-avatar");
        avatar.setText(chat.getName().substring(0, 1).toUpperCase());

        // Контент чата
        Div content = new Div();
        content.addClassName("chat-content");

        // Название чата
        Span name = new Span(chat.getName());
        name.addClassName("chat-name");

        // Контейнер для сообщения и времени
        Div messageContainer = new Div();
        messageContainer.addClassName("message-container");

        // Последнее сообщение
        Span lastMessage = new Span(chat.getDescription());
        lastMessage.addClassName("chat-last-message");

        // Время
        Span time = new Span(formatTime(chat.getLastMessageTime()));
        time.addClassName("chat-time");

        messageContainer.add(lastMessage, time);

        content.add(name, messageContainer);
        chatItem.add(avatar, content);
        chatItem.addClickListener(e -> openChat(chat.getId()));

        return chatItem;
    }

    private String formatTime(LocalDateTime time) {
        return DateTimeFormatter.ofPattern("HH:mm").format(time);
    }

    private List<Chat> getSampleChats() {
        return List.of(
                new Chat(1, "Собеседование", "Пройти собеседование на желаемую роль", LocalDateTime.now().minusHours(2)),
                new Chat(2, "Диетолог", "Составит план питания на каждый день", LocalDateTime.now().minusDays(1))
        );
    }

    private void openChat(int chatId) {
        switch (chatId) {
            case 1:
                Notification.show("Переходим в чат собеседования", 3000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate("interviewChat");
                break;

            default:
                Notification.show("Переход не реализован", 3000, Notification.Position.MIDDLE);
                break;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Chat {
        private int id;
        private String name;
        private String description;
        private LocalDateTime lastMessageTime;
    }

    private void showErrorNotification(String message) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE);
    }
}
