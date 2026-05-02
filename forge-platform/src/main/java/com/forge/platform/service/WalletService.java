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

    @Transactional(readOnly = true)
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

    @Transactional
    public void lockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        BigDecimal availableBalance = wallet.getTotalBalance().subtract(wallet.getLockedAmount());

        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds to place this bid!");
        }

        wallet.setLockedAmount(wallet.getLockedAmount().add(amount));
        walletRepository.save(wallet);
        log.info("🔒 Locked ₹{} for user {}", amount, user.getEmail());
    }

    @Transactional
    public void unlockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        if (wallet.getLockedAmount().compareTo(amount) >= 0) {
            wallet.setLockedAmount(wallet.getLockedAmount().subtract(amount));
            walletRepository.save(wallet);
            log.info("🔓 Unlocked ₹{} for user {}", amount, user.getEmail());
        }
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    // Comprehensive Settlement Method for AuctionManager
    @Transactional
    public void settleAuction(User winner, User seller, BigDecimal amount) {
        log.info("💸 Starting Settlement: Winner({}) -> Seller({}) | Amount: ₹{}",
                winner.getEmail(), seller.getEmail(), amount);

        // 1. Deduct from Winner (Total and Locked)
        Wallet winnerWallet = walletRepository.findByUser(winner)
                .orElseThrow(() -> new RuntimeException("Winner wallet missing"));

        winnerWallet.setTotalBalance(winnerWallet.getTotalBalance().subtract(amount));
        winnerWallet.setLockedAmount(winnerWallet.getLockedAmount().subtract(amount));
        walletRepository.save(winnerWallet);

        // 2. Credit to Seller
        Wallet sellerWallet = walletRepository.findByUser(seller)
                .orElseThrow(() -> new RuntimeException("Seller wallet missing"));

        sellerWallet.setTotalBalance(sellerWallet.getTotalBalance().add(amount));
        walletRepository.save(sellerWallet);

        log.info("✅ Settlement successful for amount ₹{}", amount);
    }
    @Transactional
    public void settlePayment(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        // Total balance aur locked dono kam honge winner ke liye
        wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
        wallet.setLockedAmount(wallet.getLockedAmount().subtract(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void creditFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.save(wallet);
    }
    @Transactional
    public void topUpWallet(String email, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.save(wallet);
        log.info("💰 Top-up ₹{} for user {}", amount, email);
    }
}