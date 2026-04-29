package com.forge.platform.dto;

import io.github.ketansingh.piimasker.annotation.MaskPII;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AuctionUpdateDTO {
    private Long auctionId;
    private BigDecimal newPrice;

    // 🚀 SENSEI FIX: Teri custom library ka magic idhar trigger hoga
    @MaskPII
    private String bidder;

    private String timestamp;
}