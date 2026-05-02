package com.forge.platform.controller;

import com.forge.platform.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(Principal principal) {

        return ResponseEntity.ok(walletService.getMyBalance(principal.getName()));
    }
    @PostMapping("/topup")
    public ResponseEntity<?> topUp(
            @RequestBody Map<String, BigDecimal> body,
            Principal principal
    ) {
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }
        walletService.topUpWallet(principal.getName(), amount);
        return ResponseEntity.ok(Map.of("message", "Wallet topped up successfully", "amount", amount));
    }
}