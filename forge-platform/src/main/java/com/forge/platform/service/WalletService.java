package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.entity.WalletTransaction;
import com.forge.platform.enums.TransactionReason;
import com.forge.platform.enums.TransactionType;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import com.forge.platform.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final WalletTransactionRepository walletTransactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMyBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        return Map.of(
                "totalBalance", wallet.getTotalBalance(),
                "lockedAmount", wallet.getLockedAmount(),
                "availableBalance", wallet.getAvailableBalance()
        );
    }

    @Transactional
    public void topUpWallet(String email, BigDecimal amount) {
        log.info("💰 Processing top-up of ₹{} for {}", amount, email);
        User user = userRepository.findByEmail(email).orElseThrow();

        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.saveAndFlush(wallet);

        recordTransaction(wallet, TransactionType.CREDIT, TransactionReason.TOP_UP,
                amount, wallet.getAvailableBalance(), null, "Wallet top-up");

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

        recordTransaction(wallet, TransactionType.DEBIT, TransactionReason.BID_PLACED,
                amount, wallet.getAvailableBalance(), null, "Funds locked for bid");

        broadcastBalanceUpdate(user.getEmail(), wallet.getAvailableBalance());
    }

    @Transactional
    public void unlockFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserWithLock(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        BigDecimal newLockedAmount = wallet.getLockedAmount().subtract(amount);
        wallet.setLockedAmount(newLockedAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newLockedAmount);
        walletRepository.saveAndFlush(wallet);

        recordTransaction(wallet, TransactionType.CREDIT, TransactionReason.BID_REFUND,
                amount, wallet.getAvailableBalance(), null, "Bid refund — funds unlocked");

        broadcastBalanceUpdate(user.getEmail(), wallet.getAvailableBalance());
    }

    @Transactional
    public void settleAuction(User winner, User seller, BigDecimal amount) {
        log.info("💸 Settling Auction: Winner {} -> Seller {} | ₹{}", winner.getEmail(), seller.getEmail(), amount);

        // 1. Debit Winner
        Wallet winnerWallet = walletRepository.findByUserWithLock(winner).orElseThrow();

        if (winnerWallet.getTotalBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Critical Error: Winner balance dropped below auction price during settlement!");
        }

        winnerWallet.setTotalBalance(winnerWallet.getTotalBalance().subtract(amount));
        BigDecimal newLocked = winnerWallet.getLockedAmount().subtract(amount);
        winnerWallet.setLockedAmount(newLocked.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newLocked);
        walletRepository.saveAndFlush(winnerWallet);

        recordTransaction(winnerWallet, TransactionType.DEBIT, TransactionReason.PURCHASE,
                amount, winnerWallet.getAvailableBalance(), null, "Auction settlement — payment deducted");

        broadcastBalanceUpdate(winner.getEmail(), winnerWallet.getAvailableBalance());

        // 2. Credit Seller
        Wallet sellerWallet = walletRepository.findByUserWithLock(seller).orElseThrow();
        sellerWallet.setTotalBalance(sellerWallet.getTotalBalance().add(amount));
        walletRepository.saveAndFlush(sellerWallet);

        recordTransaction(sellerWallet, TransactionType.CREDIT, TransactionReason.PURCHASE,
                amount, sellerWallet.getAvailableBalance(), null, "Auction settlement — payment received");

        broadcastBalanceUpdate(seller.getEmail(), sellerWallet.getAvailableBalance());
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(String email, TransactionType type, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found!"));

        if (type != null) {
            return walletTransactionRepository.findByWalletIdAndTypeOrderByCreatedAtDesc(wallet.getId(), type, pageable);
        }
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private void recordTransaction(Wallet wallet, TransactionType type, TransactionReason reason,
                                   BigDecimal amount, BigDecimal balanceAfter,
                                   Long referenceId, String description) {
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .reason(reason)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .description(description)
                .build();
        walletTransactionRepository.save(tx);
    }

    private void broadcastBalanceUpdate(String email, BigDecimal newBalance) {
        Map<String, Object> payload = Map.of(
                "walletBalance", newBalance,
                "type", "BALANCE_UPDATE"
        );
        messagingTemplate.convertAndSendToUser(email, "/queue/wallet", payload);
    }
}