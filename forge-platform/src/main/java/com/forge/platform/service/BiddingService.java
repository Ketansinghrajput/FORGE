package com.forge.platform.service;

import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.BidRepository;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Bid placeBid(Long auctionId, User bidder, BigDecimal bidAmount) {
        log.info("Processing bid: {} by {} on auction {}", bidAmount, bidder.getEmail(), auctionId);

        // 1. Fetch Auction (Make sure your repo has findById or findByIdWithLock)
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 2. Fetch Bidder's Wallet
        Wallet bidderWallet = walletRepository.findByUser(bidder)
                .orElseThrow(() -> new RuntimeException("Wallet not found for bidder: " + bidder.getEmail()));

        // 3. Validations
        validateBid(auction, bidder, bidderWallet, bidAmount);

        // 4. Refund previous highest bidder (Lien Reversal)
        if (auction.getHighestBidder() != null) {
            User previousBidder = auction.getHighestBidder();
            walletRepository.findByUser(previousBidder).ifPresent(prevWallet -> {
                prevWallet.setLockedAmount(BigDecimal.ZERO);
                walletRepository.save(prevWallet);
                log.info("Escrow released for previous bidder: {}", previousBidder.getEmail());
            });
        }

        // 5. Lock funds for current bidder
        bidderWallet.setLockedAmount(bidAmount);
        walletRepository.save(bidderWallet);

        // 6. Update Auction State - MATCHING YOUR ENTITY FIELDS EXACTLY
        auction.setCurrentHighestBid(bidAmount); // Method name should match field 'currentHighestBid'
        auction.setHighestBidder(bidder);
        auctionRepository.save(auction);

        // 7. Persistence
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(bidAmount)
                .successful(true)
                .createdAt(LocalDateTime.now())
                .build();

        Bid savedBid = bidRepository.save(bid);

        // 8. WebSocket Notification
        broadcastUpdate(auctionId, bidAmount, bidder.getFullName());

        return savedBid;
    }

    private void validateBid(Auction auction, User bidder, Wallet wallet, BigDecimal amount) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction is not active");
        }

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Auction has already ended");
        }

        // 🚀 Check against currentHighestBid
        if (amount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Bid must be higher than current price: " + auction.getCurrentHighestBid());
        }

        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("Sellers cannot bid on their own items");
        }

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance.");
        }
    }

    private void broadcastUpdate(Long auctionId, BigDecimal amount, String bidderName) {
        try {
            String destination = "/topic/auctions/" + auctionId;
            Map<String, Object> payload = Map.of(
                    "auctionId", auctionId,
                    "newPrice", amount,
                    "bidder", bidderName,
                    "timestamp", LocalDateTime.now().toString()
            );
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket update", e);
        }
    }
}