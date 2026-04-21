package com.forge.engine.pricing;

import com.forge.engine.model.Bid;

public class SealedBidPricing implements PricingStrategy {
    @Override
    public boolean isValid(Bid newBid, Bid currentBestBid) {
        return true;
    }
}