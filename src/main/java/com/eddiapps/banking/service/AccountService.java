package com.eddiapps.banking.service;

import com.eddiapps.banking.dto.AccountResponse;
import com.eddiapps.banking.entity.AccountEntity;
import com.eddiapps.banking.exception.AccountNotFoundException;
import com.eddiapps.banking.exception.InsufficientFundsException;
import com.eddiapps.banking.exception.InvalidAmountException;
import com.eddiapps.banking.exception.InvalidTransferException;
import com.eddiapps.banking.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse withdraw(String accountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be > 0");
        }

        AccountEntity account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        return new AccountResponse(accountNumber, account.getBalance());
    }

    @Transactional
    public AccountResponse deposit(String accountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be > 0");
        }

        Optional<AccountEntity> existingAccount =
                accountRepository.findByAccountNumber(accountNumber);

        AccountEntity account = existingAccount
                .orElseGet(() -> new AccountEntity(accountNumber));

        account.setBalance(account.getBalance().add(amount));

        if (existingAccount.isEmpty()) {
            accountRepository.save(account);
        }

        return new AccountResponse(accountNumber, account.getBalance());
    }

    public AccountResponse getBalance(String accountNumber) {
        AccountEntity account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return new AccountResponse(accountNumber, account.getBalance());
    }

    @Transactional
    public AccountResponse transfer(String senderAccountNumber, String recipientAccountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be > 0");
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
        return new AccountResponse(senderAccountNumber, senderAccount.getBalance());
    }
}
