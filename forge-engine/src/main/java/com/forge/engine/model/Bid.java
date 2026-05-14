package com.forge.engine.model;

import java.time.Instant;

public record Bid(BidKey bidKey, String bidderId) {


    public Bid(String bidderId, Money price) {
        this(new BidKey(price, Instant.now()), bidderId);
    }

    public Money getPrice() {
        return bidKey.amount();
    }

    public String getBidderId() {
        return bidderId;
    }

    public Instant getTimestamp() {
        return bidKey.timestamp();
    }

    // --- NEW GETTER FOR ENGINE (PriceTracker error solve karne ke liye) ---
    public Money amount() {
        return bidKey.amount();
    }
}