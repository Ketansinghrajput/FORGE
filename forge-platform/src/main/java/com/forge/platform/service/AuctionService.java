package com.forge.platform.service;

import com.forge.platform.dto.AuctionRequest;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final WalletService walletService;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;

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
                .imageUrl(request.imageUrl())
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .build();

        return auctionRepository.save(auction);
    }

    @Transactional
    public void placeBid(Long auctionId, User newBidder, BigDecimal bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Auction is not active");
        }

        if (bidAmount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Bid must be higher than: " + auction.getCurrentHighestBid());
        }

        if (newBidder.getId().equals(auction.getSeller().getId())) {
            throw new IllegalArgumentException("Seller cannot bid on their own auction");
        }

        log.info("Locking funds for new bidder {}: ₹{}", newBidder.getEmail(), bidAmount);
        walletService.lockFunds(newBidder, bidAmount);

        if (auction.getHighestBidder() != null) {
            User previousBidder = auction.getHighestBidder();
            log.info("Outbidding {} | Refunding: ₹{}", previousBidder.getEmail(), auction.getCurrentHighestBid());
            walletService.unlockFunds(previousBidder, auction.getCurrentHighestBid());
        }

        auction.setHighestBidder(newBidder);
        auction.setCurrentHighestBid(bidAmount);

        // 🔥 Save and Flush immediately to prevent race conditions
        auctionRepository.saveAndFlush(auction);

        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(newBidder)
                .amount(bidAmount)
                .successful(false)
                .build();
        bidRepository.save(bid);

        broadcastAuctionUpdate(auction, "BID_PLACED");
        log.info("Bid successfully placed by {}", newBidder.getEmail());

        broadcastAuctionUpdate(auction, "BID_PLACED");
        log.info("Bid successfully placed by {}", newBidder.getEmail());
    }

    @Transactional
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("SENSEI DEBUG: Checking settlement at {}", now);

        // 🔥 SENSEI FIX: Direct DB query use karo (findAll().stream() is very slow)
        // Tune repository mein findByStatusAndEndTimeBefore banaya hai, wahi use kar.
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, now);

        if (expiredAuctions.isEmpty()) {
            return;
        }

        for (Auction auction : expiredAuctions) {
            String type = "AUCTION_EXPIRED";

            try {
                if (auction.getHighestBidder() != null) {
                    User winner = auction.getHighestBidder();
                    User seller = auction.getSeller();
                    BigDecimal finalAmount = auction.getCurrentHighestBid();

                    log.info("SETTLING: Auction {} | Winner: {}", auction.getId(), winner.getEmail());

                    // Financial transaction logic
                    walletService.settleAuction(winner, seller, finalAmount);
                    auction.setStatus(AuctionStatus.COMPLETED);
                    type = "AUCTION_COMPLETED";
                    bidRepository.findHighestBidForAuction(auction.getId())
                            .ifPresent(winningBid -> {
                                winningBid.setSuccessful(true);
                                bidRepository.save(winningBid);
                            });
                } else {
                    auction.setStatus(AuctionStatus.EXPIRED);
                }

                // 🔥 Save and Flush ensures DB status is updated BEFORE WebSocket broadcast
                auctionRepository.saveAndFlush(auction);
                broadcastAuctionUpdate(auction, type);

            } catch (Exception e) {
                log.error("SENSEI ERROR: Failed to settle auction ID {}: {}", auction.getId(), e.getMessage());
                // Important: Ek auction fail hone par loop rukna nahi chahiye
            }
        }
    }

    // 🔥 Added public modifier to fix Controller visibility error
    @Transactional
    public void deleteAuction(Long auctionId, User seller) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (!auction.getSeller().getId().equals(seller.getId())) {
            throw new IllegalStateException("Sirf apna auction delete kar sakte ho");
        }

        bidRepository.deleteByAuctionId(auctionId);
        auctionRepository.deleteById(auctionId);
        log.info("Auction ID {} deleted by seller {}", auctionId, seller.getEmail());
    }

    @Transactional(readOnly = true)
    public List<Auction> getAllActiveAuctions() {
        return auctionRepository.findAll().stream()
                // 🔥 Fixed: Proper comparison ==
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .peek(a -> {
                    // 🔥 Fixed: Proper null check !=
                    if (a.getSeller() != null) a.getSeller().getEmail();
                    if (a.getHighestBidder() != null) a.getHighestBidder().getEmail();
                })
                .toList();
    }

    private void broadcastAuctionUpdate(Auction auction, String type) {
        String winnerName = (auction.getHighestBidder() != null)
                ? auction.getHighestBidder().getFullName() : "None";

        // Get winner's balance
        BigDecimal availableFunds = BigDecimal.ZERO;
        if (auction.getHighestBidder() != null) {
            try {
                availableFunds = walletService.getWalletByUserId(
                        auction.getHighestBidder().getId()
                ).getAvailableBalance();
            } catch (Exception e) {
                log.warn("Could not fetch wallet balance for broadcast");
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auction.getId());
        payload.put("status", auction.getStatus().name());
        payload.put("currentHighestBid", auction.getCurrentHighestBid());
        payload.put("newPrice", auction.getCurrentHighestBid());
        payload.put("winnerName", winnerName);
        payload.put("highestBidder", auction.getHighestBidder() != null
                ? auction.getHighestBidder().getEmail() : null);
        payload.put("type", type);
        payload.put("availableFunds", availableFunds);
        payload.put("highestBidderEmail", auction.getHighestBidder() != null
                ? auction.getHighestBidder().getEmail() : null); //   explicit email field

        messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId(), payload);
    }
}