package com.eddiapps.banking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eddiapps.banking.dto.AccountResponse;
import com.eddiapps.banking.dto.TransactionResponse;
import com.eddiapps.banking.entity.AccountEntity;
import com.eddiapps.banking.entity.IdempotencyRecord;
import com.eddiapps.banking.entity.TransactionEntity;
import com.eddiapps.banking.exception.*;
import com.eddiapps.banking.model.TransactionType;
import com.eddiapps.banking.repository.AccountRepository;
import com.eddiapps.banking.repository.IdempotencyRecordRepository;
import com.eddiapps.banking.repository.TransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void withdraw_withSufficientFunds_returnsUpdatedBalance() {
        String idempotencyKey = "withdraw-key-123";
        AccountEntity accountEntity = new AccountEntity("1");
        accountEntity.setBalance(BigDecimal.valueOf(100));

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(accountEntity));

        AccountResponse response = accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(40));

        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(60)));
        assertEquals(
                0,
                accountEntity.getBalance().compareTo(BigDecimal.valueOf(60))
        );

        verify(accountRepository).findByAccountNumber("1");
    }

    @Test
    void withdraw_withInsufficientFunds_throwsInsufficientFundsException() {
        String idempotencyKey = "withdraw-key-123";
        AccountEntity accountEntity = new AccountEntity("1");
        accountEntity.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(accountEntity));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(140)));
    }

    @Test
    void withdraw_withUnknownAccount_throwsAccountNotFoundException() {
        String idempotencyKey = "withdraw-key-123";

        when(accountRepository.findByAccountNumber("99"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.withdraw(idempotencyKey, "99", BigDecimal.valueOf(140)));
    }

    @Test
    void withdraw_withInvalidAmount_throwsInvalidAmountException() {
        String idempotencyKey = "withdraw-key-123";

        assertThrows(InvalidAmountException.class,
                () -> accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(0)));

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_withExistingAccount_returnsUpdatedBalance() {
        String idempotencyKey = "deposit-key-123";
        AccountEntity accountEntity = new AccountEntity("1");
        accountEntity.setBalance(BigDecimal.valueOf(100));

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(accountEntity));

        AccountResponse response = accountService.deposit(
                idempotencyKey,
                "1",
                BigDecimal.valueOf(40)
        );

        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(140)));
        assertEquals(
                0,
                accountEntity.getBalance().compareTo(BigDecimal.valueOf(140))
        );

        verify(accountRepository).findByAccountNumber("1");
    }

    @Test
    void deposit_withUnknownAccount_createsAndSavesAccount() {
        String idempotencyKey = "deposit-key-123";

        when(accountRepository.findByAccountNumber("2"))
                .thenReturn(Optional.empty());

        AccountResponse response = accountService.deposit(
                idempotencyKey,
                "2",
                BigDecimal.valueOf(100)
        );

        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(100)));

        verify(accountRepository).findByAccountNumber("2");
        ArgumentCaptor<AccountEntity> captor =
                ArgumentCaptor.forClass(AccountEntity.class);

        verify(accountRepository).save(captor.capture());

        AccountEntity savedAccount = captor.getValue();

        assertEquals("2", savedAccount.getAccountNumber());
        assertEquals(
                0,
                savedAccount.getBalance().compareTo(BigDecimal.valueOf(100))
        );
    }

    @Test
    void deposit_withInvalidAmount_throwsInvalidAmountException() {
        String idempotencyKey = "deposit-key-123";

        assertThrows(InvalidAmountException.class,
                () -> accountService.deposit(
                        idempotencyKey,
                        "1",
                        BigDecimal.ZERO)
        );

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void getBalance_withExistingAccount_returnsBalance() {
        AccountEntity account = new AccountEntity("1");
        account.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getBalance("1");
        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(100)));

        verify(accountRepository).findByAccountNumber("1");
    }

    @Test
    void getBalance_withUnknownAccount_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("99"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getBalance("99"));

        verify(accountRepository).findByAccountNumber("99");
    }

    @Test
    void transfer_withExistingAccounts_returnsBalance() {
        String idempotencyKey = "transfer-key-123";

        AccountEntity sender = new AccountEntity("1");
        sender.setBalance(BigDecimal.valueOf(100));

        AccountEntity recipient = new AccountEntity("2");
        recipient.setBalance(BigDecimal.valueOf(50));

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("2"))
                .thenReturn(Optional.of(recipient));

        AccountResponse response = accountService.transfer(
                idempotencyKey,
                sender.getAccountNumber(),
                recipient.getAccountNumber(),
                BigDecimal.valueOf(40)
        );

        assertEquals(0, sender.getBalance().compareTo(BigDecimal.valueOf(60)));
        assertEquals(0, recipient.getBalance().compareTo(BigDecimal.valueOf(90)));
        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(60)));

        verify(accountRepository).findByAccountNumber("1");
        verify(accountRepository).findByAccountNumber("2");
    }

    @Test
    void transfer_withInvalidAmount_throwsInvalidAmountException() {
        String idempotencyKey = "transfer-key-123";

        assertThrows(InvalidAmountException.class, () -> accountService.transfer(
                        idempotencyKey,
                        "1",
                        "2",
                        BigDecimal.ZERO
                )
        );

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(accountRepository, never()).findByAccountNumber("2");
    }

    @Test
    void transfer_withSameAccountNumbers_throwsInvalidTransferException() {
        String idempotencyKey = "transfer-key-123";

        assertThrows(InvalidTransferException.class, () -> accountService.transfer(
                        idempotencyKey,
                        "1",
                        "1",
                        BigDecimal.valueOf(100)
                )
        );

        verify(accountRepository, never()).findByAccountNumber("1");
    }

    @Test
    void transfer_withUnknownSender_throwsAccountNotFoundException() {
        String idempotencyKey = "transfer-key-123";

        when(accountRepository.findByAccountNumber("99")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.transfer(
                idempotencyKey,
                "99",
                "1",
                BigDecimal.valueOf(100))
        );

        verify(accountRepository, never()).findByAccountNumber("1");
    }

    @Test
    void transfer_withUnknownRecipient_throwsAccountNotFoundException() {
        String idempotencyKey = "transfer-key-123";
        AccountEntity sender = new AccountEntity("1");

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("99")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.transfer(
                idempotencyKey,
                "1",
                "99",
                BigDecimal.valueOf(100))
        );
    }

    @Test
    void transfer_withInsufficientFunds_throwsInsufficientFundsException() {
        String idempotencyKey = "transfer-key-123";
        AccountEntity sender = new AccountEntity("1");
        AccountEntity recipient = new AccountEntity("2");

        sender.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("2")).thenReturn(Optional.of(recipient));

        assertThrows(InsufficientFundsException.class, () -> accountService.transfer(
                idempotencyKey,
                "1",
                "2",
                BigDecimal.valueOf(120))
        );
        assertEquals(0, sender.getBalance().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, recipient.getBalance().compareTo(BigDecimal.ZERO));

        verify(accountRepository).findByAccountNumber("1");
        verify(accountRepository).findByAccountNumber("2");
    }

    @Test
    void getTransactionHistory_withExistingTransactions_returnsTransactionResponses() {
        AccountEntity account = new AccountEntity("1");

        TransactionEntity entity1 = new TransactionEntity(
                "1",
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(100),
                null,
                null
        );

        TransactionEntity entity2 = new TransactionEntity(
                "1",
                TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(40),
                null,
                null
        );

        TransactionEntity entity3 = new TransactionEntity(
                "1",
                TransactionType.TRANSFER,
                BigDecimal.valueOf(60),
                "1",
                "2"
        );

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountNumberOrderByTimestampDesc("1"))
                .thenReturn(List.of(entity1, entity2, entity3));

        List<TransactionResponse> transactions = accountService.getTransactionHistory("1");

        assertEquals(0, transactions.getFirst().amount().compareTo(BigDecimal.valueOf(100)));
        assertEquals(TransactionType.WITHDRAWAL, transactions.get(1).type());
        assertEquals("2", transactions.getLast().recipientAccountNumber());

        verify(transactionRepository).findByAccountNumberOrderByTimestampDesc("1");
    }

    @Test
    void getTransactionHistory_withUnknownAccount_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("99"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getTransactionHistory("99"));

        verify(accountRepository).findByAccountNumber("99");
        verify(transactionRepository, never()).findByAccountNumberOrderByTimestampDesc(anyString());
    }

    @Test
    void getTransactionHistory_withNoTransactions_returnsEmptyList() {
        AccountEntity account = new AccountEntity("1");

        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountNumberOrderByTimestampDesc("1"))
                .thenReturn(List.of());

        List<TransactionResponse> transactions = accountService.getTransactionHistory("1");

        assertTrue(transactions.isEmpty());

        verify(accountRepository).findByAccountNumber("1");
        verify(transactionRepository).findByAccountNumberOrderByTimestampDesc("1");
    }

    @Test
    void deposit_withExistingAccount_savesDepositTransaction() {
        String idempotencyKey = "deposit-key-123";
        AccountEntity account = new AccountEntity("1");

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));

        accountService.deposit(idempotencyKey,
                "1",
                BigDecimal.valueOf(40)
        );

        ArgumentCaptor<TransactionEntity> captor =
                ArgumentCaptor.forClass(TransactionEntity.class);

        verify(transactionRepository).save(captor.capture());

        TransactionEntity savedTransaction = captor.getValue();

        assertEquals(TransactionType.DEPOSIT, savedTransaction.getType());
        assertEquals(0, savedTransaction.getAmount().compareTo(BigDecimal.valueOf(40)));
        assertNull(savedTransaction.getSenderAccountNumber());
        assertNull(savedTransaction.getRecipientAccountNumber());
    }

    @Test
    void withdraw_withExistingAccount_savesWithdrawTransaction() {
        String idempotencyKey = "withdraw-key-123";
        AccountEntity account = new AccountEntity("1");

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));

        account.setBalance(BigDecimal.valueOf(100));
        accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(70));

        ArgumentCaptor<TransactionEntity> captor =
                ArgumentCaptor.forClass(TransactionEntity.class);

        verify(transactionRepository).save(captor.capture());

        TransactionEntity savedTransaction = captor.getValue();

        assertEquals(TransactionType.WITHDRAWAL, savedTransaction.getType());
        assertEquals(0, savedTransaction.getAmount().compareTo(BigDecimal.valueOf(70)));
        assertNull(savedTransaction.getSenderAccountNumber());
        assertNull(savedTransaction.getRecipientAccountNumber());
    }

    @Test
    void transfer_withExistingAccounts_savesTransferTransactions() {
        String idempotencyKey = "transfer-key-123";
        AccountEntity senderAccount = new AccountEntity("1");
        AccountEntity recipientAccount = new AccountEntity("2");

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("2")).thenReturn(Optional.of(recipientAccount));

        senderAccount.setBalance(BigDecimal.valueOf(100));
        accountService.transfer(idempotencyKey, "1", "2", BigDecimal.valueOf(50));

        ArgumentCaptor<TransactionEntity> captor =
                ArgumentCaptor.forClass(TransactionEntity.class);

        verify(transactionRepository, times(2)).save(captor.capture());
        List<TransactionEntity> savedTransactions = captor.getAllValues();

        assertEquals(TransactionType.TRANSFER, savedTransactions.getFirst().getType());
        assertEquals("1", savedTransactions.getFirst().getSenderAccountNumber());
        assertEquals("2", savedTransactions.getFirst().getRecipientAccountNumber());
        assertEquals(0, savedTransactions.getFirst().getAmount().compareTo(BigDecimal.valueOf(50)));

        assertEquals(TransactionType.TRANSFER, savedTransactions.get(1).getType());
        assertEquals("1", savedTransactions.get(1).getSenderAccountNumber());
        assertEquals("2", savedTransactions.get(1).getRecipientAccountNumber());
        assertEquals(0, savedTransactions.get(1).getAmount().compareTo(BigDecimal.valueOf(50)));
    }

    @Test
    void transfer_withInsufficientFunds_doesNotSaveTransactionHistory() {
        String idempotencyKey = "transfer-key-123";
        AccountEntity senderAccount = new AccountEntity("1");
        AccountEntity recipientAccount = new AccountEntity("2");

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("2")).thenReturn(Optional.of(recipientAccount));

        senderAccount.setBalance(BigDecimal.valueOf(10));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.transfer(
                        idempotencyKey,
                        "1",
                        "2",
                        BigDecimal.valueOf(50))
        );

        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void withdraw_withInsufficientFunds_doesNotSaveTransactionHistory() {
        String idempotencyKey = "withdraw-key-123";
        AccountEntity account = new AccountEntity("1");

        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));

        account.setBalance(BigDecimal.valueOf(10));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(50)));

        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void withdraw_withExistingIdempotencyKey_returnsStoredResponse() {
        String idempotencyKey = "withdraw-key-123";
        String requestFingerprint = "WITHDRAWAL" + " | " + "1" + " | " + BigDecimal.valueOf(40);
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                "1",
                BigDecimal.valueOf(60),
                Instant.now()
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(record));

        AccountResponse response = accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(40));

        assertEquals("1", response.accountNumber());
        assertEquals(0, BigDecimal.valueOf(60).compareTo(response.balance()));

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void withdraw_withReusedIdempotencyKeyForDifferentRequest_throwsConflict() {
        String idempotencyKey = "withdraw-key-123";
        String requestFingerprint = "WITHDRAWAL" + " | " + "1" + " | " + BigDecimal.valueOf(40);
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                "1",
                BigDecimal.valueOf(60),
                Instant.now()
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(record));

        assertThrows(IdempotencyConflictException.class,
                () -> accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(60)));

        verify(accountRepository, never()).findByAccountNumber(anyString());
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void withdraw_withNewIdempotencyKey_savesIdempotencyRecord() {
        String idempotencyKey = "withdraw-key-123";
        AccountEntity account = new AccountEntity("1");
        account.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(account));
        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        AccountResponse response = accountService.withdraw(idempotencyKey, "1", BigDecimal.valueOf(40));
        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);

        verify(idempotencyRecordRepository).save(captor.capture());
        IdempotencyRecord savedRecord = captor.getValue();

        assertEquals(response.accountNumber(), savedRecord.getAccountNumber());
        assertEquals(0, BigDecimal.valueOf(60).compareTo(savedRecord.getBalance()));
        assertEquals(idempotencyKey, savedRecord.getIdempotencyKey());
        assertEquals(
                "WITHDRAWAL | 1 | 40",
                savedRecord.getRequestFingerprint()
        );
        assertNotNull(savedRecord.getCreatedAt());
    }

    @Test
    void deposit_withInvalidAmount_doesNotSaveTransactionHistory() {
        String idempotencyKey = "deposit-key-123";

        assertThrows(InvalidAmountException.class,
                () -> accountService.deposit(
                        idempotencyKey,
                        "1",
                        BigDecimal.valueOf(-50)));

        verify(accountRepository, never()).findByAccountNumber(anyString());
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void deposit_withExistingIdempotencyKey_returnsStoredResponse() {
        String idempotencyKey = "deposit-key-123";
        String fingerprint = "DEPOSIT" + " | " + "1" + " | " + "100";
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                fingerprint,
                "1",
                BigDecimal.valueOf(100),
                Instant.now()
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        AccountResponse response = accountService.deposit(
                idempotencyKey,
                "1",
                BigDecimal.valueOf(100)
        );

        assertEquals("1", response.accountNumber());
        assertEquals(
                0,
                response.balance().compareTo(BigDecimal.valueOf(100))
        );

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void deposit_withReusedIdempotencyKeyForDifferentRequest_throwsConflict() {
        String idempotencyKey = "deposit-key-123";
        String fingerprint = "DEPOSIT" + " | " + "1" + " | " + "100";
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                fingerprint,
                "1",
                BigDecimal.valueOf(100),
                Instant.now()
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        assertThrows(IdempotencyConflictException.class,
                () -> accountService.deposit(
                        idempotencyKey,
                        "1",
                        BigDecimal.valueOf(200)
                ));

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void deposit_withNewIdempotencyKey_savesIdempotencyRecord() {
        String idempotencyKey = "deposit-key-123";

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.empty());

        AccountResponse response = accountService.deposit(idempotencyKey, "1", BigDecimal.valueOf(100));

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);

        verify(idempotencyRecordRepository).save(captor.capture());

        IdempotencyRecord savedRecord = captor.getValue();
        assertEquals(response.accountNumber(), savedRecord.getAccountNumber());
        assertEquals(response.balance(), savedRecord.getBalance());
        assertEquals(idempotencyKey, savedRecord.getIdempotencyKey());
        assertNotNull(savedRecord.getCreatedAt());

        String expectedFingerprint = "DEPOSIT" + " | " + response.accountNumber() + " | " + BigDecimal.valueOf(100);
        assertEquals(expectedFingerprint, savedRecord.getRequestFingerprint());
    }

    @Test
    void transfer_withExistingIdempotencyKey_returnsStoredResponse() {
        String idempotencyKey = "transfer-key-123";
        BigDecimal amount = BigDecimal.valueOf(40);
        String requestFingerprint = "TRANSFER" + " | "
                + "1" + " | "
                + "2" + " | "
                + amount;
        IdempotencyRecord existingRecord = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                "1",
                BigDecimal.valueOf(60),
                Instant.now()
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingRecord));

        AccountResponse response = accountService.transfer(
                idempotencyKey,
                "1",
                "2",
                amount
        );

        assertEquals("1", response.accountNumber());
        assertEquals(0, existingRecord.getBalance().compareTo(response.balance()));

        verify(accountRepository, never()).findByAccountNumber(anyString());
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
        verify(idempotencyRecordRepository, never()).save(any(IdempotencyRecord.class));
    }

    @Test
    void transfer_withReusedIdempotencyKeyForDifferentRequest_throwsConflict() {
        String idempotencyKey = "transfer-key-123";
        String requestFingerprint = "TRANSFER" + " | "
                + "1" + " | "
                + "2" + " | "
                + BigDecimal.valueOf(40);
        IdempotencyRecord existingRecord = new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                "1",
                BigDecimal.valueOf(60),
                Instant.now()
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingRecord));

        assertThrows(IdempotencyConflictException.class,
                () -> accountService.transfer(
                        idempotencyKey,
                        "1",
                        "2",
                        BigDecimal.valueOf(50)
                )
        );
        verify(accountRepository, never()).findByAccountNumber(anyString());
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
        verify(idempotencyRecordRepository, never()).save(any(IdempotencyRecord.class));
    }

    @Test
    void transfer_withNewIdempotencyKey_savesIdempotencyRecord() {
        String idempotencyKey = "transfer-key-123";
        AccountEntity sender = new AccountEntity("1");
        sender.setBalance(BigDecimal.valueOf(100));
        AccountEntity receiver = new AccountEntity("2");

        when(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("2")).thenReturn(Optional.of(receiver));

        AccountResponse response = accountService.transfer(
                idempotencyKey,
                "1",
                "2",
                BigDecimal.valueOf(40)
        );

        assertEquals("1", response.accountNumber());
        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(60)));

        verify(transactionRepository, times(2)).save(any(TransactionEntity.class));

        ArgumentCaptor<IdempotencyRecord> captor =
                ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).save(captor.capture());
        IdempotencyRecord savedRecord = captor.getValue();

        assertEquals("1", savedRecord.getAccountNumber());
        assertEquals(idempotencyKey, savedRecord.getIdempotencyKey());
        assertNotNull(savedRecord.getCreatedAt());
        assertEquals(0, savedRecord.getBalance().compareTo(BigDecimal.valueOf(60)));

        String expectedFingerprint = "TRANSFER" + " | "
                + "1" + " | "
                + "2" + " | "
                + BigDecimal.valueOf(40);
        assertEquals(expectedFingerprint, savedRecord.getRequestFingerprint());
    }
}
