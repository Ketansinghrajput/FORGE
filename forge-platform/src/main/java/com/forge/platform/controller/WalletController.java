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
@RequestMapping("/api/v1/wallets") // 🔥 SENSEI FIX: Path ko plural 'wallets' kiya hai taaki frontend se match ho
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Fresh wallet balance fetch karne ke liye (Source of Truth)
     * Frontend call: /api/v1/wallets/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@AuthenticationPrincipal User user) {
        // Log for debugging
        // log.info("SENSEI: Fetching fresh balance for user: {}", user.getEmail());

        // Ensure service method returns a Map with key "balance"
        return ResponseEntity.ok(walletService.getMyBalance(user.getEmail()));
    }

    /**
     * Paisa add karne ke liye
     * Frontend call: /api/v1/wallets/topup
     */
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

            // Fresh balance fetch karke hi return karo taaki UI turant update ho
            Map<String, BigDecimal> updatedBalance = walletService.getMyBalance(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Paisa jama ho gaya!",
                    "addedAmount", amount,
                    "balance", updatedBalance.get("balance") // 🔥 Frontend expects 'balance' key
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Bank server issue? Check logs."));
        }
    }
}