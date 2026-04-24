package com.forge.platform.service;

import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.BidRepository;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Bid placeBid(Long auctionId, User bidder, BigDecimal bidAmount) {
        log.info("Attempting to place bid of {} on auction {} by user {}", bidAmount, auctionId, bidder.getEmail());

        // 1. Lock the auction row (Race condition prevention)
        // findByIdWithLock humne AuctionRepository mein likha hai
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 2. Run Industrial Validation
        validateBid(auction, bidder, bidAmount);

        // 3. Update Auction State
        auction.setCurrentHighestBid(bidAmount);
        auctionRepository.save(auction);

        // 4. Record the Bid
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(bidAmount)
                .successful(true)
                .build();

        Bid savedBid = bidRepository.save(bid);

        // 5. Real-time Broadcast via WebSocket
        // Ab frontend ko bina refresh kiye update milega
        broadcastUpdate(auctionId, bidAmount, bidder.getFullName());

        return savedBid;
    }

    private void validateBid(Auction auction, User bidder, BigDecimal amount) {
        // Rule: Auction must be ACTIVE
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction is not active or has already ended");
        }

        // Rule: Time check
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Auction time expired");
        }

        // Rule: Bid increments
        if (amount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Bid must be strictly higher than current price: " + auction.getCurrentHighestBid());
        }

        // Rule: Self-bidding check (Fraud Prevention)
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("You cannot bid on your own auction");
        }

        // Rule: Wallet sufficiency
        if (bidder.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance for this bid");
        }
    }

    private void broadcastUpdate(Long auctionId, BigDecimal amount, String bidderName) {
        String destination = "/topic/auctions/" + auctionId;
        Map<String, Object> payload = Map.of(
                "auctionId", auctionId,
                "newPrice", amount,
                "bidder", bidderName,
                "timestamp", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(destination, payload);
    }
}