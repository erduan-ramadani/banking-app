package com.eddiapps.banking.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eddiapps.banking.entity.IdempotencyRecord;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;

@DataJpaTest
public class IdempotencyRecordRepositoryIntegrationTest {

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    void saveDuplicateIdempotencyKey_throwsDataIntegrityViolationException() {
        String idempotencyKey = "duplicate-key123";

        String requestFingerprint1 = "DEPOSIT | 1 | 40";
        IdempotencyRecord idempotencyRecord1 = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint1,
                "1",
                BigDecimal.valueOf(100),
                Instant.now()
        );
        idempotencyRecordRepository.saveAndFlush(idempotencyRecord1);

        String requestFingerprint2 = "WITHDRAWAL | 2 | 50";
        IdempotencyRecord idempotencyRecord2 = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint2,
                "2",
                BigDecimal.valueOf(100),
                Instant.now()
        );
        assertThrows(DataIntegrityViolationException.class,
                () -> idempotencyRecordRepository.saveAndFlush(idempotencyRecord2)
        );
    }
}
