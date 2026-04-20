package com.forge.engine.model;

import java.time.Instant;

public record BidKey(Money amount, Instant timestamp) implements Comparable<BidKey> {
    @Override
    public int compareTo(BidKey other) {
        int priceCompare = other.amount().compareTo(this.amount());
        if (priceCompare != 0) return priceCompare;
        return this.timestamp().compareTo(other.timestamp());
    }
}