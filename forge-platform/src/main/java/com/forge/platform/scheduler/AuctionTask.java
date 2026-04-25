package com.forge.platform.scheduler;

import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionTask {

    private final AuctionService auctionService;

    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void closeExpiredAuctions() {
        try {
            log.info("Cron Triggered: Checking for expired auctions at {}", LocalDateTime.now());

            auctionService.processExpiredAuctions();

        } catch (Exception e) {
            log.error("Could not process auctions. Error: {}", e.getMessage());
        }
    }
}