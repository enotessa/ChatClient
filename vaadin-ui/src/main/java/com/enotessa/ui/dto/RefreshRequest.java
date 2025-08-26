package com.enotessa.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshRequest implements ChatRequest{
    private String refreshToken;
}
