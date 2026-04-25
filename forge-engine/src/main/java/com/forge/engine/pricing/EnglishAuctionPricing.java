package com.forge.engine.pricing;

import com.forge.engine.model.Money;
import java.math.BigDecimal;

public class EnglishAuctionPricing implements PricingStrategy {
    private static final double MIN_INCREMENT_PERCENT = 0.05; // 5%

    @Override
    public boolean isValidIncrement(Money currentPrice, Money newBidPrice) {
        // BigDecimal ke sath operations .multiply() se hote hain
        BigDecimal multiplier = BigDecimal.valueOf(1 + MIN_INCREMENT_PERCENT);
        BigDecimal minRequired = currentPrice.amount().multiply(multiplier);

        // .compareTo() return >= 0 if newBidPrice is greater or equal
        return newBidPrice.amount().compareTo(minRequired) >= 0;
    }

    @Override
    public Money calculateNextMinimum(Money currentPrice) {
        BigDecimal multiplier = BigDecimal.valueOf(1 + MIN_INCREMENT_PERCENT);
        BigDecimal newAmount = currentPrice.amount().multiply(multiplier);

        // Yahan amount ke sath currentPrice.currency() bhi pass karna hai
        return new Money(newAmount, currentPrice.currency());
    }
}