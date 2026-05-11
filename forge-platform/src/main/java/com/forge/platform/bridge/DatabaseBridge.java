package com.forge.platform.bridge;

import com.forge.engine.event.BidPlacedEvent;
import com.forge.engine.event.EventBus;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.repository.BidRepository;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBridge {

    private final EventBus eventBus;
    private final BidRepository bidRepository;
    private final EntityManager entityManager;

    @PostConstruct
    public void bridgeEngineToDatabase() {
        eventBus.subscribe(event -> {
            if (event instanceof BidPlacedEvent bidEvent) {
                saveBidToDb(bidEvent);
            }
        });
    }

    @Transactional
    protected void saveBidToDb(BidPlacedEvent event) {
        try {

            Auction auctionProxy = entityManager.getReference(Auction.class, event.auctionId());


            User bidderProxy = entityManager.getReference(User.class, Long.parseLong(event.bid().getBidderId()));

            Bid bid = Bid.builder()
                    .auction(auctionProxy)
                    .bidder(bidderProxy)
                    .amount(event.bid().getPrice().getAmount())
                    .successful(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            bidRepository.save(bid);
            log.info("💾 Bid persisted to DB for Auction: {}", event.auctionId());
        } catch (Exception e) {
            log.error("❌ DatabaseBridge persistence failed: {}", e.getMessage());
        }
    }
}