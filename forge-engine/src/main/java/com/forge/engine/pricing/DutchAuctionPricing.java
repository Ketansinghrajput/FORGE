package com.forge.engine.pricing;

import com.forge.engine.model.Money;

public class DutchAuctionPricing implements PricingStrategy {

    @Override
    public boolean isValidIncrement(Money currentPrice, Money newBidPrice) {
        // Dutch auction mein price reverse tick hota hai.
        // Pehla valid bid jo current asking price ke equal ya usse zyada ho, wo jeet jata hai.
        return newBidPrice.amount().compareTo(currentPrice.amount()) >= 0;
    }

    @Override
    public Money calculateNextMinimum(Money currentPrice) {
        // Dutch auction mein next minimum backend timer se decrease hota hai.
        // Interface rule satisfy karne ke liye hum current state return kar rahe hain.
        return new Money(currentPrice.amount(), currentPrice.currency());
    }
}