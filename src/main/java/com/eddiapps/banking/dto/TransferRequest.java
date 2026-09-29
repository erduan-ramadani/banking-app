package com.eddiapps.banking.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotBlank
        String senderAccountNumber,
        @NotBlank
        String recipientAccountNumber,
        @NotNull
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Amount must be greater than 0"
        )
        BigDecimal amount
) {
}
