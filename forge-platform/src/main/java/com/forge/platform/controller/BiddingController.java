package com.forge.platform.controller;

import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.service.BiddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Slf4j // Logging ke liye
public class BiddingController {

    private final BiddingService biddingService;

    @PostMapping("/{auctionId}")
    public ResponseEntity<Bid> placeBid(
            @PathVariable Long auctionId,
            @RequestBody BidRequest request,
            @AuthenticationPrincipal User bidder // Asli User Context
    ) {
        if (bidder == null) {
            log.error("Unauthorized bid attempt on auction: {}", auctionId);
            return ResponseEntity.status(403).build();
        }

        log.info("Bid placed by User: {} for Amount: {}", bidder.getEmail(), request.amount());

        Bid savedBid = biddingService.placeBid(auctionId, bidder, request.amount());
        return ResponseEntity.ok(savedBid);
    }
}

// Keep the record here or move to a DTO package
record BidRequest(BigDecimal amount) {}