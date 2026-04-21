package com.forge.engine.pricing;

import com.forge.engine.model.Bid;

public class EnglishAuctionPricing implements PricingStrategy {

    @Override
    public boolean isValid(Bid newBid, Bid currentBestBid) {
        if (currentBestBid == null) {
            return true;
        }

        return newBid.amount().compareTo(currentBestBid.amount()) > 0;
    }
}