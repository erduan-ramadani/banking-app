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

}
