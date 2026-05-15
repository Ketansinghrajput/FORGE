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
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    // Optimistic locking — prevents concurrent bids corrupting balance
    @Version
    @Column(nullable = false)
    private Integer version;
}