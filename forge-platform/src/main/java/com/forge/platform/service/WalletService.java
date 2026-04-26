package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    // 1. Balance Check
    public Map<String, BigDecimal> getMyBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user!"));
        return Map.of(
                "totalBalance", wallet.getTotalBalance(),
                "lockedAmount", wallet.getLockedAmount()
        );
    }

    // 2. Bid lagate waqt paise Escrow mein lock karna
    @Transactional
    public void lockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        BigDecimal availableBalance = wallet.getTotalBalance().subtract(wallet.getLockedAmount());

        if (availableBalance.compareTo(amount) < 0) {
            log.error("Insufficient funds for user {}. Required: {}, Available: {}", user.getEmail(), amount, availableBalance);
            throw new IllegalStateException("Insufficient funds to place this bid!");
        }

        wallet.setLockedAmount(wallet.getLockedAmount().add(amount));
        walletRepository.save(wallet);
        log.info("🔒 Locked ₹{} in Escrow for user {}", amount, user.getEmail());
    }

    // 3. FIX: Outbid hone par purane bidder ke paise unlock (refund) karna
    @Transactional
    public void unlockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        if (wallet.getLockedAmount().compareTo(amount) < 0) {
            log.error("Fatal Error: Trying to unlock more funds than locked for user {}", user.getEmail());
            throw new IllegalStateException("Invalid Escrow unlock operation!");
        }

        wallet.setLockedAmount(wallet.getLockedAmount().subtract(amount));
        walletRepository.save(wallet);
        log.info("🔓 Unlocked ₹{} for previous bidder {}", amount, user.getEmail());
    }

    // 4. Auction jeetne ke baad Winner ke paise kaatna
    @Transactional
    public void settlePayment(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        if (wallet.getLockedAmount().compareTo(amount) < 0) {
            log.error("Fatal Error: Winner {} doesn't have enough locked funds!", user.getEmail());
            throw new IllegalStateException("Settlement failed!");
        }

        wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
        wallet.setLockedAmount(wallet.getLockedAmount().subtract(amount));

        walletRepository.save(wallet);
        log.info("💸 SETTLEMENT: Deducted ₹{} from Winner {}", amount, user.getEmail());
    }

    // 5. Seller ke account mein paise credit karna
    @Transactional
    public void creditFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.save(wallet);
        log.info("💰 CREDIT: Added ₹{} to Seller {}", amount, user.getEmail());
    }
}