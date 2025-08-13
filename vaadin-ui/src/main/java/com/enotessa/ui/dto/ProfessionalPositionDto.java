package com.enotessa.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfessionalPositionDto implements ChatRequest{
    private String professionalPosition;
}
