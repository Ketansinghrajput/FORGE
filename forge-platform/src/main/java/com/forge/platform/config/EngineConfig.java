package com.forge.platform.config;

import com.forge.engine.bidding.BidBook;
import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.event.EventBus;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import com.forge.engine.pricing.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class EngineConfig {

    @Bean
    public EventBus eventBus() {
        // Naya EventBus thread-safe aur reactive hai
        // startDispatching() ki zaroorat nahi hai
        return new EventBus();
    }

    @Bean
    public BiddingEngine biddingEngine(EventBus eventBus) {
        // 1. Default Strategy: English Auction (5% increment)
        PricingStrategy strategy = new EnglishAuctionPricing();

        // 2. High-performance ledger
        BidBook bidBook = new BidBook();

        // 3. Initial Base Price (Auction yahin se start hoga)
        Bid initialBid = new Bid("SYSTEM", new Money(BigDecimal.valueOf(12000), "INR"));

        // 4. Everything wired up
        return new BiddingEngine(strategy, bidBook, eventBus, initialBid);
    }
}