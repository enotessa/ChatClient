package com.enotessa.ui;

import com.enotessa.ui.common.StyledVerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("Welcome | RealTimeChat")
@CssImport("./styles/views/welcome-view.css")
public class WelcomeView extends StyledVerticalLayout {

    public WelcomeView() {
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

        // Полупрозрачный контейнер
        Div contentContainer = new Div();
        contentContainer.addClassNames("content-container", "main-container-width");

        // Контент
        H1 title = new H1("Welcome to RealTimeChat");
        title.addClassName("h1-title");

        Paragraph subtitle = new Paragraph("Connect with your team in real-time");
        subtitle.addClassName("subtitle");

        // Кнопки в горизонтальном layout
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.addClassName("buttons-layout");
        buttonsLayout.setSpacing(true);
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Button loginButton = new Button("Login", event -> {
            getUI().ifPresent(ui -> ui.navigate("login"));
        });
        loginButton.addClassName("button");

        Button registerButton = new Button("Register", event -> {
            getUI().ifPresent(ui -> ui.navigate("register"));
        });
        registerButton.addClassName("button");

        loginButton.setWidth(registerButton.getWidth());

        buttonsLayout.add(loginButton, registerButton);

        contentContainer.add(title, subtitle, buttonsLayout);
        add(backgroundWrapper, contentContainer);
    }
}