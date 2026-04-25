package com.forge.engine.model;

import java.time.Instant;

public class Bid {
    private final String bidderId;
    private final Money price;
    private final Instant timestamp; // Nayi field add ki

    public Bid(String bidderId, Money price) {
        this.bidderId = bidderId;
        this.price = price;
        this.timestamp = Instant.now(); // Jaise hi object banega, time record ho jayega!
    }

    // Getters
    public Money getPrice() {
        return price;
    }

    public String getBidderId() {
        return bidderId;
    }

    public Instant getTimestamp() { // Getter for timestamp
        return timestamp;
    }
}