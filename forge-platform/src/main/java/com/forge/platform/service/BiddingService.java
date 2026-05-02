package com.forge.platform.service;

import com.forge.platform.dto.AuctionUpdateDTO;
import com.forge.platform.entity.*;
import com.forge.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.forge.platform.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.forge.platform.entity.Bid_.amount;

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

        // 2. Business Validations
        validateBid(auction, bidder, bidderWallet, bidAmount);
        BigDecimal currentPrice = auction.getCurrentHighestBid() != null ?
                auction.getCurrentHighestBid() :
                auction.getStartingPrice();

        if (bidAmount.compareTo(currentPrice) <= 0) {
            throw new IllegalArgumentException("Bid current price (₹" + currentPrice + ") se zyada honi chahiye.");
        }

        // 3. Refund & Unlock Logic
        // Agar pehle se koi highest bidder hai aur wo ye khud nahi hai, toh purane wale ka paisa unlock karo
        // BiddingService.java ke andar refund logic update kar:
        if (auction.getHighestBidder() != null && !auction.getHighestBidder().getId().equals(bidder.getId())) {
            User previousBidder = auction.getHighestBidder();
            walletRepository.findByUser(previousBidder).ifPresent(prevWallet -> {
                log.info("Refunding previous bidder: {}", previousBidder.getEmail());

                // SENSEI FIX: Sirf current auction ka amount subtract kar, ZERO mat kar
                BigDecimal refundAmount = auction.getCurrentHighestBid();
                prevWallet.setLockedAmount(prevWallet.getLockedAmount().subtract(refundAmount));

                walletRepository.save(prevWallet);
            });
        }

        // 4. Lock Current Bidder's Money
        bidderWallet.setLockedAmount(bidAmount);
        walletRepository.save(bidderWallet);

        // 5. Smart Auction Extension (Anti-Snipe)
        // Agar auction khatam hone mein < 1 minute bacha hai, 5 min badha do
        if (auction.getEndTime().minusMinutes(1).isBefore(LocalDateTime.now())) {
            auction.setEndTime(auction.getEndTime().plusMinutes(5));
            log.info("Auction extended due to last minute bid by {}", bidder.getEmail());
        }

        // 6. Update Auction State
        auction.setCurrentHighestBid(bidAmount);
        auction.setHighestBidder(bidder);
        auctionRepository.save(auction);

        // 7. Save Bid Audit Record
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(bidAmount)
                .successful(true)
                .createdAt(LocalDateTime.now())
                .build();
        Bid savedBid = bidRepository.save(bid);

        // 8. Live Broadcast
        broadcastUpdate(auctionId, bidAmount, bidder, auction.getEndTime());
        return savedBid;
    }


    private void validateBid(Auction auction, User bidder, Wallet wallet, BigDecimal amount) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction active nahi hai, Sensei!");
        }
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Auction khatam ho chuki hai.");
        }
        if (amount.compareTo(auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartingPrice()) <= 0) {
            throw new IllegalArgumentException("Bid current price se zyada honi chahiye.");
        }
        // Use your getAvailableBalance() helper from Wallet entity
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Wallet mein itne paise nahi hain!");
        }
    }

    @Autowired
    private WalletService walletService;
    private void broadcastUpdate(Long auctionId, BigDecimal amount, User bidder, LocalDateTime newEndTime) {
        String destination = "/topic/auctions/" + auctionId;

        // DTO mein email ki jagah bidder.getFullName() bhejo UI ke liye
        AuctionUpdateDTO payload = AuctionUpdateDTO.builder()
                .auctionId(auctionId)
                .newPrice(amount)
                .bidder(bidder.getEmail())     // Backend tracking ke liye email rehne do
                .bidderName(bidder.getFullName()) // 👈 UI par "Ketan Singh" dikhane ke liye
                .availableFunds(walletService.getWalletByUserId(bidder.getId()).getAvailableBalance())                .timestamp(LocalDateTime.now().toString())
                .build();

        messagingTemplate.convertAndSend(destination, payload);
    }
}