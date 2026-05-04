//package com.forge.platform.controller;
//
//import com.forge.platform.dto.BidRequest;
//import com.forge.platform.entity.Bid;
//import com.forge.platform.entity.User;
//import com.forge.platform.repository.UserRepository;
//import com.forge.platform.service.BiddingService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/bids")
//@RequiredArgsConstructor
//public class BidController {
//
//    private final BiddingService biddingService;
//    private final UserRepository userRepository;
//
//    @PostMapping
//    public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
//        try {
//            // 1. Fetch User (In production, get this from SecurityContext/JWT)
//            User bidder = userRepository.findByEmail(request.getUserEmail())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            // 2. Call the Escrow Service
//            Bid savedBid = biddingService.placeBid(request.getAuctionId(), bidder, request.getAmount());
//
//            return ResponseEntity.ok("Bid placed successfully! Escrow Locked: ₹" + savedBid.getAmount());
//
//        } catch (IllegalArgumentException | IllegalStateException e) {
//            // Business logic errors (e.g., Insufficient funds, Auction ended)
//            return ResponseEntity.badRequest().body(e.getMessage());
//        } catch (Exception e) {
//            // System errors
//            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
//        }
//    }
//}
package com.forge.platform.controller;

import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidRepository bidRepository;

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyBids(@AuthenticationPrincipal User user) {
        List<Bid> bids = bidRepository.findByBidderId(user.getId());

        List<Map<String, Object>> response = bids.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(bid -> Map.<String, Object>of(
                        "bidId",      bid.getId(),
                        "amount",     bid.getAmount(),
                        "auctionId",  bid.getAuction().getId(),
                        "auctionTitle", bid.getAuction().getTitle(),
                        "auctionStatus", bid.getAuction().getStatus().name(),
                        "imageUrl",   bid.getAuction().getImageUrl() != null ? bid.getAuction().getImageUrl() : "",
                        "successful", bid.isSuccessful(),
                        "placedAt",   bid.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}