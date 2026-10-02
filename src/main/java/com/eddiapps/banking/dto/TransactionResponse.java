package com.eddiapps.banking.dto;

import com.eddiapps.banking.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        TransactionType type,
        BigDecimal amount,
        Instant timestamp,
        String senderAccountNumber,
        String recipientAccountNumber
) {
}
