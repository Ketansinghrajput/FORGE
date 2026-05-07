package com.forge.engine.settlement;

import com.forge.engine.bidding.BidBook;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;

import java.util.Optional;

public class SettlementCalculator {

    private final FeeCalculator feeCalculator = new FeeCalculator();

    public SettlementResult calculate(Long auctionId, BidBook bidBook) {
        Bid winningBid = bidBook.getBestBid();

        if (winningBid == null) {
            return SettlementResult.unsold(auctionId);
        }

        Money finalPrice = winningBid.getPrice();
        Money platformFee = feeCalculator.calculatePlatformFee(finalPrice);
        Money tax = feeCalculator.calculateTax(platformFee);
        Money sellerPayout = feeCalculator.calculateSellerPayout(finalPrice);

        return new SettlementResult(
                auctionId,
                winningBid.getBidderId(),
                finalPrice,
                platformFee,
                tax,
                sellerPayout,
                true
        );
    }
}