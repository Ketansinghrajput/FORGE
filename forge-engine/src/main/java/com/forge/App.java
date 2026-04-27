package com.forge;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.bidding.BidBook;
import com.forge.engine.event.EventBus;
import com.forge.engine.model.*;
import com.forge.engine.tracker.PriceTracker;
import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        System.out.println("🚀 FORGE ENGINE TEST STARTING...\n");

        // 1. Setup Infrastructure
        EventBus eventBus = new EventBus();
        BiddingEngine engine = new BiddingEngine(eventBus);

        // 2. Setup a specific Auction (e.g., Rolex ID: 101)
        Long auctionId = 101L;
        Money startPrice = new Money(new BigDecimal("10000.00"));
        Bid initialBid = new Bid("SYSTEM", startPrice);

        // Context contains the logic for THIS specific auction
        AuctionContext context = new AuctionContext(
                new PriceTracker(initialBid),
                new AuctionStateMachine(),
                new BidBook()
        );

        // 3. Register and Start the Auction
        engine.registerAuction(auctionId, context);
        context.getStateMachine().transitionTo(AuctionState.ACTIVE);

        System.out.println("Auction 101 started at ₹10,000");

        // 4. Place a Bid (Async)
        Bid userBid = new Bid("SENSEI_KETAN", new Money(new BigDecimal("11000.00")));

        System.out.println("Placing bid of ₹11,000...");

        // join() is needed so the main thread waits for the virtual thread
        engine.placeBid(auctionId, userBid).thenAccept(success -> {
            System.out.println("Bid Accepted? " + (success ? "YES ✅" : "NO ❌"));
        }).join();

        // 5. Final Result
        Bid winner = context.getPriceTracker().getCurrentHighestBid();
        System.out.println("\nFinal Winner: " + winner.getBidderId());
        System.out.println("Final Price:  ₹" + winner.getPrice().getAmount());

        System.exit(0);
    }
}