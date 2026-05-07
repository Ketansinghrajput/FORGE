package com.forge.platform.controller;

import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class BiddingWebSocketController {

    private final AuctionService auctionService;

    @MessageMapping("/bid")
    public void processBidFromClient(@Payload com.forge.platform.dto.BidRequest request, Principal principal) {
        if (principal == null) {
            System.err.println("🔴 UNAUTHORIZED");
            return;
        }

        String email = principal.getName();

        try {
            // 🔥 SENSEI FIX: Email bhej rahe hain user object nahi, taaki repo call service me ho!
            auctionService.placeBid(request.getAuctionId(), email, request.getBidAmount());

        } catch (Exception e) {
            System.err.println("🔴 Bid Failed: " + e.getMessage());
        }
    }
}