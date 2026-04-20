package com.forge.engine.pricing;

import com.forge.engine.model.Bid;

public interface PricingStrategy {
    boolean isValid(Bid newBid, Bid currentBestBid);
}