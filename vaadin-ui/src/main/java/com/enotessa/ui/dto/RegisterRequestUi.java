package com.enotessa.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterRequestUi implements ChatRequest {
    private String login;
    private String email;
    private String password;
}

