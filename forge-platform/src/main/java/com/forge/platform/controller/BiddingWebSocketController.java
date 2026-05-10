package com.forge.platform.controller;

import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BiddingWebSocketController {

    private final AuctionService auctionService;
    private final SimpMessagingTemplate messagingTemplate; // 🔥 inject karo

    @MessageMapping("/bid")
    public void processBidFromClient(
            @Payload com.forge.platform.dto.BidRequest request,
            Principal principal) {

        if (principal == null) {
            System.err.println("🔴 UNAUTHORIZED");
            return;
        }

        String email = principal.getName();

        try {
            auctionService.placeBid(request.getAuctionId(), email, request.getBidAmount());

        } catch (Exception e) {
            System.err.println("🔴 Bid Failed: " + e.getMessage());

            // 🔥 Error user ko personally bhejo — frontend /user/queue/errors sun raha hai
            messagingTemplate.convertAndSendToUser(
                    email,
                    "/queue/errors",
                    Map.of(
                            "type", "BID_ERROR",
                            "message", e.getMessage()
                    )
            );
        }
    }
}