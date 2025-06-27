package com.enotessa.ui;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("login")
@PageTitle("Login | RealTimeChat")
public class LoginView extends StyledVerticalLayout {

    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(false);
        setSpacing(false);

        // Фоновый слой
        Div background = new Div();
        background.addClassName("animated-background");

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
        Button loginButton = new Button("Sign In");
        loginButton.addClassName("button");

        formContainer.add(header, fieldsContainer, forgotLink, loginButton);
        add(backgroundWrapper, formContainer);
        setSizeFull();
    }
}