package com.forge.engine.settlement;

import com.forge.engine.model.Money;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeeCalculatorTest {

    private final FeeCalculator feeCalculator = new FeeCalculator();

    @Test
    void testCalculatePlatformFee() {
        Money price = Money.inr(1000); // Using the static method from your record
        Money fee = feeCalculator.calculatePlatformFee(price);
        assertNotNull(fee, "Platform fee should not be null");
    }

    @Test
    void testCalculateTax() {
        Money fee = Money.inr(50);
        Money tax = feeCalculator.calculateTax(fee);
        assertNotNull(tax, "Tax should not be null");
    }

    @Test
    void testCalculateSellerPayout() {
        Money price = Money.inr(1000);
        Money payout = feeCalculator.calculateSellerPayout(price);
        assertNotNull(payout, "Seller payout should not be null");
    }
}