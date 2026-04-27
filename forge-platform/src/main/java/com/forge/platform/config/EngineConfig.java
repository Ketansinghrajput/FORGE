package com.forge.platform.config;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.event.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    /**
     * EventBus: Pure Java aur Spring ke beech ka bridge.
     */
    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    /**
     * BiddingEngine: Multi-tenant ready.
     * Sirf EventBus chahiye, baaki sab internal registerAuction se aayega.
     */
    @Bean
    public BiddingEngine biddingEngine(EventBus eventBus) {
        return new BiddingEngine(eventBus);
    }
}