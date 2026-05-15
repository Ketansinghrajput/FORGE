package com.forge.platform.entity;

import com.forge.platform.enums.TransactionReason;
import com.forge.platform.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class WalletTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // CREDIT | DEBIT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionReason reason; // BID_PLACED | BID_REFUND | PURCHASE | TOP_UP | WITHDRAWAL

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Snapshot of balance after this tx — no recomputation needed
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    // Links back to Bid/Order — nullable for TOP_UP / WITHDRAWAL
    @Column
    private Long referenceId;

    @Column
    private String description;
}