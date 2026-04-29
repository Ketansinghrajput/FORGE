package com.forge.platform.controller;

import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.service.BiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate; // SENSEI FIX: Ye missing tha!
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BiddingWebSocketController {

    private final BiddingService biddingService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // SENSEI FIX: Isse hum broadcast karenge

    @MessageMapping("/bid")
    public void processBidFromClient(@Payload com.forge.platform.dto.BidRequest request, Principal principal) {
        if (principal == null) {
            System.err.println("🔴 UNAUTHORIZED");
            return;
        }

        String email = principal.getName();
        User bidder = userRepository.findByEmail(email).orElseThrow();

        try {
            // 1. Asli engine logic call karo (Save to DB)
            Bid savedBid = biddingService.placeBid(request.getAuctionId(), bidder, request.getBidAmount());

            // 2. SENSEI FIX: Frontend ko wapas JSON bhej!
            // Angular is 'newPrice' aur 'bidder' ka wait kar raha hai
            Map<String, Object> broadcastData = Map.of(
                    "newPrice", savedBid.getAmount(),
                    "bidder", bidder.getEmail()
            );

            messagingTemplate.convertAndSend("/topic/auctions/" + request.getAuctionId(), broadcastData);
            System.out.println("✅ Broadcast Fired to Frontend: " + broadcastData);

        } catch (Exception e) {
            System.err.println("🔴 Bid Failed in DB: " + e.getMessage());
            // Optionally, send an error back to the specific user here
        }
    }
}