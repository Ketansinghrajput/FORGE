package com.forge.platform.controller;

import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.service.BiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BiddingController {

    private final BiddingService biddingService;

    @PostMapping("/{auctionId}")
    public ResponseEntity<Bid> placeBid(
            @PathVariable Long auctionId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal User bidder
    ) {
        return ResponseEntity.ok(biddingService.placeBid(auctionId, bidder, amount));
    }
}