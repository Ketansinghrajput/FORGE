package com.forge.platform.service;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.bidding.BidBook;
import com.forge.engine.model.*;
import com.forge.engine.tracker.PriceTracker;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class EngineInitializer {

    private final BiddingEngine engine;

    public EngineInitializer(BiddingEngine engine) {
        this.engine = engine;
    }

    @PostConstruct
    public void setupTestAuction() {
        // 1. Create a Test Auction (ID: 101)
        Long auctionId = 101L;

        // 2. Setup Base Price
        Money startPrice = new Money(new BigDecimal("10000.00"), "INR");
        Bid initialBid = new Bid("SYSTEM", startPrice);

        // 3. Create Context (Jhola)
        AuctionContext context = new AuctionContext(
                new PriceTracker(initialBid),
                new AuctionStateMachine(),
                new BidBook()
        );

        // 4. Register and make it ACTIVE
        engine.registerAuction(auctionId, context);
        context.getStateMachine().transitionTo(AuctionState.ACTIVE);

        System.out.println("✅ FORGE ENGINE: Test Auction 101 is now LIVE at ₹10,000");
    }
}