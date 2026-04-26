package com.forge.platform.controller;

import com.forge.platform.dto.BidRequest;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping("/{id}/bid")
    public ResponseEntity<String> placeBid(
            @PathVariable Long id,
            @RequestBody BidRequest request,
            @AuthenticationPrincipal User bidder
    ) {
        if (bidder == null) {
            return ResponseEntity.status(403).body("User not authenticated");
        }

        auctionService.placeBid(id, bidder, request.getBidAmount());
        return ResponseEntity.ok("Bid placed successfully by " + bidder.getFullName());
    }

    // 🚀 THE NUCLEAR FIX: Jackson ko proxy chune ka mauka hi mat do
    // 🚀 THE NUCLEAR FIX (Updated for Java Compiler compatibility)
    @GetMapping("/active")
    public ResponseEntity<?> getActiveAuctions() {
        List<Auction> activeAuctions = auctionService.getAllActiveAuctions();

        // Convert List of Entities to List of Maps using standard HashMap
        List<Map<String, Object>> safeResponse = activeAuctions.stream()
                .map(auction -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", auction.getId());
                    map.put("title", auction.getTitle());
                    map.put("currentHighestBid", auction.getCurrentHighestBid());
                    // Null check just to be extra safe
                    map.put("status", auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN");
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(safeResponse);
    }
}