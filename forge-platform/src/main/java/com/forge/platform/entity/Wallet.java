package com.forge.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lockedAmount = BigDecimal.ZERO;

    @Version
    private Long version; // Optimistic Locking for concurrent bids

    public BigDecimal getAvailableBalance() {
        return totalBalance.subtract(lockedAmount);
    }
}