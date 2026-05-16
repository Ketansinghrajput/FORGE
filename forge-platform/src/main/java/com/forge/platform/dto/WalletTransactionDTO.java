package com.forge.platform.dto;

import com.forge.platform.enums.TransactionReason;
import com.forge.platform.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionDTO(
        Long id,
        TransactionType type,
        TransactionReason reason,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long referenceId,
        String description,
        LocalDateTime createdAt
) {
    public static WalletTransactionDTO from(com.forge.platform.entity.WalletTransaction tx) {
        return new WalletTransactionDTO(
                tx.getId(),
                tx.getType(),
                tx.getReason(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getReferenceId(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}