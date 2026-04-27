package com.forge.engine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    // Primary Constructor Validation
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        // Hamesha 2 decimal places pe scale set karo (e.g., 10 -> 10.00)
        // Taaki comparison mein precision ka jhagda na ho
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    // Helper: Sirf amount se INR banane ke liye (App.java ke liye)
    public Money(BigDecimal amount) {
        this(amount, "INR");
    }

    // Static Factory Method (Jo tune banaya tha, optimized)
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    // Getter for amount (App.java aur controllers ke liye)
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public int compareTo(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies: "
                    + this.currency + " vs " + other.currency);
        }
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return currency + " " + amount;
    }
}