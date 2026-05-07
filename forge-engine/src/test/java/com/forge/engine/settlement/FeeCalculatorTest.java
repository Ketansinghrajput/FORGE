package com.forge.engine.settlement;

import com.forge.engine.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeeCalculatorTest {

    private FeeCalculator feeCalculator;

    @BeforeEach
    void setUp() {
        feeCalculator = new FeeCalculator();
    }

    @Test
    void shouldCalculate5PercentPlatformFee() {
        Money finalPrice = Money.inr(10000);
        Money fee = feeCalculator.calculatePlatformFee(finalPrice);
        assertEquals(Money.inr(500), fee);
    }

    @Test
    void shouldCalculate18PercentGSTOnFee() {
        Money fee = Money.inr(500);
        Money tax = feeCalculator.calculateTax(fee);
        assertEquals(Money.inr(90), tax);
    }

    @Test
    void shouldCalculateCorrectSellerPayout() {
        Money finalPrice = Money.inr(10000);
        Money payout = feeCalculator.calculateSellerPayout(finalPrice);
        // 10000 - 500 (fee) - 90 (tax on fee) = 9410
        assertEquals(Money.inr(9410), payout);
    }
}