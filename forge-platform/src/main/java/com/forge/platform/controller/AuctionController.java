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

    /**
     * 🔥 SENSEI FIX: Replaced manual stream with clean pagination call.
     * Ab list crash nahi hogi aur performance mast rahegi.
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // AuctionService ka paginated method use kar rahe hain jo humne pehle likha tha
        return ResponseEntity.ok(auctionService.getActiveAuctionsPaginated(page, size));
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
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}