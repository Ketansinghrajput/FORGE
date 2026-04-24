package com.forge.platform.service;

import com.forge.platform.dto.AuctionRequest;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;

    @Transactional
    public Auction createAuction(AuctionRequest request, User seller) {
        // 1. Roadmap Validation: End time start se pehle nahi ho sakti
        if (request.endTime().isBefore(request.startTime())) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        // 2. Roadmap Validation: Start time past mein nahi ho sakti
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction cannot start in the past");
        }

        Auction auction = Auction.builder()
                .title(request.title())
                .description(request.description())
                .startingPrice(request.startingPrice())
                .currentHighestBid(request.startingPrice()) // Initial highest bid is starting price
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AuctionStatus.PENDING)
                .metadata(request.metadata())
                .seller(seller)
                .build();

        return auctionRepository.save(auction);
    }

    public List<Auction> getAllActiveAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE);
    }
}