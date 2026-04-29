package com.forge.platform.service;

import com.forge.platform.dto.AuctionUpdateDTO;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.BidRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletRepository walletRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Bid placeBid(Long auctionId, User bidder, BigDecimal bidAmount) {
        log.info("Processing bid: {} by {} on auction {}", bidAmount, bidder.getEmail(), auctionId);

        // 1. Pessimistic Lock: Jab tak ye transaction khatam nahi hota, koi aur is auction ko touch nahi kar sakta
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        Wallet bidderWallet = walletRepository.findByUser(bidder)
                .orElseThrow(() -> new RuntimeException("Wallet not found for: " + bidder.getEmail()));

        // 2. Validation
        validateBid(auction, bidder, bidderWallet, bidAmount);

        // 3. Refund Previous Bidder: Agar koi purana bidder tha, uske paise unlock karo
        if (auction.getHighestBidder() != null) {
            User previousBidder = auction.getHighestBidder();
            // Khud ki purani bid ko refund karne ki zarurat nahi agar logic simple rakhna hai
            if (!previousBidder.getId().equals(bidder.getId())) {
                walletRepository.findByUser(previousBidder).ifPresent(prevWallet -> {
                    log.info("Refunding previous bidder: {}", previousBidder.getEmail());
                    prevWallet.setLockedAmount(BigDecimal.ZERO);
                    walletRepository.save(prevWallet);
                });
            }
        }

        // 4. Lock New Bidder's Money
        bidderWallet.setLockedAmount(bidAmount);
        walletRepository.save(bidderWallet);

        // 5. Update Auction State
        auction.setCurrentHighestBid(bidAmount);
        auction.setHighestBidder(bidder);
        auctionRepository.save(auction);

        // 6. Save Bid Record
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(bidAmount)
                .successful(true)
                .build();

        Bid savedBid = bidRepository.save(bid);

        // 7. WebSocket Broadcast
        broadcastUpdate(auctionId, bidAmount, bidder.getFullName());

        return savedBid;
    }

    private void validateBid(Auction auction, User bidder, Wallet wallet, BigDecimal amount) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction active nahi hai, Sensei!");
        }
        if (auction.getEndTime() != null && LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new IllegalStateException("Auction khatam ho chuki hai.");
        }
        if (amount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Bid current price se zyada honi chahiye.");
        }
        // Available Balance = Total - Locked (Ensure your Wallet entity has this logic)
        if (wallet.getTotalBalance().subtract(wallet.getLockedAmount()).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Wallet mein itne paise nahi hain!");
        }
    }

    private void broadcastUpdate(Long auctionId, BigDecimal amount, String bidderName) {
        String destination = "/topic/auctions/" + auctionId;
        AuctionUpdateDTO payload = AuctionUpdateDTO.builder()
                .auctionId(auctionId)
                .newPrice(amount)
                .bidder(bidderName)
                .timestamp(LocalDateTime.now().toString())
                .build();

        // SENSEI: Yahan 'payload' use karo, kyunki upar wahi define kiya hai
        messagingTemplate.convertAndSend(destination, payload);
    }
}