package com.forge.engine.bidding;

import com.forge.engine.pricing.PricingStrategy;
import com.forge.engine.model.Bid;
import com.forge.engine.event.EventBus;
import java.util.concurrent.atomic.AtomicReference;

public class BiddingEngine {
    private final PricingStrategy pricingStrategy;
    private final BidBook bidBook;
    private final EventBus eventBus;
    private final AtomicReference<Bid> currentHighestBid;

    public BiddingEngine(PricingStrategy strategy, BidBook book, EventBus bus, Bid initialPrice) {
        this.pricingStrategy = strategy;
        this.bidBook = book;
        this.eventBus = bus;
        this.currentHighestBid = new AtomicReference<>(initialPrice);
    }

    public synchronized boolean placeBid(Bid newBid) {
        Bid current = currentHighestBid.get();


        if (pricingStrategy.isValidIncrement(current.getPrice(), newBid.getPrice())) {
            currentHighestBid.set(newBid);

            // bidBook logic
            bidBook.addBid(newBid);

            // eventBus logic
            eventBus.publish("AUCTION_UPDATE", newBid);

            return true;
        }
        return false;
    }

    public Bid getCurrentHighestBid() {
        return currentHighestBid.get();
    }
}