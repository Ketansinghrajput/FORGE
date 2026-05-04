package com.forge.platform.controller;
import org.springframework.transaction.annotation.Transactional; // 🚀 SENSEI FIX: Ensure import is correct
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class EngineStateController {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @GetMapping("/auction-state/{auctionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getInitialState(
            @PathVariable Long auctionId,
            Principal principal
    ) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        String leader = (auction.getHighestBidder() != null)
                ? auction.getHighestBidder().getFullName()
                : "Waiting for Bids...";

        Wallet wallet = walletRepository.findByUserEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // ✅ Derive effective status — don't trust DB blindly if time has passed
        String effectiveStatus = auction.getStatus().name();
        if (effectiveStatus.equals("ACTIVE") && auction.getEndTime().isBefore(java.time.LocalDateTime.now())) {
            effectiveStatus = "COMPLETED"; // scheduler hasn't caught up yet, but time is up
        }

        Map<String, Object> response = new HashMap<>();
        response.put("currentBid", auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartingPrice());
        response.put("highestBidder", leader);
        response.put("availableFunds", wallet.getTotalBalance());
        // ✅ Correct fix
        response.put("endTime", auction.getEndTime()
                .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        response.put("title", auction.getTitle());
        response.put("description", auction.getDescription());
        response.put("imageUrl", auction.getImageUrl());
        response.put("status", effectiveStatus); // ✅ now always accurate

        return ResponseEntity.ok(response);
    }
}