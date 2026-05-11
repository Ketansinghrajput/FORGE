package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMyBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        return Map.of("balance", wallet.getAvailableBalance());
    }

    @Transactional
    public void topUpWallet(String email, BigDecimal amount) {
        log.info("💰 Processing top-up of ₹{} for {}", amount, email);
        User user = userRepository.findByEmail(email).orElseThrow();

        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.saveAndFlush(wallet);

        broadcastBalanceUpdate(email, wallet.getAvailableBalance());
    }

    @Transactional
    public void lockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));


        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds! Required: ₹" + amount + ", Available: ₹" + wallet.getAvailableBalance());
        }

        log.info("🔒 Locking funds: ₹{} for {}", amount, user.getEmail());
        wallet.setLockedAmount(wallet.getLockedAmount().add(amount));
        walletRepository.saveAndFlush(wallet);

        broadcastBalanceUpdate(user.getEmail(), wallet.getAvailableBalance());
    }

    @Transactional
    public void unlockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        BigDecimal newLockedAmount = wallet.getLockedAmount().subtract(amount);
        wallet.setLockedAmount(newLockedAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newLockedAmount);

        walletRepository.saveAndFlush(wallet);
        broadcastBalanceUpdate(user.getEmail(), wallet.getAvailableBalance());
    }

    @Transactional
    public void settleAuction(User winner, User seller, BigDecimal amount) {
        log.info("💸 Settling Auction: Winner {} -> Seller {} | ₹{}", winner.getEmail(), seller.getEmail(), amount);

        // 1. Debit Winner
        Wallet winnerWallet = walletRepository.findByUserWithLock(winner).orElseThrow();

        // Final balance check before settlement
        if (winnerWallet.getTotalBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Critical Error: Winner balance dropped below auction price during settlement!");
        }

        winnerWallet.setTotalBalance(winnerWallet.getTotalBalance().subtract(amount));

        BigDecimal newLocked = winnerWallet.getLockedAmount().subtract(amount);
        winnerWallet.setLockedAmount(newLocked.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newLocked);

        walletRepository.saveAndFlush(winnerWallet);
        broadcastBalanceUpdate(winner.getEmail(), winnerWallet.getAvailableBalance());

        // 2. Credit Seller
        Wallet sellerWallet = walletRepository.findByUserWithLock(seller).orElseThrow();
        sellerWallet.setTotalBalance(sellerWallet.getTotalBalance().add(amount));

        walletRepository.saveAndFlush(sellerWallet);
        broadcastBalanceUpdate(seller.getEmail(), sellerWallet.getAvailableBalance());
    }

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