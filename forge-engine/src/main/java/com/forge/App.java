package com.forge;

import com.forge.engine.bidding.BiddingEngine;
// Import paths tere actual project structure ke hisaab se check kar lena
import com.forge.engine.bidding.BidBook;
import com.forge.engine.event.EventBus;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import com.forge.engine.pricing.EnglishAuctionPricing;

import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        System.out.println("🚀 FORGE ENGINE TEST RUN STARTING...\n");

        // 1. System Setup (Manual Wiring)
        EventBus eventBus = new EventBus();
        BidBook bidBook = new BidBook();
        EnglishAuctionPricing pricingStrategy = new EnglishAuctionPricing();

        // 2. Base Price Setup: Vintage Rolex ₹10,000 par start ho raha hai
        Bid baseBid = new Bid("SYSTEM", new Money(new BigDecimal("10000"), "INR"));
        BiddingEngine engine = new BiddingEngine(pricingStrategy, bidBook, eventBus, baseBid);

        System.out.println("Base Price: ₹" + engine.getCurrentHighestBid().getPrice().amount());
        System.out.println("Required Minimum Increment: 5%");
        System.out.println("--------------------------------------------------");

        // Test Case 1: Invalid Bid (Amount too low)
        // 5% of 10000 is 500, so minimum acceptable is 10500. Let's send 10200.
        System.out.println("➡️  USER_A bids ₹10,200");
        Bid badBid = new Bid("USER_A", new Money(new BigDecimal("10200"), "INR"));
        boolean isBadAccepted = engine.placeBid(badBid);
        System.out.println("Result: " + (isBadAccepted ? "ACCEPTED ❌ (Bug!)" : "REJECTED ✅ (Works!)"));
        System.out.println("Current Winner: " + engine.getCurrentHighestBid().getBidderId());
        System.out.println("--------------------------------------------------");

        // Test Case 2: Valid Bid
        System.out.println("➡️  USER_B bids ₹11,000");
        Bid goodBid = new Bid("USER_B", new Money(new BigDecimal("11000"), "INR"));
        boolean isGoodAccepted = engine.placeBid(goodBid);
        System.out.println("Result: " + (isGoodAccepted ? "ACCEPTED ✅ (Works!)" : "REJECTED ❌ (Bug!)"));
        System.out.println("Current Winner: " + engine.getCurrentHighestBid().getBidderId());
        System.out.println("--------------------------------------------------");

        // Test Case 3: Another Invalid Bid (Someone tries to bid lower than current)
        System.out.println("➡️  USER_C bids ₹10,500 (Lower than current 11,000)");
        Bid lateBid = new Bid("USER_C", new Money(new BigDecimal("10500"), "INR"));
        boolean isLateAccepted = engine.placeBid(lateBid);
        System.out.println("Result: " + (isLateAccepted ? "ACCEPTED ❌ (Bug!)" : "REJECTED ✅ (Works!)"));
        System.out.println("Final Winner: " + engine.getCurrentHighestBid().getBidderId());
        System.out.println("Final Price: ₹" + engine.getCurrentHighestBid().getPrice().amount());
    }
}