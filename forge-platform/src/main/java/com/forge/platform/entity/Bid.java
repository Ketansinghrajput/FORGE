package com.forge.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Bid extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "seller", "highestBidder"})
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "authorities", "walletBalance"})
    private User bidder;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false)
    private boolean successful = false; // By default bid successful nahi hoti jab tak auction end na ho

    // 🚨 SENSEI REALITY CHECK:
    // Agar tere 'BaseEntity' class mein already 'createdAt' ya 'createdDate' hai,
    // toh isko yahan se HATA DE, warna JPA confuse ho jayega.
    // Agar BaseEntity mein nahi hai, toh hi isko rakhna.

}