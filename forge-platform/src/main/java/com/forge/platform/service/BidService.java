package com.forge.platform.service;

import com.forge.platform.dto.BidResponseDto; // DTO import zaroori hai
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;

    @Transactional(readOnly = true)
    public List<BidResponseDto> getMyBids(Long userId) {
        return bidRepository.findByBidderId(userId).stream().map(bid -> {
            Auction auction = bid.getAuction();

            // 🔥 SENSEI FIX: Using correct column names from your DB
            // DB mein 'current_highest_bid' hai toh entity mein 'currentHighestBid' hoga
            boolean isTopBid = bid.getAmount().compareTo(auction.getCurrentHighestBid()) == 0;

            return new BidResponseDto(
                    bid.getId(),
                    bid.getAmount(),
                    auction.getId(),
                    auction.getTitle(),
                    auction.getStatus().name(),
                    auction.getImageUrl(),
                    isTopBid,
                    bid.getCreatedAt(),
                    bid.getBidder().getId(), // current bidder ID
                    // 🔥 SENSEI FIX: Match with 'highest_bidder_id' column
                    auction.getHighestBidder() != null ? auction.getHighestBidder().getId() : null
            );
        }).collect(Collectors.toList());
    }
}