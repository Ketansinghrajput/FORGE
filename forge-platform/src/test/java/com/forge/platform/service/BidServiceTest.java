package com.forge.platform.service;

import com.forge.platform.dto.BidResponseDto;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.BidRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock BidRepository bidRepository;
    @InjectMocks BidService bidService;

    private Bid buildBid(Long bidId, BigDecimal bidAmount, BigDecimal auctionHighest, User highestBidder) {
        User bidder = new User();
        bidder.setId(1L);

        Auction auction = new Auction();
        auction.setId(10L);
        auction.setTitle("Vintage Guitar");
        auction.setCurrentHighestBid(auctionHighest);
        auction.setHighestBidder(highestBidder);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setImageUrl("http://img.test/guitar.jpg");

        return Bid.builder()
                .id(bidId)
                .amount(bidAmount)
                .auction(auction)
                .bidder(bidder)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getMyBids_isTopBid_true_whenAmountMatchesHighest() {
        User highestBidder = new User();
        highestBidder.setId(1L);

        Bid bid = buildBid(1L, new BigDecimal("500.00"), new BigDecimal("500.00"), highestBidder);
        when(bidRepository.findByBidderId(1L)).thenReturn(List.of(bid));

        List<BidResponseDto> result = bidService.getMyBids(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).successful()).isTrue();
        assertThat(result.get(0).highestBidderId()).isEqualTo(1L);
    }

    @Test
    void getMyBids_isTopBid_false_whenAmountBelowHighest() {
        User highestBidder = new User();
        highestBidder.setId(99L);

        Bid bid = buildBid(2L, new BigDecimal("300.00"), new BigDecimal("500.00"), highestBidder);
        when(bidRepository.findByBidderId(1L)).thenReturn(List.of(bid));

        List<BidResponseDto> result = bidService.getMyBids(1L);

        assertThat(result.get(0).successful()).isFalse();
        assertThat(result.get(0).highestBidderId()).isEqualTo(99L);
    }

    @Test
    void getMyBids_noHighestBidder_highestBidderIdIsNull() {
        Bid bid = buildBid(3L, new BigDecimal("200.00"), new BigDecimal("200.00"), null);
        when(bidRepository.findByBidderId(1L)).thenReturn(List.of(bid));

        List<BidResponseDto> result = bidService.getMyBids(1L);

        assertThat(result.get(0).highestBidderId()).isNull();
    }

    @Test
    void getMyBids_emptyList_returnsEmpty() {
        when(bidRepository.findByBidderId(42L)).thenReturn(List.of());

        assertThat(bidService.getMyBids(42L)).isEmpty();
    }
}