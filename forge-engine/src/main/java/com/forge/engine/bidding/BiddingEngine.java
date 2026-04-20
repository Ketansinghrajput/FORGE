package com.forge.engine.bidding;

import com.forge.engine.auction.AuctionType;
import com.forge.engine.event.EventBus;
import com.forge.engine.model.Bid;
import com.forge.engine.pricing.PricingStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BiddingEngine {
    private final Map<Long, BidBook> auctionBids = new ConcurrentHashMap<>();
    private final Map<AuctionType, PricingStrategy> strategies;
    private final EventBus eventBus;

    public BiddingEngine(Map<AuctionType, PricingStrategy> strategies, EventBus eventBus) {
        this.strategies = strategies;
        this.eventBus = eventBus;
    }

    public void processBid(Bid bid, AuctionType type) {
        BidBook book = auctionBids.computeIfAbsent(bid.auctionId(), k -> new BidBook());
        PricingStrategy strategy = strategies.get(type);

        if (strategy != null && strategy.isValid(bid, book.getBestBid())) {
            book.addBid(bid);
            // Future: EventBus triggers here
        }
    }
}