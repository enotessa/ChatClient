package com.enotessa.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequest implements ChatRequest {
    private String login;
    private String password;
}
