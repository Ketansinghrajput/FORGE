package com.forge.platform.scheduler;

import com.forge.platform.entity.Auction;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionTask {

    private final AuctionRepository auctionRepository;

    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void closeExpiredAuctions() {
        try {
            log.info("Checking for expired auctions at {}", LocalDateTime.now());

            List<Auction> expiredAuctions = auctionRepository.findAllByStatusAndEndTimeBefore(
                    AuctionStatus.ACTIVE, LocalDateTime.now());

            if (expiredAuctions.isEmpty()) {
                log.info("No expired auctions found.");
                return;
            }

            for (Auction auction : expiredAuctions) {
                log.info("Closing auction ID: {}", auction.getId());
                auction.setStatus(AuctionStatus.CLOSED);
            }

            auctionRepository.saveAll(expiredAuctions);
            log.info("Successfully closed {} auctions.", expiredAuctions.size());

        } catch (Exception e) {
            log.warn("Could not check auctions: Database table might not be ready yet. Retrying in 1 minute...");
        }
    }
}