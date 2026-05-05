package com.forge.platform.controller;

import com.forge.platform.entity.User;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BiddingWebSocketController {

    private final AuctionService auctionService; //   Use AuctionService, not BiddingService
    private final UserRepository userRepository;

    @MessageMapping("/bid")
    public void processBidFromClient(@Payload com.forge.platform.dto.BidRequest request, Principal principal) {
        if (principal == null) {
            System.err.println("🔴 UNAUTHORIZED");
            return;
        }

        String email = principal.getName();
        User bidder = userRepository.findByEmail(email).orElseThrow();

        try {
            //   AuctionService.placeBid handles locking, refunds, broadcast — everything
            auctionService.placeBid(request.getAuctionId(), bidder, request.getBidAmount());

        } catch (Exception e) {
            System.err.println("🔴 Bid Failed: " + e.getMessage());
        }
    }
}