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
    @Transactional(readOnly = true) // 🚀 SENSEI: Iske bina 500 error aayega (Lazy Loading issue)
    public ResponseEntity<Map<String, Object>> getInitialState(@PathVariable Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // User data nikalne se pehle null check zaroori hai
        String leader = (auction.getHighestBidder() != null)
                ? auction.getHighestBidder().getEmail()
                : "No bids yet";

        // Wallet fetch logic (Make sure user fixed hai ya Principal se aa raha hai)
        // Abhi ke liye tester@forge.com ka wallet fetch kar rahe hain
        Wallet wallet = walletRepository.findByUserEmail("tester@forge.com")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("currentBid", auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartingPrice());
        response.put("highestBidder", leader);
        response.put("availableFunds", wallet.getTotalBalance());
        response.put("endTime", auction.getEndTime().toString());

        return ResponseEntity.ok(response);
    }
}