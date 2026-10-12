package com.eddiapps.banking.service;

import static org.junit.jupiter.api.Assertions.*;

import com.eddiapps.banking.entity.AccountEntity;
import com.eddiapps.banking.entity.IdempotencyRecord;
import com.eddiapps.banking.exception.IdempotencyConflictException;
import com.eddiapps.banking.repository.AccountRepository;
import com.eddiapps.banking.repository.IdempotencyRecordRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@SpringBootTest
@Transactional
public class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    void deposit_savesAccountAndIdempotencyRecord() {
        String idempotencyKey = "deposit-integration-1";
        accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(100));
        Optional<AccountEntity> account = accountRepository.findByAccountNumber("1");

        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));

        Optional<IdempotencyRecord> idempotencyRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        assertTrue(idempotencyRecord.isPresent());
        IdempotencyRecord existingRecord = idempotencyRecord.get();
        assertEquals("1", existingRecord.getAccountNumber());
        assertEquals(0, existingRecord.getBalance().compareTo(BigDecimal.valueOf(100)));
        assertEquals("DEPOSIT | 1 | 100", existingRecord.getRequestFingerprint());
    }

    @Test
    void deposit_withSameIdempotencyKey_doesNotApplyDepositTwice() {
        String idempotencyKey = "deposit-integration-2";

        accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(100));
        accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(100));

        Optional<AccountEntity> account = accountRepository.findByAccountNumber("1");

        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));

        assertEquals(1, idempotencyRecordRepository.countByIdempotencyKey(idempotencyKey));
    }

    @Test
    void deposit_withSameIdempotencyKeyAndDifferentAmount_throwsConflict() {
        String idempotencyKey = "deposit-integration-3";

        accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(100));
        assertThrows(IdempotencyConflictException.class,
                () -> accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(200))
        );

        Optional<AccountEntity> account = accountRepository.findByAccountNumber("1");
        assertTrue(account.isPresent());
        assertEquals(0, account.get().getBalance().compareTo(BigDecimal.valueOf(100)));
        assertEquals(1, idempotencyRecordRepository.countByIdempotencyKey(idempotencyKey));
    }
}
