package com.forge.platform.config;

import com.forge.engine.auction.AuctionType;
import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.event.EventBus;
import com.forge.engine.pricing.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class EngineConfig {

    @Bean(destroyMethod = "stop")
    public EventBus eventBus() {
        EventBus bus = new EventBus();
        bus.startDispatching();
        return bus;
    }
    @Bean
    public BiddingEngine biddingEngine(EventBus eventBus) {
        Map<AuctionType, PricingStrategy> strategies = Map.of(
                AuctionType.ENGLISH, new EnglishAuctionPricing(),
                AuctionType.DUTCH, new DutchAuctionPricing(),
                AuctionType.SEALED, new SealedBidPricing()
        );
        return new BiddingEngine(strategies, eventBus);
    }
}