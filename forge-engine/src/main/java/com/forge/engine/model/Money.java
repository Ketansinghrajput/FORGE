package com.forge.engine.model;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    // Helper method to easily create Money objects
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    @Override
    public int compareTo(Money other) {
        // Warning: Assuming same currency for now to keep it simple
        return this.amount().compareTo(other.amount());
    }
}