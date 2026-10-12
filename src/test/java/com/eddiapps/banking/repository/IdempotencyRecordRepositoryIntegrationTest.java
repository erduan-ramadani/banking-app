package com.eddiapps.banking.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.eddiapps.banking.entity.IdempotencyRecord;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

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

    @Test
    void findByIdempotencyKey_withExistingRecord_returnsRecord() {
        String idempotencyKey = "deposit-key123";
        String requestFingerprint = "DEPOSIT | 1 | 100";
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                "1",
                BigDecimal.valueOf(50),
                Instant.now()
        );
        idempotencyRecordRepository.saveAndFlush(idempotencyRecord);
        Optional<IdempotencyRecord> loadedIdempotencyRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        assertTrue(loadedIdempotencyRecord.isPresent());
        assertEquals(idempotencyRecord.getIdempotencyKey(), loadedIdempotencyRecord.get().getIdempotencyKey());
        assertEquals(idempotencyRecord.getRequestFingerprint(), loadedIdempotencyRecord.get().getRequestFingerprint());
    }
}
