package com.forge.engine.model;

import com.forge.engine.tracker.PriceTracker;
import com.forge.engine.bidding.BidBook;

public class AuctionContext {
    private final PriceTracker priceTracker;
    private final AuctionStateMachine stateMachine;
    private final BidBook bidBook;

    public AuctionContext(PriceTracker priceTracker, AuctionStateMachine stateMachine, BidBook bidBook) {
        this.priceTracker = priceTracker;
        this.stateMachine = stateMachine;
        this.bidBook = bidBook;
    }

    // Getters for BiddingEngine
    public PriceTracker getPriceTracker() { return priceTracker; }
    public AuctionStateMachine getStateMachine() { return stateMachine; }
    public BidBook getBidBook() { return bidBook; }
}