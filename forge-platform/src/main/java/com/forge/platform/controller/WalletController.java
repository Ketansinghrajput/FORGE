package com.forge.platform.controller;

import com.forge.platform.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // Yahan sirf Logged-In user aa sakta hai, token check hone ke baad
    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(Principal principal) {
        // DRY RUN:
        // 1. Postman se Request aayi with Bearer Token.
        // 2. JwtAuthenticationFilter ne token verify kiya aur usme se 'email' nikala.
        // 3. Filter ne wo email SecurityContext mein daal diya.
        // 4. 'Principal' usi context se aaya hai. principal.getName() = tera email!

        return ResponseEntity.ok(walletService.getMyBalance(principal.getName()));
    }
}