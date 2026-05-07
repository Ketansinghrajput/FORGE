package com.forge.engine.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithCorrectScale() {
        Money money = Money.inr(100);
        assertEquals(2, money.amount().scale());
    }

    @Test
    void shouldAddTwoMoneyValues() {
        Money a = Money.inr(1000);
        Money b = Money.inr(500);
        assertEquals(Money.inr(1500), a.add(b));
    }

    @Test
    void shouldSubtractMoneyValues() {
        Money a = Money.inr(1000);
        Money b = Money.inr(300);
        assertEquals(Money.inr(700), a.subtract(b));
    }

    @Test
    void shouldMultiplyByFactor() {
        Money fee = Money.inr(1000).multiply(0.05);
        assertEquals(Money.inr(50), fee);
    }

    @Test
    void shouldCompareCorrectly() {
        Money low = Money.inr(100);
        Money high = Money.inr(200);
        assertTrue(high.isGreaterThan(low));
        assertFalse(low.isGreaterThan(high));
    }

    @Test
    void shouldThrowExceptionForDifferentCurrencies() {
        Money inr = new Money(new BigDecimal("100"), "INR");
        Money usd = new Money(new BigDecimal("100"), "USD");
        assertThrows(IllegalArgumentException.class, () -> inr.add(usd));
    }

    @Test
    void shouldDetectZeroOrNegative() {
        assertTrue(Money.inr(0).isZeroOrNegative());
        assertTrue(Money.inr(-1).isZeroOrNegative());
        assertFalse(Money.inr(1).isZeroOrNegative());
    }
}