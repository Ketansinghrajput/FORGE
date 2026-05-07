package com.forge.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidResponseDto(
        Long bidId,
        BigDecimal amount,
        Long auctionId,
        String auctionTitle,
        String auctionStatus,
        String imageUrl,
        boolean successful,
        LocalDateTime placedAt,
        Long bidderId,
        Long highestBidderId
) {}