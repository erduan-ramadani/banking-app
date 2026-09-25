package com.eddiapps.banking.dto;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        BigDecimal balance
) {

}
