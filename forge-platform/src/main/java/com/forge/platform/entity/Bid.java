package com.forge.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_bid_auction", columnList = "auction_id"),
        @Index(name = "idx_bid_bidder", columnList = "bidder_id"),
        @Index(name = "idx_bid_amount", columnList = "amount DESC")
})
@Getter
// @Setter class level se HATA DIYA HAI - Bids should be immutable!
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Bid extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, updatable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false, updatable = false)
    private User bidder;

    @Column(nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private boolean successful = false;

}