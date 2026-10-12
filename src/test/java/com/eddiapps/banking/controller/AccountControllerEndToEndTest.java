package com.eddiapps.banking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eddiapps.banking.entity.AccountEntity;
import com.eddiapps.banking.repository.AccountRepository;

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
public class AccountControllerEndToEndTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MockMvc mockMvc;

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
}
