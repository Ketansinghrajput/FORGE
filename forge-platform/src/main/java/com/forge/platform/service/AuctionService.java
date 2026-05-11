package com.forge.platform.service;

import com.forge.platform.dto.AuctionRequest;
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
import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable; // 🔥 SENSEI: Cache removed for real-time DB sync
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final WalletService walletService;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @CacheEvict(value = "activeAuctions", allEntries = true)
    @Transactional
    public Auction createAuction(AuctionRequest request, User seller) {
        log.info("SENSEI DEBUG: Creating auction: {} | StartTime: {}", request.title(), request.startTime());

        if (request.startTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new IllegalArgumentException("Auction cannot start in the past");
        }
        if (request.endTime().isBefore(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        AuctionStatus initialStatus = request.startTime().isAfter(LocalDateTime.now())
                ? AuctionStatus.PLANNED
                : AuctionStatus.ACTIVE;

        Auction auction = Auction.builder()
                .title(request.title())
                .description(request.description())
                .startingPrice(request.startingPrice())
                .currentHighestBid(request.startingPrice())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .imageUrl(request.imageUrl())
                .status(initialStatus)
                .seller(seller)
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        log.info("Auction saved with status: {}", savedAuction.getStatus());

        return savedAuction;
    }

    @Transactional
    public void placeBid(Long auctionId, String bidderEmail, BigDecimal bidAmount) {
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(() -> new RuntimeException("Bidder not found"));
        placeBid(auctionId, bidder, bidAmount);
    }

    @CacheEvict(value = "activeAuctions", allEntries = true)
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

            if (!previousBidder.getId().equals(newBidder.getId())) {
                notificationService.sendOutbidEmail(
                        previousBidder.getEmail(),
                        auction.getTitle(),
                        bidAmount
                );
            }
        }

        auction.setHighestBidder(newBidder);
        auction.setCurrentHighestBid(bidAmount);
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
    }

    @Transactional
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, now);

        for (Auction auction : expiredAuctions) {
            try {
                if (auction.getHighestBidder() != null) {
                    User winner = auction.getHighestBidder();
                    BigDecimal finalAmount = auction.getCurrentHighestBid();

                    log.info("SETTLING: Auction {} | Winner: {}", auction.getId(), winner.getEmail());

                    walletService.settleAuction(winner, auction.getSeller(), finalAmount);
                    auction.setStatus(AuctionStatus.COMPLETED);

                    notificationService.sendAuctionWonEmail(
                            winner.getEmail(),
                            auction.getTitle(),
                            finalAmount
                    );

                    bidRepository.findHighestBidForAuction(auction.getId())
                            .ifPresent(winningBid -> {
                                winningBid.setSuccessful(true);
                                bidRepository.save(winningBid);
                            });
                } else {
                    auction.setStatus(AuctionStatus.EXPIRED);
                }

                auctionRepository.saveAndFlush(auction);
                broadcastAuctionUpdate(auction, "AUCTION_COMPLETED");

            } catch (Exception e) {
                log.error("SENSEI ERROR: Failed to settle auction ID {}: {}", auction.getId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInitialAuctionState(Long auctionId, String userEmail) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        String leader = (auction.getHighestBidder() != null)
                ? auction.getHighestBidder().getFullName()
                : "Waiting for Bids...";

        Wallet wallet = walletRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        String effectiveStatus = auction.getStatus().name();
        if (effectiveStatus.equals("ACTIVE") && auction.getEndTime().isBefore(LocalDateTime.now())) {
            effectiveStatus = "COMPLETED";
        }

        // 🔥 SENSEI FIX: Option A - Saari bids fetch kar li
        List<Bid> pastBids = bidRepository.findByAuctionIdOrderByAmountDesc(auctionId);

        List<Map<String, Object>> history = pastBids.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("amount", b.getAmount());
            map.put("bidderName", b.getBidder().getFullName());
            // Time chahiye toh b.getPlacedAt() bhej sakte ho
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("currentBid", auction.getCurrentHighestBid());
        response.put("highestBidder", leader);
        response.put("availableFunds", wallet.getTotalBalance());
        response.put("title", auction.getTitle());
        response.put("description", auction.getDescription());
        response.put("status", effectiveStatus);
        response.put("endTime", auction.getEndTime().toString());
        response.put("imageUrl", auction.getImageUrl());
        response.put("history", history); // Angular ko poori array milegi

        return response;
    }

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
    public Map<String, Object> getActiveAuctionsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("endTime").ascending());

        List<AuctionStatus> targetStatuses = List.of(AuctionStatus.ACTIVE, AuctionStatus.PLANNED);
        Page<Auction> auctionPage = auctionRepository.findByStatusIn(targetStatuses, pageable);

        List<Map<String, Object>> content = auctionPage.getContent().stream()
                .map(auction -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", auction.getId());
                    map.put("title", auction.getTitle());
                    map.put("currentHighestBid", auction.getCurrentHighestBid());
                    map.put("startTime", auction.getStartTime().toString());
                    map.put("endTime", auction.getEndTime().toString());
                    map.put("imageUrl", auction.getImageUrl());
                    map.put("description", auction.getDescription());
                    map.put("status", auction.getStatus().name());
                    map.put("sellerEmail", auction.getSeller() != null ? auction.getSeller().getEmail() : null);
                    return map;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", auctionPage.getTotalPages());
        response.put("totalElements", auctionPage.getTotalElements());
        return response;
    }

    private void broadcastAuctionUpdate(Auction auction, String type) {
        String bidderName = (auction.getHighestBidder() != null)
                ? auction.getHighestBidder().getFullName() : "System";

        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auction.getId());
        payload.put("status", auction.getStatus().name());
        payload.put("newPrice", auction.getCurrentHighestBid());
        payload.put("type", type);
        payload.put("bidderName", bidderName);
        payload.put("highestBidder", auction.getHighestBidder() != null
                ? auction.getHighestBidder().getEmail() : null);

        messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId(), payload);
    }


    @Transactional(readOnly = true)
    public Map<String, Object> getResultAuctions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("endTime").descending());

        List<AuctionStatus> closedStatuses = List.of(
                AuctionStatus.COMPLETED,
                AuctionStatus.CLOSED,
                AuctionStatus.EXPIRED,
                AuctionStatus.CANCELLED
        );

        // Fetch formally closed auctions + ACTIVE ones whose endTime has passed
        Page<Auction> auctionPage = auctionRepository.findResultAuctions(
                closedStatuses,
                LocalDateTime.now(),
                pageable
        );

        List<Map<String, Object>> content = auctionPage.getContent().stream()
                .map(auction -> {
                    // CLOSED with a winner = sold, CLOSED with no bids = expired
                    String displayStatus = auction.getStatus().name();
                    if (auction.getStatus() == AuctionStatus.ACTIVE
                            && auction.getEndTime().isBefore(LocalDateTime.now())) {
                        displayStatus = auction.getHighestBidder() != null
                                ? "COMPLETED" : "EXPIRED";
                    }

                    Map<String, Object> map = new HashMap<>();
                    map.put("id",          auction.getId());
                    map.put("title",       auction.getTitle());
                    map.put("description", auction.getDescription());
                    map.put("imageUrl",    auction.getImageUrl());
                    map.put("status",      displayStatus);
                    map.put("startPrice",  auction.getStartingPrice());
                    map.put("currentPrice",auction.getCurrentHighestBid());
                    map.put("endTime",     auction.getEndTime().toString());
                    map.put("sellerEmail", auction.getSeller() != null
                            ? auction.getSeller().getEmail() : null);

                    if (auction.getHighestBidder() != null) {
                        map.put("winnerName",  auction.getHighestBidder().getFullName());
                        map.put("winnerEmail", auction.getHighestBidder().getEmail());
                    } else {
                        map.put("winnerName",  null);
                        map.put("winnerEmail", null);
                    }

                    map.put("bidCount", bidRepository.countByAuctionId(auction.getId()));
                    return map;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content",       content);
        response.put("totalPages",    auctionPage.getTotalPages());
        response.put("totalElements", auctionPage.getTotalElements());
        return response;
    }
}