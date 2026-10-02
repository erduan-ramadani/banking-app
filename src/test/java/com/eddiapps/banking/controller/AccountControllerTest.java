package com.eddiapps.banking.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eddiapps.banking.dto.AccountResponse;
import com.eddiapps.banking.dto.TransactionResponse;
import com.eddiapps.banking.exception.AccountNotFoundException;
import com.eddiapps.banking.exception.InsufficientFundsException;
import com.eddiapps.banking.exception.InvalidTransferException;
import com.eddiapps.banking.model.TransactionType;
import com.eddiapps.banking.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

    @Test
    void transfer_withValidAccounts_returnsUpdatedBalance() throws Exception {
        AccountResponse response = new AccountResponse(
                "1", BigDecimal.valueOf(60)
        );

        when(accountService.transfer(
                "1",
                "2",
                BigDecimal.valueOf(40))
        ).thenReturn(response);

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "senderAccountNumber": "1",
                                "recipientAccountNumber": "2",
                                "amount": 40
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1"))
                .andExpect(jsonPath("$.balance").value(60));

        verify(accountService).transfer("1", "2", BigDecimal.valueOf(40));
    }

    @Test
    void transfer_withSameAccountNumbers_returnsBadRequest() throws Exception {
        when(accountService.transfer(
                "1",
                "1",
                BigDecimal.valueOf(40))
        ).thenThrow(new InvalidTransferException(
                "Sender and recipient must be different"
        ));

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {
                                        "senderAccountNumber": "1",
                                        "recipientAccountNumber": "1",
                                        "amount": 40
                                        }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Sender and recipient must be different"));

        verify(accountService).transfer("1", "1", BigDecimal.valueOf(40));
    }

    @Test
    void transfer_withInvalidAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {
                                        "senderAccountNumber": "1",
                                        "recipientAccountNumber": "2",
                                        "amount": 0
                                        }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Amount must be greater than 0"));

        verify(accountService, never()).transfer(
                any(String.class),
                any(String.class),
                any(BigDecimal.class)
        );
    }

    @Test
    void transfer_withUnknownSender_returnsNotFound() throws Exception {
        when(accountService.transfer("99", "2", BigDecimal.valueOf(100))
        ).thenThrow(new AccountNotFoundException("Sender account not found"));

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {
                                        "senderAccountNumber": "99",
                                        "recipientAccountNumber": "2",
                                        "amount": 100
                                        }
                                """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Sender account not found"));

        verify(accountService).transfer("99", "2", BigDecimal.valueOf(100));
    }

    @Test
    void transfer_withUnknownRecipient_returnsNotFound() throws Exception {
        when(accountService.transfer(
                "1",
                "99",
                BigDecimal.valueOf(100)))
                .thenThrow(new AccountNotFoundException("Recipient account not found"));

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {
                                        "senderAccountNumber": "1",
                                        "recipientAccountNumber": "99",
                                        "amount": 100
                                        }
                                """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Recipient account not found"));

        verify(accountService).transfer("1", "99", BigDecimal.valueOf(100));
    }

    @Test
    void transfer_withInsufficientFunds_returnsBadRequest() throws Exception {
        when(accountService.transfer(
                "1",
                "2",
                BigDecimal.valueOf(120)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {
                                        "senderAccountNumber": "1",
                                        "recipientAccountNumber": "2",
                                        "amount": 120
                                        }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));

        verify(accountService).transfer("1", "2", BigDecimal.valueOf(120));
    }

    @Test
    void getTransactionHistory_withExistingTransactions_returnsTransactionResponses() throws Exception {
        TransactionResponse entity1 = new TransactionResponse(
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(100),
                Instant.now(),
                null,
                null
        );

        TransactionResponse entity2 = new TransactionResponse(
                TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(40),
                Instant.now(),
                null,
                null
        );

        TransactionResponse entity3 = new TransactionResponse(
                TransactionType.TRANSFER,
                BigDecimal.valueOf(120),
                Instant.now(),
                "1",
                "2"
        );

        when(accountService.getTransactionHistory("1"))
                .thenReturn(List.of(entity1, entity2, entity3));

        mockMvc.perform(get("/api/accounts/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[2].type").value("TRANSFER"))
                .andExpect(jsonPath("$[1].amount").value(40))
                .andExpect(jsonPath("$[2].recipientAccountNumber").value("2"))
        ;

        verify(accountService).getTransactionHistory("1");
    }

    @Test
    void getTransactionHistory_withUnknownAccount_returnsNotFound() throws Exception {
        when(accountService.getTransactionHistory("99"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/99/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found"));

        verify(accountService).getTransactionHistory("99");
    }
}
