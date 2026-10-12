package com.eddiapps.banking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eddiapps.banking.entity.AccountEntity;
import com.eddiapps.banking.repository.AccountRepository;
import com.eddiapps.banking.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void deposit_withValidRequest_returnsOkAndPersistsAccount() throws Exception {
        String idempotencyKey = "e2e-deposit-1";
        mockMvc.perform(post("/api/accounts/1/deposit")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 100
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1"))
                .andExpect(jsonPath("$.balance").value(100));

        Optional<AccountEntity> account = accountRepository.findByAccountNumber("1");
        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void deposit_withSameIdempotencyKey_doesNotApplyDepositTwice() throws Exception {
        String idempotencyKey = "e2e-deposit-2";

        mockMvc.perform(post("/api/accounts/2/deposit")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 100
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("2"))
                .andExpect(jsonPath("$.balance").value(100));

        mockMvc.perform(post("/api/accounts/2/deposit")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 100
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("2"))
                .andExpect(jsonPath("$.balance").value(100));

        Optional<AccountEntity> account = accountRepository.findByAccountNumber("2");
        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void deposit_withSameIdempotencyKeyAndDifferentAmount_returnsConflict() throws Exception {
        String idempotencyKey = "e2e-deposit-3";

        mockMvc.perform(post("/api/accounts/3/deposit")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 100
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("3"))
                .andExpect(jsonPath("$.balance").value(100));

        mockMvc.perform(post("/api/accounts/3/deposit")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 200
                                }
                                """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Idempotency key already used for a different request"));

        Optional<AccountEntity> account = accountRepository.findByAccountNumber("3");
        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void transfer_withValidRequest_updatesBothAccounts() throws Exception {
        String idempotencyKey1 = "e2e-deposit-sender";
        accountService.deposit(idempotencyKey1, "1", BigDecimal.valueOf(100));

        String idempotencyKey2 = "e2e-deposit-recipient";
        accountService.deposit(idempotencyKey2, "2", BigDecimal.valueOf(50));

        String idempotencyKey3 = "e2e-transfer-1";
        mockMvc.perform(post("/api/accounts/transfer")
                        .header("Idempotency-Key", idempotencyKey3)
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

        Optional<AccountEntity> senderAccount = accountRepository.findByAccountNumber("1");
        assertTrue(senderAccount.isPresent());
        assertEquals(0, senderAccount.get().getBalance().compareTo(BigDecimal.valueOf(60)));

        Optional<AccountEntity> receiverAccount = accountRepository.findByAccountNumber("2");
        assertTrue(receiverAccount.isPresent());
        assertEquals(0, receiverAccount.get().getBalance().compareTo(BigDecimal.valueOf(90)));
    }

    @Test
    void transfer_withInsufficientFunds_returnsUnprocessableEntityAndKeepsBalances() throws Exception {
        String idempotencyKey1 = "e2e-deposit-sender";
        accountService.deposit(idempotencyKey1, "1", BigDecimal.valueOf(100));

        String idempotencyKey2 = "e2e-deposit-recipient";
        accountService.deposit(idempotencyKey2, "2", BigDecimal.valueOf(50));

        String idempotencyKey3 = "e2e-transfer-1";
        mockMvc.perform(post("/api/accounts/transfer")
                        .header("Idempotency-Key", idempotencyKey3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "senderAccountNumber": "1",
                                "recipientAccountNumber": "2",
                                "amount": 150
                                }
                                """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));

        Optional<AccountEntity> senderAccount = accountRepository.findByAccountNumber("1");
        assertTrue(senderAccount.isPresent());
        assertEquals(0, senderAccount.get().getBalance().compareTo(BigDecimal.valueOf(100)));

        Optional<AccountEntity> receiverAccount = accountRepository.findByAccountNumber("2");
        assertTrue(receiverAccount.isPresent());
        assertEquals(0, receiverAccount.get().getBalance().compareTo(BigDecimal.valueOf(50)));
    }

    @Test
    void transfer_toSameAccount_returnsBadRequestAndKeepsBalance() throws Exception {
        String idempotencyKey = "e2e-deposit-1";
        accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(100));

        String idempotencyKey2 = "e2e-transfer-1";
        mockMvc.perform(post("/api/accounts/transfer")
                        .header("Idempotency-Key", idempotencyKey2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "senderAccountNumber": "1",
                                "recipientAccountNumber": "1",
                                "amount": 50
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Sender and recipient must be different"));

        Optional<AccountEntity> account = accountRepository.findByAccountNumber("1");
        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void transactionHistory_afterTransfer_returnsPersistedTransactions() throws Exception {
        String idempotencyKey1 = "e2e-deposit-sender";
        accountService.deposit(idempotencyKey1, "1", BigDecimal.valueOf(100));

        String idempotencyKey2 = "e2e-deposit-recipient";
        accountService.deposit(idempotencyKey2, "2", BigDecimal.valueOf(50));

        String idempotencyKey3 = "e2e-transfer-1";
        mockMvc.perform(post("/api/accounts/transfer")
                        .header("Idempotency-Key", idempotencyKey3)
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

        mockMvc.perform(get("/api/accounts/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$[0].amount").value(40))
                .andExpect(jsonPath("$[0].senderAccountNumber").value("1"))
                .andExpect(jsonPath("$[0].recipientAccountNumber").value("2"));
    }
}
