package com.forge.platform.service;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.bidding.BidBook;
import com.forge.engine.model.AuctionContext;
import com.forge.engine.model.AuctionStateMachine;
import com.forge.engine.model.AuctionState;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import com.forge.engine.tracker.PriceTracker;
import com.forge.platform.entity.Auction;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionManagerService {

    private final AuctionRepository auctionRepository;
    private final BiddingEngine engine;
    private final WalletService walletService;

    @Transactional
    public void startPendingAuctions() {
        List<Auction> pending = auctionRepository.findPendingAuctions(LocalDateTime.now());

        for (Auction a : pending) {
            // Update Database
            a.setStatus(AuctionStatus.ACTIVE);
            auctionRepository.save(a);

            // Register in Engine
            Bid initialBid = new Bid("SYSTEM", new Money(a.getStartingPrice(), "INR"));
            AuctionContext context = new AuctionContext(
                    new PriceTracker(initialBid),
                    new AuctionStateMachine(),
                    new BidBook()
            );

            context.getStateMachine().transitionTo(AuctionState.ACTIVE);
            engine.registerAuction(a.getId(), context);

            log.info("🚀 Auction {} ('{}') is now LIVE!", a.getId(), a.getTitle());
        }
    }

    @Transactional
    public void closeExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredAuctions(LocalDateTime.now());

        for (Auction a : expired) {
            log.info("🏁 Closing Auction {}: {}", a.getId(), a.getTitle());

            // 1. Database Status Update
            a.setStatus(AuctionStatus.CLOSED);

            // 2. Money Settlement Logic
            if (a.getHighestBidder() != null) {
                try {
                    walletService.settleAuction(
                            a.getHighestBidder(),
                            a.getSeller(),
                            a.getCurrentHighestBid()
                    );
                    log.info("🏆 Winner: {} | Final Price: ₹{}", a.getHighestBidder().getEmail(), a.getCurrentHighestBid());
                } catch (Exception e) {
                    log.error("❌ Settlement failed for Auction {}: {}", a.getId(), e.getMessage());
                }
            } else {
                log.info("🛑 Auction {} ended with no participants.", a.getId());
            }

            auctionRepository.save(a);

            // 3. Engine Cleanup
            engine.unregisterAuction(a.getId());
        }
    }
}