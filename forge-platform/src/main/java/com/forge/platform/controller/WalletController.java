package com.forge.platform.controller;

import com.forge.platform.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.forge.platform.entity.User;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;


    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@AuthenticationPrincipal User user) {

        return ResponseEntity.ok(walletService.getMyBalance(user.getEmail()));
    }


    @PostMapping("/topup")
    public ResponseEntity<?> topUp(
            @RequestBody Map<String, BigDecimal> body,
            @AuthenticationPrincipal User user
    ) {
        BigDecimal amount = body.get("amount");

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount zero se bada hona chahiye Sensei!"));
        }

        try {
            walletService.topUpWallet(user.getEmail(), amount);

            Map<String, BigDecimal> updatedBalance = walletService.getMyBalance(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Paisa jama ho gaya!",
                    "addedAmount", amount,
                    "balance", updatedBalance.get("balance")
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Bank server issue? Check logs."));
        }
    }
}