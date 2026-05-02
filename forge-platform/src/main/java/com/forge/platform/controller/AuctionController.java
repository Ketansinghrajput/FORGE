package com.forge.platform.controller;

import com.forge.platform.dto.AuctionRequest;
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

    @GetMapping("/active")
    public ResponseEntity<?> getActiveAuctions() {
        List<Auction> activeAuctions = auctionService.getAllActiveAuctions();

        List<Map<String, Object>> safeResponse = activeAuctions.stream()
                .map(auction -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", auction.getId());
                    map.put("title", auction.getTitle());
                    map.put("currentHighestBid", auction.getCurrentHighestBid());
                    map.put("endTime", auction.getEndTime());
                    map.put("imageUrl", auction.getImageUrl());
                    map.put("description", auction.getDescription());
                    map.put("sellerEmail", auction.getSeller() != null ? auction.getSeller().getEmail() : null);
                    map.put("status", auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN");
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(safeResponse);
    }

    @PostMapping
    public ResponseEntity<?> createAuction(
            @RequestBody AuctionRequest request,
            @AuthenticationPrincipal User seller
    ) {
        if (seller == null) {
            return ResponseEntity.status(403).body("User not authenticated");
        }
        Auction created = auctionService.createAuction(request, seller);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", created.getId());
        response.put("title", created.getTitle());
        response.put("startingPrice", created.getStartingPrice());
        response.put("startTime", created.getStartTime());
        response.put("endTime", created.getEndTime());
        response.put("status", created.getStatus().name());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAuction(
            @PathVariable Long id,
            @AuthenticationPrincipal User seller
    ) {
        if (seller == null) {
            return ResponseEntity.status(403).body("Not authenticated");
        }
        try {
            auctionService.deleteAuction(id, seller);
            return ResponseEntity.ok("Auction deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}