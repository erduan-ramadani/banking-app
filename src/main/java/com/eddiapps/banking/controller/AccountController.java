package com.eddiapps.banking.controller;

import com.eddiapps.banking.dto.AccountResponse;
import com.eddiapps.banking.dto.AmountRequest;
import com.eddiapps.banking.dto.TransactionResponse;
import com.eddiapps.banking.dto.TransferRequest;
import com.eddiapps.banking.service.AccountService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountNumber}/deposit")
    AccountResponse deposit(
            @PathVariable String accountNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AmountRequest request
    ) {
        return accountService.deposit(idempotencyKey, accountNumber, request.amount());
    }

    @PostMapping("/{accountNumber}/withdraw")
    AccountResponse withdraw(
            @PathVariable String accountNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AmountRequest request
    ) {
        return accountService.withdraw(idempotencyKey, accountNumber, request.amount());
    }

    @GetMapping("/{accountNumber}")
    AccountResponse getBalance(
            @PathVariable String accountNumber
    ) {
        return accountService.getBalance(accountNumber);
    }

    @PostMapping("/transfer")
    AccountResponse transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        return accountService.transfer(
                idempotencyKey,
                request.senderAccountNumber(),
                request.recipientAccountNumber(),
                request.amount()
        );
    }

    @GetMapping("/{accountNumber}/history")
    List<TransactionResponse> getHistory(@PathVariable String accountNumber) {
        return accountService.getTransactionHistory(accountNumber);
    }
}
