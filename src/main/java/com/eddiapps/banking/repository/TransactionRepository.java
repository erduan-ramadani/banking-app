package com.eddiapps.banking.repository;

import com.eddiapps.banking.entity.TransactionEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByAccountNumberOrderByTimestampDesc(String accountNumber);
}
