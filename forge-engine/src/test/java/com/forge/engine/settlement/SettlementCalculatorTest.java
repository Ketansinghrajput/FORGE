package com.forge.engine.settlement;

import com.forge.engine.bidding.BidBook;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettlementCalculatorTest {

    private final SettlementCalculator calculator = new SettlementCalculator();

    @Test
    void shouldReturnUnsoldResultWhenNoBidsExist() {
        // Mock BidBook to return null (no bids)
        BidBook mockBidBook = mock(BidBook.class);
        when(mockBidBook.getBestBid()).thenReturn(null);

        SettlementResult result = calculator.calculate(101L, mockBidBook);

        assertFalse(result.settled(), "Auction should not be settled");
        assertNull(result.winnerId(), "Winner ID should be null for unsold auction");
        assertEquals(101L, result.auctionId(), "Auction ID should match");
    }

    @Test
    void shouldCalculateFullSettlementWhenWinningBidExists() {
        // Mock BidBook to return a winning bid
        BidBook mockBidBook = mock(BidBook.class);
        Bid mockBid = new Bid("Sensei-Winner", Money.inr(5000));
        when(mockBidBook.getBestBid()).thenReturn(mockBid);

        SettlementResult result = calculator.calculate(202L, mockBidBook);

        assertTrue(result.settled(), "Auction should be settled");
        assertEquals("Sensei-Winner", result.winnerId(), "Winner ID should match");
        assertEquals(202L, result.auctionId(), "Auction ID should match");
        assertNotNull(result.platformFee(), "Platform fee must be calculated");
        assertNotNull(result.taxAmount(), "Tax must be calculated");
        assertNotNull(result.sellerPayout(), "Payout must be calculated");
    }
}