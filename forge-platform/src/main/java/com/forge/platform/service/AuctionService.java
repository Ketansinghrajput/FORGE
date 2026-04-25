package com.forge.platform.service;

import com.forge.platform.dto.AuctionRequest;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Auction createAuction(AuctionRequest request, User seller) {
        if (request.endTime().isBefore(request.startTime())) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction cannot start in the past");
        }

        Auction auction = Auction.builder()
                .title(request.title())
                .description(request.description())
                .startingPrice(request.startingPrice())
                .currentHighestBid(request.startingPrice())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AuctionStatus.PENDING)
                .metadata(request.metadata())
                .seller(seller)
                .build();

        return auctionRepository.save(auction);
    }

    @Transactional
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, now);

        if (expiredAuctions.isEmpty()) {
            return;
        }

        for (Auction auction : expiredAuctions) {
            // 2. Mark as COMPLETED (Enum type)
            auction.setStatus(AuctionStatus.CLOSED);
            auctionRepository.save(auction);

            System.out.println("🔒 Auction Closed: " + auction.getTitle() + " | Winner ID: " +
                    (auction.getHighestBidder() != null ? auction.getHighestBidder().getId() : "No Bids"));

            // 3. Fire WebSocket Event so UI knows it's over!
            messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId(),
                    "{\"status\": \"COMPLETED\", \"message\": \"Auction has ended!\"}");
        }
    }

    public List<Auction> getAllActiveAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE);
    }
}