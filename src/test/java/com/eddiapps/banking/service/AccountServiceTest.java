package com.eddiapps.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.eddiapps.banking.dto.AccountResponse;
import com.eddiapps.banking.entity.AccountEntity;
import com.eddiapps.banking.exception.AccountNotFoundException;
import com.eddiapps.banking.exception.InsufficientFundsException;
import com.eddiapps.banking.exception.InvalidAmountException;
import com.eddiapps.banking.repository.AccountRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void withdraw_withSufficientFunds_returnsUpdatedBalance() {
        AccountEntity accountEntity = new AccountEntity("1");
        accountEntity.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(accountEntity));

        AccountResponse response = accountService.withdraw("1", BigDecimal.valueOf(40));

        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(60)));
        assertEquals(
                0,
                accountEntity.getBalance().compareTo(BigDecimal.valueOf(60))
        );

        verify(accountRepository).findByAccountNumber("1");
    }

    @Test
    void withdraw_withInsufficientFunds_throwsInsufficientFundsException() {
        AccountEntity accountEntity = new AccountEntity("1");
        accountEntity.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(accountEntity));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.withdraw("1", BigDecimal.valueOf(140)));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_withUnknownAccount_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("99"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.withdraw("99", BigDecimal.valueOf(140)));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_withInvalidAmount_throwsInvalidAmountException() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.withdraw("1", BigDecimal.valueOf(0)));

        verify(accountRepository, never()).findByAccountNumber("1");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_existingAccount_returnsUpdatedBalance() {
        AccountEntity accountEntity = new AccountEntity("1");
        accountEntity.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("1"))
                .thenReturn(Optional.of(accountEntity));

        AccountResponse response = accountService.deposit("1", BigDecimal.valueOf(40));

        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(140)));
        assertEquals(
                0,
                accountEntity.getBalance().compareTo(BigDecimal.valueOf(140))
        );

        verify(accountRepository).findByAccountNumber("1");
    }

    @Test
    void deposit_unknownAccount_createsAndSavesAccount() {
        when(accountRepository.findByAccountNumber("2"))
                .thenReturn(Optional.empty());

        AccountResponse response = accountService.deposit("2", BigDecimal.valueOf(100));

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
        assertThrows(InvalidAmountException.class,
                () -> accountService.deposit("1", BigDecimal.ZERO)
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

}
