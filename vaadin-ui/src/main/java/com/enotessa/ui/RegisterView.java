package com.enotessa.ui;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("register")
@PageTitle("Register | RealTimeChat")
public class RegisterView extends StyledVerticalLayout {

    public RegisterView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(false);
        setSpacing(false);

        // Фоновый слой
        Div background = new Div();
        background.addClassName("animated-background");

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
        Button registerButton = new Button("Register", e -> {
            if (!password.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Passwords don't match!");
                return;
            }
            Notification.show("Registration successful!");
        });
        registerButton.addClassName("button");

        formContainer.add(header, fieldsContainer, registerButton);
        add(backgroundWrapper, formContainer);
        setSizeFull();
    }
}