package com.forge.platform.scheduler;

import com.forge.platform.service.AuctionManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionManagerService auctionManager;

    // Har 1 minute mein check karega
    @Scheduled(fixedRate = 60000)
    public void processAuctionTransitions() {
        log.info("⏰ Running Auction Lifecycle Scheduler...");

        // 1. Jo start hone wale hain unhe Engine mein register karo
        auctionManager.startPendingAuctions();

        // 2. Jo khatam ho gaye hain unka winner announce karo
        auctionManager.closeExpiredAuctions();
    }
}