package com.forge.engine.pricing;

import com.forge.engine.model.Money;

public interface PricingStrategy {
    boolean isValidIncrement(Money currentPrice, Money newBidPrice);

    Money calculateNextMinimum(Money currentPrice);
}