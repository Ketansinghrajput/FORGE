package com.forge.platform.controller;

import com.forge.platform.dto.BidRequest;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.service.BiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final BiddingService biddingService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
        try {
            // 1. Fetch User (In production, get this from SecurityContext/JWT)
            User bidder = userRepository.findByEmail(request.getUserEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Call the Escrow Service
            Bid savedBid = biddingService.placeBid(request.getAuctionId(), bidder, request.getAmount());

            return ResponseEntity.ok("Bid placed successfully! Escrow Locked: ₹" + savedBid.getAmount());

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Business logic errors (e.g., Insufficient funds, Auction ended)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // System errors
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}