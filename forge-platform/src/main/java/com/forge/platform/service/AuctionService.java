package com.forge.platform.service;

import com.forge.platform.dto.AuctionRequest;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final WalletService walletService;

    @Transactional
    public Auction createAuction(AuctionRequest request, User seller) {
        log.info("Creating new auction: {} by seller: {}", request.title(), seller.getEmail());

        if (request.startTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new IllegalArgumentException("Auction cannot start in the past");
        }
        if (request.endTime().isBefore(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Auction auction = Auction.builder()
                .title(request.title())
                .description(request.description())
                .startingPrice(request.startingPrice())
                .currentHighestBid(request.startingPrice())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .build();

        return auctionRepository.save(auction);
    }

    @Transactional
    public void placeBid(Long auctionId, User newBidder, BigDecimal bidAmount) {
        // Auction fetch with lock for concurrency
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 1. Validations
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction is not active");
        }
        if (bidAmount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Bid must be higher than: " + auction.getCurrentHighestBid());
        }
        if (newBidder.getId().equals(auction.getSeller().getId())) {
            throw new IllegalArgumentException("Seller cannot bid on their own auction");
        }

        // 2. LOCK NEW BIDDER'S FUNDS FIRST
        // Agar yahan balance kam hua, toh exception yahi se throw ho jayega
        log.info("Locking funds for new bidder {}: ₹{}", newBidder.getEmail(), bidAmount);
        walletService.lockFunds(newBidder, bidAmount);

        // 3. REFUND PREVIOUS BIDDER (Only after new bid is secured)
        if (auction.getHighestBidder() != null) {
            User previousBidder = auction.getHighestBidder();
            log.info("Outbidding {} | Refunding: ₹{}", previousBidder.getEmail(), auction.getCurrentHighestBid());
            walletService.unlockFunds(previousBidder, auction.getCurrentHighestBid());
        }

        // 4. UPDATE AUCTION STATE
        auction.setHighestBidder(newBidder);
        auction.setCurrentHighestBid(bidAmount);
        auctionRepository.save(auction);

        log.info("Bid successfully placed by {}", newBidder.getEmail());
    }

    @Transactional
    public void processExpiredAuctions() {
        log.info("System checking for expired auctions...");

        List<Auction> expiredAuctions = auctionRepository.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .filter(a -> a.getEndTime().isBefore(LocalDateTime.now()))
                .toList();

        for (Auction auction : expiredAuctions) {
            if (auction.getHighestBidder() != null) {
                User winner = auction.getHighestBidder();
                User seller = auction.getSeller();
                BigDecimal finalAmount = auction.getCurrentHighestBid();

                log.info("SETTLEMENT START: Winner {} paying ₹{} to Seller {}", winner.getEmail(), finalAmount, seller.getEmail());

                walletService.settlePayment(winner, finalAmount);
                walletService.creditFunds(seller, finalAmount);

                auction.setStatus(AuctionStatus.COMPLETED);
            } else {
                log.info("Auction ID {} EXPIRED with no bids.", auction.getId());
                auction.setStatus(AuctionStatus.EXPIRED);
            }
            auctionRepository.save(auction);
        }
    }

    public List<Auction> getAllActiveAuctions() {
        return auctionRepository.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .toList();
    }
}