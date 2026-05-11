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
@NoArgsConstructor
public class AuctionUpdateDTO {
    private Long auctionId;
    private BigDecimal newPrice;

    @MaskPII
    private String bidder;

    private BigDecimal availableFunds;

    private String bidderName;

    private String endTime;

    private String timestamp;
}