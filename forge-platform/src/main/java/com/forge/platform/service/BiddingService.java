package com.forge.platform.service;

import com.forge.platform.dto.AuctionUpdateDTO;
import com.forge.platform.entity.*;
import com.forge.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.forge.platform.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class BiddingService {

    private final AuctionRepository auctionRepository;
    private final WalletRepository walletRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Bid placeBid(Long auctionId, User bidder, BigDecimal bidAmount) {
        log.info("Processing bid: {} by {} on auction {}", bidAmount, bidder.getEmail(), auctionId);

        // 1. Pessimistic Lock: Row lock taaki concurrency issues na aayein
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        Wallet bidderWallet = walletRepository.findByUser(bidder)
                .orElseThrow(() -> new RuntimeException("Wallet not found for: " + bidder.getEmail()));

        // 2. Business Validations (Lock lene ke baad hi validate karna hai)
        validateBid(auction, bidder, bidderWallet, bidAmount);

        // 3. Smart Wallet Locking & Refund Logic (The Phantom Deduction Fix)
        User previousBidder = auction.getHighestBidder();
        BigDecimal previousBidAmount = auction.getCurrentHighestBid();

        if (previousBidder != null) {
            if (!previousBidder.getId().equals(bidder.getId())) {
                // Case A: Naya user aaya hai. Purane user ke paise unlock karo.
                walletRepository.findByUser(previousBidder).ifPresent(prevWallet -> {
                    BigDecimal newLockedAmount = prevWallet.getLockedAmount().subtract(previousBidAmount);
                    prevWallet.setLockedAmount(newLockedAmount.max(BigDecimal.ZERO)); // Prevent negative
                    walletRepository.save(prevWallet);
                    log.info("Unlocked ₹{} for previous bidder: {}", previousBidAmount, previousBidder.getEmail());
                });

                // Aur naye user (current bidder) ka pura amount lock karo
                BigDecimal currentLocked = bidderWallet.getLockedAmount() != null ? bidderWallet.getLockedAmount() : BigDecimal.ZERO;
                bidderWallet.setLockedAmount(currentLocked.add(bidAmount));

            } else {
                // Case B: Same user ne apni hi bid badha di. Sirf difference amount aur lock karo.
                BigDecimal difference = bidAmount.subtract(previousBidAmount);
                BigDecimal currentLocked = bidderWallet.getLockedAmount() != null ? bidderWallet.getLockedAmount() : BigDecimal.ZERO;
                bidderWallet.setLockedAmount(currentLocked.add(difference));
                log.info("Same user increased bid. Locked additional ₹{}", difference);
            }
        } else {
            // Case C: First bid ever on this auction. Pura amount lock karo.
            BigDecimal currentLocked = bidderWallet.getLockedAmount() != null ? bidderWallet.getLockedAmount() : BigDecimal.ZERO;
            bidderWallet.setLockedAmount(currentLocked.add(bidAmount));
        }

        // Wallet DB mein save karo
        walletRepository.save(bidderWallet);

        // 4. Smart Auction Extension (Anti-Snipe) - Isko active kar diya hai, premium feature hai!
        if (auction.getEndTime().minusMinutes(1).isBefore(LocalDateTime.now())) {
            auction.setEndTime(auction.getEndTime().plusMinutes(5));
            log.info("Auction extended due to last minute bid by {}", bidder.getEmail());
        }

        // 5. Update Auction State
        auction.setCurrentHighestBid(bidAmount);
        auction.setHighestBidder(bidder);
        auctionRepository.save(auction);

        // 6. Save Bid Audit Record
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(bidAmount)
                .successful(true)
                .build();
        Bid savedBid = bidRepository.save(bid);

        // 7. Live Broadcast (DB Hit removed, memory se direct bhej rahe hain)
        broadcastUpdate(auctionId, bidAmount, bidder, auction.getEndTime(), bidderWallet.getAvailableBalance());

        return savedBid;
    }

    private void validateBid(Auction auction, User bidder, Wallet wallet, BigDecimal amount) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction active nahi hai, Sensei!");
        }
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Auction khatam ho chuki hai.");
        }

        BigDecimal currentPrice = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartingPrice();
        if (amount.compareTo(currentPrice) <= 0) {
            throw new IllegalArgumentException("Bid current price (₹" + currentPrice + ") se zyada honi chahiye.");
        }

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Wallet mein itne paise nahi hain!");
        }
    }

    // N+1 Query fix: availableFunds ab parameter mein aayega, faltu ka walletService call hata diya.
    private void broadcastUpdate(Long auctionId, BigDecimal amount, User bidder, LocalDateTime newEndTime, BigDecimal availableFunds) {
        String destination = "/topic/auctions/" + auctionId;

        AuctionUpdateDTO payload = AuctionUpdateDTO.builder()
                .auctionId(auctionId)
                .newPrice(amount)
                .bidder(bidder.getEmail())
                .bidderName(bidder.getFullName())
                .availableFunds(availableFunds) // Directly mapped!
                .endTime(newEndTime != null ? newEndTime
                        .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null)
                .timestamp(LocalDateTime.now().toString())
                .build();

        messagingTemplate.convertAndSend(destination, payload);
        log.info("✅ Broadcast Fired to Frontend: {}", payload);
    }
}