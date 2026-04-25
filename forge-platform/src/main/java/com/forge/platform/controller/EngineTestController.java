package com.forge.platform.controller;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/bids") // Postman wale URL se exact match
@CrossOrigin(origins = "*")
public class EngineTestController {

    private final BiddingEngine engine;

    // Direct Engine inject kar rahe hain test ke liye
    public EngineTestController(BiddingEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/place")
    public String placeBid(@RequestParam String userId, @RequestParam double amount) {
        // Core engine wala Bid object banao
        Bid newBid = new Bid(userId, new Money(BigDecimal.valueOf(amount), "INR"));

        // Engine ko do aur real-time websocket magic dekho
        boolean success = engine.placeBid(newBid);

        return success ? "ACCEPTED" : "REJECTED";
    }
}