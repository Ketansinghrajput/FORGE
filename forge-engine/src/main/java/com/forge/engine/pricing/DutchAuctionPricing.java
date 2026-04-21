package com.forge.engine.pricing;

import com.forge.engine.model.Bid;

public class DutchAuctionPricing implements PricingStrategy {
    @Override
    public boolean isValid(Bid newBid, Bid currentBestBid) {
        return currentBestBid == null;
    }
}