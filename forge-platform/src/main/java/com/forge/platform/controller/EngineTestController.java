package com.forge.platform.controller;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class EngineTestController {

    private final BiddingEngine engine;

    public EngineTestController(BiddingEngine engine) {
        this.engine = engine;
    }

    /**
     * POST /api/bids/place?auctionId=101&userId=Sensei&amount=15000
     */
    @PostMapping("/place")
    public CompletableFuture<String> placeBid(
            @RequestParam Long auctionId,
            @RequestParam String userId,
            @RequestParam double amount) {

        Bid newBid = new Bid(userId, new Money(BigDecimal.valueOf(amount), "INR"));

        // Async call using Virtual Threads
        return engine.placeBid(auctionId, newBid)
                .thenApply(success -> success ? "ACCEPTED ✅" : "REJECTED ❌");
    }
}