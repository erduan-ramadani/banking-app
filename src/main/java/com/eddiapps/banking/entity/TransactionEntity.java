package com.eddiapps.banking.entity;

import com.eddiapps.banking.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

@Entity
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String accountNumber;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private Instant timestamp;
    private String senderAccountNumber;
    private String recipientAccountNumber;

    protected TransactionEntity() {
    }

    public TransactionEntity(String accountNumber, TransactionType type, BigDecimal amount) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.timestamp = Instant.now();
    }

    public TransactionEntity(String accountNumber, TransactionType type, BigDecimal amount, String senderAccountNumber, String recipientAccountNumber) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.timestamp = Instant.now();
        this.senderAccountNumber = senderAccountNumber;
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSenderAccountNumber() {
        return senderAccountNumber;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }
}
