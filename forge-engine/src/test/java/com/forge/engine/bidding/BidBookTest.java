package com.forge.engine.bidding;

import com.forge.engine.model.Bid;
import com.forge.engine.model.BidKey;
import com.forge.engine.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BidBookTest {

    private BidBook bidBook;

    @BeforeEach
    void setUp() {
        bidBook = new BidBook();
    }

    @Test
    void shouldReturnNullWhenEmpty() {
        assertNull(bidBook.getBestBid());
    }

    @Test
    void shouldReturnHighestBidAsWinner() {
        Bid lowBid = new Bid("user1", Money.inr(1000));
        Bid highBid = new Bid("user2", Money.inr(5000));
        Bid midBid = new Bid("user3", Money.inr(3000));

        bidBook.addBid(lowBid);
        bidBook.addBid(highBid);
        bidBook.addBid(midBid);

        Bid winner = bidBook.getBestBid();
        assertNotNull(winner);
        assertEquals("user2", winner.getBidderId());
        assertEquals(Money.inr(5000), winner.getPrice());
    }

    @Test
    void shouldEnforceTimePriorityForEqualPrices() throws InterruptedException {
        Bid firstBid = new Bid("user1", Money.inr(1000));
        Thread.sleep(10);
        Bid secondBid = new Bid("user2", Money.inr(1000));

        bidBook.addBid(firstBid);
        bidBook.addBid(secondBid);

        Bid winner = bidBook.getBestBid();
        assertEquals("user1", winner.getBidderId());
    }

    @Test
    void shouldHandleMultipleBidsFromSameBidder() {
        bidBook.addBid(new Bid("user1", Money.inr(1000)));
        bidBook.addBid(new Bid("user1", Money.inr(2000)));

        Bid winner = bidBook.getBestBid();
        assertEquals(Money.inr(2000), winner.getPrice());
    }
}