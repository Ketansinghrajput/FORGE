package com.forge.engine.bidding;

import com.forge.engine.event.EventBus;
import com.forge.engine.model.*;
import com.forge.engine.tracker.PriceTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BiddingEngineConcurrencyTest {

    private BiddingEngine engine;
    private static final Long AUCTION_ID = 1L;

    @BeforeEach
    void setUp() {
        EventBus eventBus = new EventBus();
        engine = new BiddingEngine(eventBus);

        Bid startingBid = new Bid("SYSTEM", Money.inr(1000));
        AuctionContext context = new AuctionContext(
                new PriceTracker(startingBid),
                new AuctionStateMachine(),
                new BidBook()
        );
        context.getStateMachine().transitionTo(AuctionState.ACTIVE);
        engine.registerAuction(AUCTION_ID, context);
    }

    @Test
    void shouldHandle1000ConcurrentBidsCorrectly() throws InterruptedException {
        int bidderCount = 1000;
        CountDownLatch ready = new CountDownLatch(bidderCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(bidderCount);

        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger acceptedCount = new AtomicInteger(0);

        for (int i = 0; i < bidderCount; i++) {
            final int bidderIndex = i;
            Thread.startVirtualThread(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    return;
                }

                Bid bid = new Bid("bidder-" + bidderIndex, Money.inr(1000 + bidderIndex + 1));
                Boolean result = engine.placeBid(AUCTION_ID, bid).join();
                results.add(result);
                if (result) acceptedCount.incrementAndGet();
                done.countDown();
            });
        }

        ready.await();
        go.countDown();
        done.await();

        assertEquals(bidderCount, results.size(), "All bids should be processed");
        assertTrue(acceptedCount.get() > 0, "At least some bids should be accepted");

        Bid winner = engine.getWinningBid(AUCTION_ID);
        assertNotNull(winner, "There should be a winner");
        assertTrue(winner.getPrice().isGreaterThan(Money.inr(1000)),
                "Winner price should be above starting price");
    }

    @Test
    void shouldRejectBidsLowerThanCurrentPrice() throws InterruptedException {
        Bid highBid = new Bid("bigspender", Money.inr(9000));
        assertTrue(engine.placeBid(AUCTION_ID, highBid).join());

        int lowBidderCount = 100;
        CountDownLatch done = new CountDownLatch(lowBidderCount);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < lowBidderCount; i++) {
            final int idx = i;
            Thread.startVirtualThread(() -> {
                Bid lowBid = new Bid("loser-" + idx, Money.inr(500 + idx));
                Boolean result = engine.placeBid(AUCTION_ID, lowBid).join();
                if (!result) rejectedCount.incrementAndGet();
                done.countDown();
            });
        }

        done.await();
        assertEquals(lowBidderCount, rejectedCount.get(), "All low bids should be rejected");
    }

    @Test
    void shouldNotAcceptBidsAfterAuctionEnded() throws InterruptedException {
        engine.endAuction(AUCTION_ID);

        int bidderCount = 50;
        CountDownLatch done = new CountDownLatch(bidderCount);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < bidderCount; i++) {
            final int idx = i;
            Thread.startVirtualThread(() -> {
                Bid bid = new Bid("bidder-" + idx, Money.inr(5000 + idx));
                Boolean result = engine.placeBid(AUCTION_ID, bid).join();
                if (!result) rejectedCount.incrementAndGet();
                done.countDown();
            });
        }

        done.await();
        assertEquals(bidderCount, rejectedCount.get(), "All bids after auction end should be rejected");
    }
}