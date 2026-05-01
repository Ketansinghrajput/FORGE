package com.forge.platform.dto;

import io.github.ketansingh.piimasker.annotation.MaskPII;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // 🚀 SENSEI: JSON deserialization ke liye zaroori hai
public class AuctionUpdateDTO {
    private Long auctionId;
    private BigDecimal newPrice;

    // 🚀 SENSEI: Teri pii-masker library yahan bidder ka email/name mask kar degi logs mein
    @MaskPII
    private String bidder;

    private String endTime; // 🚀 SENSEI FIX: Timer sync ke liye ye field add kar di hai

    private String timestamp;
}