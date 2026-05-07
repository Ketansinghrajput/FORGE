package com.forge.engine.settlement;

import com.forge.engine.model.Money;

public class FeeCalculator {

    private static final double PLATFORM_FEE_PERCENT = 5.0;  // 5% platform fee
    private static final double TAX_PERCENT = 18.0;           // 18% GST on fee

    public Money calculatePlatformFee(Money finalPrice) {
        return finalPrice.multiply(PLATFORM_FEE_PERCENT / 100.0);
    }

    public Money calculateTax(Money platformFee) {
        return platformFee.multiply(TAX_PERCENT / 100.0);
    }

    public Money calculateSellerPayout(Money finalPrice) {
        Money platformFee = calculatePlatformFee(finalPrice);
        Money tax = calculateTax(platformFee);
        return finalPrice.subtract(platformFee).subtract(tax);
    }
}