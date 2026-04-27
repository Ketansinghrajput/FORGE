package com.forge.engine.model;

import java.time.Instant;

public record Bid(BidKey bidKey, String bidderId) {

    // Backward Compatibility Constructor: Tera purana code yahi constructor call karega
    // aur yeh automatically naya BidKey object generate kar lega!
    public Bid(String bidderId, Money price) {
        this(new BidKey(price, Instant.now()), bidderId);
    }

    // --- OLD GETTERS (Taaki tera bacha hua code break na ho) ---
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