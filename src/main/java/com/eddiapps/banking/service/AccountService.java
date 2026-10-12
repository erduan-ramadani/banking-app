package com.eddiapps.banking.service;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public AccountResponse deposit(String idempotencyKey, String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be > 0");
        }

        String requestFingerprint = "DEPOSIT" + " | " + accountNumber + " | " + amount;
        Optional<AccountResponse> idempotentResponse = findStoredIdempotentResponse(idempotencyKey, requestFingerprint);
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }

        Optional<AccountEntity> existingAccount =
                accountRepository.findByAccountNumber(accountNumber);

        AccountEntity account = existingAccount
                .orElseGet(() -> new AccountEntity(accountNumber));

        account.setBalance(account.getBalance().add(amount));

        if (existingAccount.isEmpty()) {
            accountRepository.save(account);
        }

        transactionRepository.save(new TransactionEntity(
                accountNumber,
                TransactionType.DEPOSIT,
                amount,
                null,
                null)
        );

        idempotencyRecordRepository.save(new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                accountNumber,
                account.getBalance(),
                Instant.now()
        ));
        return new AccountResponse(accountNumber, account.getBalance());
    }
    
    @Transactional
    public AccountResponse withdraw(String idempotencyKey, String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be > 0");
        }

        String requestFingerprint = "WITHDRAWAL" + " | " + accountNumber + " | " + amount;
        Optional<AccountResponse> idempotentResponse = findStoredIdempotentResponse(idempotencyKey, requestFingerprint);
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }

        AccountEntity account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));

        transactionRepository.save(new TransactionEntity(
                accountNumber,
                TransactionType.WITHDRAWAL,
                amount,
                null,
                null)
        );

        idempotencyRecordRepository.save(
                new IdempotencyRecord(
                        idempotencyKey,
                        requestFingerprint,
                        accountNumber,
                        account.getBalance(),
                        Instant.now()
                )
        );

        return new AccountResponse(accountNumber, account.getBalance());
    }

    @Transactional
    public AccountResponse transfer(
            String idempotencyKey,
            String senderAccountNumber,
            String recipientAccountNumber,
            BigDecimal amount
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be > 0");
        }

        String requestFingerprint = "TRANSFER" + " | "
                + senderAccountNumber + " | "
                + recipientAccountNumber + " | "
                + amount;
        Optional<AccountResponse> idempotentResponse = findStoredIdempotentResponse(idempotencyKey, requestFingerprint);
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }

        if (senderAccountNumber.equals(recipientAccountNumber)) {
            throw new InvalidTransferException("Sender and recipient must be different");
        }

        AccountEntity senderAccount = accountRepository
                .findByAccountNumber(senderAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found"));
        AccountEntity recipientAccount = accountRepository
                .findByAccountNumber(recipientAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Recipient account not found"));

        if (senderAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        transactionRepository.save(new TransactionEntity(
                        senderAccountNumber,
                        TransactionType.TRANSFER,
                        amount,
                        senderAccountNumber,
                        recipientAccountNumber
                )
        );

        transactionRepository.save(new TransactionEntity(
                        recipientAccountNumber,
                        TransactionType.TRANSFER,
                        amount,
                        senderAccountNumber,
                        recipientAccountNumber
                )
        );

        idempotencyRecordRepository.save(new IdempotencyRecord(
                idempotencyKey,
                requestFingerprint,
                senderAccountNumber,
                senderAccount.getBalance(),
                Instant.now()
        ));

        return new AccountResponse(senderAccountNumber, senderAccount.getBalance());
    }

    @Transactional(readOnly = true)
    public AccountResponse getBalance(String accountNumber) {
        AccountEntity account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return new AccountResponse(accountNumber, account.getBalance());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        accountRepository.findByAccountNumber(accountNumber).orElseThrow(
                () -> new AccountNotFoundException("Account not found"));

        List<TransactionEntity> transactions = transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber);
        return transactions.stream().map(entity ->
                new TransactionResponse(
                        entity.getType(),
                        entity.getAmount(),
                        entity.getTimestamp(),
                        entity.getSenderAccountNumber(),
                        entity.getRecipientAccountNumber()
                )
        ).toList();
    }

    private Optional<AccountResponse> findStoredIdempotentResponse(String idempotencyKey, String requestFingerprint) {
        Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotencyRecord idempotencyRecord = existingRecord.get();
            if (!idempotencyRecord.getRequestFingerprint().equals(requestFingerprint)) {
                throw new IdempotencyConflictException("Idempotency key already used for a different request");
            }

            return Optional.of(new AccountResponse(
                    idempotencyRecord.getAccountNumber(),
                    idempotencyRecord.getBalance()
            ));
        }
        return Optional.empty();
    }
}
