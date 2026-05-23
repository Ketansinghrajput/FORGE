package com.forge.engine.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BidKeyTest {

    @Test
    void compareTo_higherAmountWins() {
        BidKey high = new BidKey(Money.inr(5000), Instant.now());
        BidKey low  = new BidKey(Money.inr(3000), Instant.now());

        // high should come before low (descending price order)
        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    void compareTo_sameAmount_earlierTimestampWins() {
        Instant earlier = Instant.now();
        Instant later   = earlier.plusMillis(100);

        BidKey first  = new BidKey(Money.inr(2000), earlier);
        BidKey second = new BidKey(Money.inr(2000), later);

        // Earlier timestamp wins when price is equal
        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    void compareTo_sameAmountSameTimestamp_returnsZero() {
        Instant ts = Instant.now();
        BidKey a = new BidKey(Money.inr(1000), ts);
        BidKey b = new BidKey(Money.inr(1000), ts);

        assertEquals(0, a.compareTo(b));
    }

    @Test
    void accessors_returnCorrectValues() {
        Money price = Money.inr(7500);
        Instant ts  = Instant.now();
        BidKey key  = new BidKey(price, ts);

        assertEquals(price, key.amount());
        assertEquals(ts,    key.timestamp());
    }
}

class BidResultTest {

    @Test
    void accepted_factory_setsCorrectFields() {
        Bid bid = new Bid("user1", Money.inr(2000));
        Money price = Money.inr(2000);

        BidResult result = BidResult.accepted(bid, price);

        assertTrue(result.accepted());
        assertTrue(result.isAccepted());
        assertFalse(result.isRejected());
        assertEquals("Bid accepted", result.reason());
        assertEquals(bid, result.bid());
        assertEquals(price, result.currentPrice());
    }

    @Test
    void rejected_factory_setsCorrectFields() {
        BidResult result = BidResult.rejected("Bid too low");

        assertFalse(result.accepted());
        assertFalse(result.isAccepted());
        assertTrue(result.isRejected());
        assertEquals("Bid too low", result.reason());
        assertNull(result.bid());
        assertNull(result.currentPrice());
    }

    @Test
    void rejected_withInsufficientFundsReason() {
        BidResult result = BidResult.rejected("Insufficient funds");

        assertTrue(result.isRejected());
        assertEquals("Insufficient funds", result.reason());
    }
}

class BidTest {

    @Test
    void bid_getters_returnCorrectValues() {
        Money price = Money.inr(3000);
        Bid bid = new Bid("sensei", price);

        assertEquals("sensei", bid.getBidderId());
        assertEquals(price,    bid.getPrice());
        assertEquals(price,    bid.amount());
        assertNotNull(bid.getTimestamp());
        assertNotNull(bid.bidKey());
    }

    @Test
    void bid_bidKey_hasCorrectAmount() {
        Money price = Money.inr(5000);
        Bid bid = new Bid("user1", price);

        assertEquals(price, bid.bidKey().amount());
    }

    @Test
    void twoDistinctBids_haveDifferentTimestamps() throws Exception {
        Bid b1 = new Bid("u1", Money.inr(1000));
        Thread.sleep(1);
        Bid b2 = new Bid("u2", Money.inr(1000));

        assertNotEquals(b1.getTimestamp(), b2.getTimestamp());
    }
}

class AuctionContextTest {

    @Test
    void getters_returnSameInstances() {
        Bid startingBid = new Bid("system", Money.inr(1000));
        com.forge.engine.tracker.PriceTracker tracker = new com.forge.engine.tracker.PriceTracker(startingBid);
        AuctionStateMachine sm = new AuctionStateMachine();
        com.forge.engine.bidding.BidBook book = new com.forge.engine.bidding.BidBook();

        AuctionContext ctx = new AuctionContext(tracker, sm, book);

        assertSame(tracker, ctx.getPriceTracker());
        assertSame(sm,      ctx.getStateMachine());
        assertSame(book,    ctx.getBidBook());
    }
}