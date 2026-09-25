package com.eddiapps.banking.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String accountNumber;
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    @Version
    private Long version;

    protected AccountEntity() {
    }

    public AccountEntity(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getVersion() {
        return version;
    }
}
