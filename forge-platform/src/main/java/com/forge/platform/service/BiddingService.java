package com.forge.platform.service;

import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.BidRepository;
import com.forge.platform.repository.UserRepository;
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
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public Bid placeBid(Long auctionId, User bidder, BigDecimal bidAmount) {
        log.info("Processing bid: {} by {} on auction {}", bidAmount, bidder.getEmail(), auctionId);

        // 1. Pessimistic Locking: Row lock taaki koi aur simultaneous bid process na ho
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 2. Industrial Validations (Fraud, Time, and Balance checks)
        validateBid(auction, bidder, bidAmount);

        // 3. REFUND LOGIC: Purane highest bidder ko uske paise wapas karo
        if (auction.getHighestBidder() != null) {
            User previousBidder = auction.getHighestBidder();
            BigDecimal refundAmount = auction.getCurrentHighestBid();

            previousBidder.setWalletBalance(previousBidder.getWalletBalance().add(refundAmount));
            userRepository.save(previousBidder);
            log.info("Refunded {} to previous bidder {}", refundAmount, previousBidder.getEmail());
        }

        // 4. DEBIT LOGIC: Naye bidder ke account se bid amount kato
        bidder.setWalletBalance(bidder.getWalletBalance().subtract(bidAmount));
        userRepository.save(bidder);
        log.info("Debited {} from current bidder {}", bidAmount, bidder.getEmail());

        // 5. UPDATE AUCTION STATE: Naya price aur naya highest bidder set karo
        auction.setCurrentHighestBid(bidAmount);
        auction.setHighestBidder(bidder);
        auctionRepository.save(auction);

        // 6. RECORD THE BID: Persistence for Audit Trail
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(bidAmount)
                .successful(true)
                .createdAt(LocalDateTime.now())
                .build();

        Bid savedBid = bidRepository.save(bid);

        // 7. REAL-TIME BROADCAST: WebSocket notification for Frontend
        broadcastUpdate(auctionId, bidAmount, bidder.getFullName());

        return savedBid;
    }

    private void validateBid(Auction auction, User bidder, BigDecimal amount) {
        // Auction Status Check
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction is not active or has already ended");
        }

        // Time Validation
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Auction time expired");
        }

        // Price Increment Check
        if (amount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Bid must be strictly higher than current price: " + auction.getCurrentHighestBid());
        }

        // Self-bidding Prevention
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("You cannot bid on your own auction");
        }

        // Real-time Wallet Sufficiency Check
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