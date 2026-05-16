package com.forge.platform.controller;

import com.forge.platform.dto.WalletTransactionDTO;
import com.forge.platform.entity.User;
import com.forge.platform.enums.TransactionType;
import com.forge.platform.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
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
                    "availableBalance", updatedBalance.get("availableBalance")
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Bank server issue? Check logs."));
        }
    }

    // GET /api/v1/wallet/transactions?page=0&size=10&type=CREDIT
    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionDTO>> getTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionDTO> transactions = walletService.getTransactionHistory(user.getEmail(), type, pageable);
        return ResponseEntity.ok(transactions);
    }
}