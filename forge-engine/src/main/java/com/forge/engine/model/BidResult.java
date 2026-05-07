package com.forge.engine.model;

public record BidResult(
        boolean accepted,
        String reason,
        Bid bid,
        Money currentPrice
) {
    public static BidResult accepted(Bid bid, Money currentPrice) {
        return new BidResult(true, "Bid accepted", bid, currentPrice);
    }

    public static BidResult rejected(String reason) {
        return new BidResult(false, reason, null, null);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isRejected() {
        return !accepted;
    }
}