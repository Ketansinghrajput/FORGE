package com.forge.platform.config;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.event.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {


    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }


    @Bean
    public BiddingEngine biddingEngine(EventBus eventBus) {
        return new BiddingEngine(eventBus);
    }
}