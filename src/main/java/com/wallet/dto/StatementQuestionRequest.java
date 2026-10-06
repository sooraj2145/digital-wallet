package com.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatementQuestionRequest(
        @NotBlank
        @Size(max= 500)
        String question
) {
}
