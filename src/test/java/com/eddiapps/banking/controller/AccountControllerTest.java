package com.eddiapps.banking.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eddiapps.banking.dto.AccountResponse;
import com.eddiapps.banking.exception.AccountNotFoundException;
import com.eddiapps.banking.exception.InsufficientFundsException;
import com.eddiapps.banking.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

@WebMvcTest(AccountController.class)
public class AccountControllerTest {

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deposit_returnsAccountResponse() throws Exception {
        when(accountService.deposit("1", BigDecimal.valueOf(100)))
                .thenReturn(new AccountResponse("1", BigDecimal.valueOf(200)));

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 100
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1"))
                .andExpect(jsonPath("$.balance").value(200));
    }

    @Test
    void withdraw_returnsAccountResponse() throws Exception {
        when(accountService.withdraw("1", BigDecimal.valueOf(40)))
                .thenReturn(new AccountResponse("1", BigDecimal.valueOf(60)));

        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 40
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1"))
                .andExpect(jsonPath("$.balance").value(60));
    }

    @Test
    void getBalance_returnsAccountResponse() throws Exception {
        when(accountService.getBalance("1")).thenReturn(
                new AccountResponse("1", BigDecimal.valueOf(60))
        );

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1"))
                .andExpect(jsonPath("$.balance").value(60));

    }

    @Test
    void getBalance_withUnknownAccount_returnsNotFound() throws Exception {
        when(accountService.getBalance("99"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/99"))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void withdraw_withInsufficientFunds_returnsInsufficientFundsError() throws Exception {
        when(accountService.withdraw("1", BigDecimal.valueOf(140)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 140
                                }
                                """)
                )
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void withdraw_withInvalidAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 0
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Amount must be greater than 0"));

        verify(accountService, never()).withdraw(anyString(), any(BigDecimal.class));
    }
}
