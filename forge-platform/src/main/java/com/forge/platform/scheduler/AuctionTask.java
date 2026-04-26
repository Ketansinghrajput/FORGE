package com.forge.platform.scheduler;

import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionTask {

    private final AuctionService auctionService;

    // Har 1 minute mein chalega (60000ms)
    @Scheduled(fixedRate = 60000)
    public void checkExpiredAuctions() {
        log.info("AuctionTask: Running scheduled expiry check...");
        try {
            auctionService.processExpiredAuctions();
        } catch (Exception e) {
            log.error("Error during auction expiry task: {}", e.getMessage());
        }
    }
}