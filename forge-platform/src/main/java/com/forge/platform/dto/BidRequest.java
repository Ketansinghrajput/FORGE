package com.forge.platform.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BidRequest {
    private Long auctionId;
    private String userEmail;
    private BigDecimal amount;
    private BigDecimal bidAmount;
}