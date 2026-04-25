package com.forge.engine.pricing;

import com.forge.engine.model.Money;

public class SealedBidPricing implements PricingStrategy {

    @Override
    public boolean isValidIncrement(Money currentPrice, Money newBidPrice) {
        // Sealed bid (Blind auction) mein koi minimum current price nahi dikhta.
        // Har bid valid hoti hai (sirf wallet balance check hota hai).
        return newBidPrice.amount().compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    @Override
    public Money calculateNextMinimum(Money currentPrice) {
        // Blind auction mein user ko next minimum dikhana allow nahi hota.
        return new Money(java.math.BigDecimal.ZERO, currentPrice.currency());
    }
}