package com.forge.engine.pricing;

import com.forge.engine.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EnglishAuctionPricingTest {

    private final EnglishAuctionPricing strategy = new EnglishAuctionPricing();

    @Test
    void isValidIncrement_returnsTrue_whenBidExceedsFivePercent() {
        Money current = Money.inr(1000);
        Money newBid = Money.inr(1060); // 6% above — valid
        assertTrue(strategy.isValidIncrement(current, newBid));
    }

    @Test
    void isValidIncrement_returnsTrue_whenBidExactlyFivePercent() {
        Money current = Money.inr(1000);
        Money newBid = Money.inr(1050); // exactly 5% — boundary valid
        assertTrue(strategy.isValidIncrement(current, newBid));
    }

    @Test
    void isValidIncrement_returnsFalse_whenBidBelowFivePercent() {
        Money current = Money.inr(1000);
        Money newBid = Money.inr(1020); // only 2% — rejected
        assertFalse(strategy.isValidIncrement(current, newBid));
    }

    @Test
    void isValidIncrement_returnsFalse_whenBidEqualToCurrent() {
        Money current = Money.inr(2000);
        assertFalse(strategy.isValidIncrement(current, Money.inr(2000)));
    }

    @Test
    void isValidIncrement_returnsFalse_whenBidLower() {
        Money current = Money.inr(5000);
        assertFalse(strategy.isValidIncrement(current, Money.inr(4000)));
    }

    @Test
    void calculateNextMinimum_returnsCorrectFivePercentAbove() {
        Money current = Money.inr(1000);
        Money next = strategy.calculateNextMinimum(current);

        assertEquals(new BigDecimal("1050.00"), next.amount());
        assertEquals("INR", next.currency());
    }

    @Test
    void calculateNextMinimum_handlesLargeAmounts() {
        Money current = Money.inr(100_000);
        Money next = strategy.calculateNextMinimum(current);

        assertEquals(new BigDecimal("105000.00"), next.amount());
    }

    @Test
    void calculateNextMinimum_preservesCurrency() {
        Money current = new Money(new BigDecimal("500"), "USD");
        Money next = strategy.calculateNextMinimum(current);

        assertEquals("USD", next.currency());
    }
}

class DutchAuctionPricingTest {

    private final DutchAuctionPricing strategy = new DutchAuctionPricing();

    @Test
    void isValidIncrement_returnsTrue_whenBidEqualsCurrentPrice() {
        // Dutch: first bid >= current asking price wins
        Money current = Money.inr(3000);
        assertTrue(strategy.isValidIncrement(current, Money.inr(3000)));
    }

    @Test
    void isValidIncrement_returnsTrue_whenBidAboveCurrent() {
        Money current = Money.inr(3000);
        assertTrue(strategy.isValidIncrement(current, Money.inr(5000)));
    }

    @Test
    void isValidIncrement_returnsFalse_whenBidBelowCurrent() {
        Money current = Money.inr(3000);
        assertFalse(strategy.isValidIncrement(current, Money.inr(2999)));
    }

    @Test
    void calculateNextMinimum_returnsSamePrice() {
        // Dutch pricing: next minimum = current (timer handles decrease)
        Money current = Money.inr(4000);
        Money next = strategy.calculateNextMinimum(current);

        assertEquals(current.amount(), next.amount());
        assertEquals(current.currency(), next.currency());
    }

    @Test
    void calculateNextMinimum_doesNotDecrease() {
        Money current = Money.inr(1500);
        Money next = strategy.calculateNextMinimum(current);

        assertTrue(next.amount().compareTo(current.amount()) >= 0);
    }
}

class SealedBidPricingTest {

    private final SealedBidPricing strategy = new SealedBidPricing();

    @Test
    void isValidIncrement_returnsTrue_forAnyPositiveBid() {
        Money current = Money.inr(5000);
        assertTrue(strategy.isValidIncrement(current, Money.inr(1))); // Even 1 rupee is valid
    }

    @Test
    void isValidIncrement_returnsTrue_evenIfBidLowerThanCurrent() {
        // Sealed bid — bids are blind, no visibility into current price
        Money current = Money.inr(9999);
        assertTrue(strategy.isValidIncrement(current, Money.inr(100)));
    }

    @Test
    void isValidIncrement_returnsFalse_forZeroBid() {
        Money current = Money.inr(1000);
        assertFalse(strategy.isValidIncrement(current, Money.inr(0)));
    }

    @Test
    void isValidIncrement_returnsFalse_forNegativeBid() {
        Money current = Money.inr(1000);
        assertFalse(strategy.isValidIncrement(current, Money.inr(-500)));
    }

    @Test
    void calculateNextMinimum_returnsZero() {
        // Sealed bid: no next minimum shown to user
        Money current = Money.inr(5000);
        Money next = strategy.calculateNextMinimum(current);

        assertEquals(new BigDecimal("0.00"), next.amount());
    }

    @Test
    void calculateNextMinimum_preservesCurrency() {
        Money current = new Money(new BigDecimal("1000"), "USD");
        Money next = strategy.calculateNextMinimum(current);

        assertEquals("USD", next.currency());
    }
}