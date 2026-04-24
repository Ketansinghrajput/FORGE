package com.forge.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionRequest(
        String title,
        String description,
        BigDecimal startingPrice,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String metadata
) {}