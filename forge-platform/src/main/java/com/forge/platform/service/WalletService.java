package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate; // 🔥 For instant UI update
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
    private final SimpMessagingTemplate messagingTemplate; // 🔥 Sensei Fix

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMyBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        // Frontend expects 'walletBalance' or 'balance' key based on your DTO
        return Map.of("balance", wallet.getAvailableBalance());
    }

    @Transactional
    public void topUpWallet(String email, BigDecimal amount) {
        log.info("💰 Processing top-up of ₹{} for {}", amount, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        // 🔥 SENSEI FIX: Use Pessimistic Lock taaki balance update safe rahe
        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.save(wallet);

        // 🔥 SENSEI MAGIC: Push update to WebSocket
        // Isse Navbar ka BehaviorSubject instantly update ho jayega!
        broadcastBalanceUpdate(email, wallet.getAvailableBalance());

        log.info("✅ Top-up successful. New Balance: ₹{}", wallet.getAvailableBalance());
    }

    @Transactional
    public void lockFunds(User user, BigDecimal amount) {
        // Lock with Write access to prevent concurrent bidding issues
        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds to place this bid!");
        }

        wallet.setLockedAmount(wallet.getLockedAmount().add(amount));
        walletRepository.save(wallet);

        broadcastBalanceUpdate(user.getEmail(), wallet.getAvailableBalance());
        log.info("🔒 Locked ₹{} for user {}", amount, user.getEmail());
    }

    @Transactional
    public void unlockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        BigDecimal newLockedAmount = wallet.getLockedAmount().subtract(amount);
        if (newLockedAmount.compareTo(BigDecimal.ZERO) < 0) {
            newLockedAmount = BigDecimal.ZERO; // Safety check
        }

        wallet.setLockedAmount(newLockedAmount);
        walletRepository.save(wallet);

        broadcastBalanceUpdate(user.getEmail(), wallet.getAvailableBalance());
        log.info("🔓 Unlocked ₹{} for user {}", amount, user.getEmail());
    }

    @Transactional
    public void settleAuction(User winner, User seller, BigDecimal amount) {
        log.info("💸 Settling Auction: {} -> {} | ₹{}", winner.getEmail(), seller.getEmail(), amount);

        // 1. Deduct from Winner — clear both total and locked
        Wallet winnerWallet = walletRepository.findByUserWithLock(winner).orElseThrow();

        // ✅ Subtract from total balance
        winnerWallet.setTotalBalance(winnerWallet.getTotalBalance().subtract(amount));

        // ✅ Clear locked amount safely — don't assume exact locked value
        BigDecimal newLocked = winnerWallet.getLockedAmount().subtract(amount);
        winnerWallet.setLockedAmount(newLocked.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newLocked);

        walletRepository.save(winnerWallet);
        broadcastBalanceUpdate(winner.getEmail(), winnerWallet.getAvailableBalance());
        log.info("✅ Winner {} debited ₹{} | New balance: ₹{}", winner.getEmail(), amount, winnerWallet.getAvailableBalance());

        // 2. Credit to Seller
        Wallet sellerWallet = walletRepository.findByUserWithLock(seller).orElseThrow();
        sellerWallet.setTotalBalance(sellerWallet.getTotalBalance().add(amount));
        walletRepository.save(sellerWallet);
        broadcastBalanceUpdate(seller.getEmail(), sellerWallet.getAvailableBalance());
        log.info("✅ Seller {} credited ₹{} | New balance: ₹{}", seller.getEmail(), amount, sellerWallet.getAvailableBalance());
    }

    // Common method to notify Frontend via WebSocket
    private void broadcastBalanceUpdate(String email, BigDecimal newBalance) {
        Map<String, Object> payload = Map.of(
                "walletBalance", newBalance,
                "type", "BALANCE_UPDATE"
        );
        messagingTemplate.convertAndSendToUser(email, "/queue/wallet", payload);
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }
}