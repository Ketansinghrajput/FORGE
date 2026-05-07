package com.forge.engine.settlement;

import com.forge.engine.model.Money;

public record SettlementResult(
        Long auctionId,
        String winnerId,
        Money finalPrice,
        Money platformFee,
        Money taxAmount,
        Money sellerPayout,
        boolean settled
) {
    public static SettlementResult unsold(Long auctionId) {
        return new SettlementResult(
                auctionId,
                null,
                Money.inr(0),
                Money.inr(0),
                Money.inr(0),
                Money.inr(0),
                false
        );
    }
}