package com.forge.engine.bidding;

import com.forge.engine.event.EventBus;
import com.forge.engine.event.EngineEventListener;
import com.forge.engine.event.BidPlacedEvent;
import com.forge.engine.model.*;
import com.forge.engine.tracker.PriceTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BiddingEngineTest {

    private BiddingEngine engine;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        engine = new BiddingEngine(eventBus);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AuctionContext buildActiveContext(double startingPrice) {
        Bid startingBid = new Bid("system", Money.inr(startingPrice));
        PriceTracker tracker = new PriceTracker(startingBid);
        AuctionStateMachine sm = new AuctionStateMachine();
        sm.transitionTo(AuctionState.ACTIVE);
        BidBook book = new BidBook();
        return new AuctionContext(tracker, sm, book);
    }

    // ── registerAuction / unregisterAuction ───────────────────────────────────

    @Test
    void placeBid_returnsTrue_whenAuctionActiveAndBidHigher() throws Exception {
        engine.registerAuction(1L, buildActiveContext(1000));

        Bid bid = new Bid("user1", Money.inr(1500));
        boolean accepted = engine.placeBid(1L, bid).get(2, TimeUnit.SECONDS);

        assertTrue(accepted);
    }

    @Test
    void placeBid_returnsFalse_whenAuctionNotRegistered() throws Exception {
        Bid bid = new Bid("user1", Money.inr(9999));
        boolean accepted = engine.placeBid(999L, bid).get(2, TimeUnit.SECONDS);

        assertFalse(accepted);
    }

    @Test
    void placeBid_returnsFalse_whenBidTooLow() throws Exception {
        engine.registerAuction(2L, buildActiveContext(5000));

        Bid lowBid = new Bid("user1", Money.inr(3000));
        boolean accepted = engine.placeBid(2L, lowBid).get(2, TimeUnit.SECONDS);

        assertFalse(accepted);
    }

    @Test
    void placeBid_returnsFalse_whenBidEqualToCurrentPrice() throws Exception {
        engine.registerAuction(3L, buildActiveContext(2000));

        Bid equalBid = new Bid("user1", Money.inr(2000));
        boolean accepted = engine.placeBid(3L, equalBid).get(2, TimeUnit.SECONDS);

        assertFalse(accepted);
    }

    @Test
    void placeBid_returnsFalse_whenAuctionNotActive() throws Exception {
        Bid startingBid = new Bid("system", Money.inr(1000));
        PriceTracker tracker = new PriceTracker(startingBid);
        AuctionStateMachine sm = new AuctionStateMachine(); // stays DRAFT
        BidBook book = new BidBook();
        AuctionContext draftContext = new AuctionContext(tracker, sm, book);

        engine.registerAuction(4L, draftContext);

        Bid bid = new Bid("user1", Money.inr(2000));
        boolean accepted = engine.placeBid(4L, bid).get(2, TimeUnit.SECONDS);

        assertFalse(accepted);
    }

    @Test
    void placeBid_returnsFalse_afterAuctionUnregistered() throws Exception {
        engine.registerAuction(5L, buildActiveContext(1000));
        engine.unregisterAuction(5L);

        Bid bid = new Bid("user1", Money.inr(2000));
        boolean accepted = engine.placeBid(5L, bid).get(2, TimeUnit.SECONDS);

        assertFalse(accepted);
    }

    // ── getWinningBid ─────────────────────────────────────────────────────────

    @Test
    void getWinningBid_returnsNull_whenAuctionNotFound() {
        assertNull(engine.getWinningBid(99L));
    }

    @Test
    void getWinningBid_returnsHighestBid_afterMultipleBids() throws Exception {
        engine.registerAuction(6L, buildActiveContext(1000));

        engine.placeBid(6L, new Bid("user1", Money.inr(2000))).get(2, TimeUnit.SECONDS);
        engine.placeBid(6L, new Bid("user2", Money.inr(5000))).get(2, TimeUnit.SECONDS);
        engine.placeBid(6L, new Bid("user3", Money.inr(3000))).get(2, TimeUnit.SECONDS); // rejected

        Bid winner = engine.getWinningBid(6L);
        assertNotNull(winner);
        assertEquals("user2", winner.getBidderId());
        assertEquals(Money.inr(5000), winner.getPrice());
    }

    // ── endAuction ────────────────────────────────────────────────────────────

    @Test
    void endAuction_preventsSubsequentBids() throws Exception {
        engine.registerAuction(7L, buildActiveContext(1000));
        engine.endAuction(7L);

        Bid bid = new Bid("user1", Money.inr(2000));
        boolean accepted = engine.placeBid(7L, bid).get(2, TimeUnit.SECONDS);

        assertFalse(accepted);
    }

    @Test
    void endAuction_onNonExistentAuction_doesNotThrow() {
        assertDoesNotThrow(() -> engine.endAuction(999L));
    }

    // ── EventBus integration ──────────────────────────────────────────────────

    @Test
    void placeBid_publishesBidPlacedEvent_toEventBus() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);

        eventBus.subscribe(event -> {
            if (event instanceof BidPlacedEvent bidEvent) {
                eventCount.incrementAndGet();
                latch.countDown();
            }
        });

        engine.registerAuction(8L, buildActiveContext(1000));
        engine.placeBid(8L, new Bid("user1", Money.inr(2000))).get(2, TimeUnit.SECONDS);

        boolean fired = latch.await(3, TimeUnit.SECONDS);
        assertTrue(fired, "BidPlacedEvent should have been published");
        assertEquals(1, eventCount.get());
    }

    @Test
    void placeBid_doesNotPublishEvent_whenBidRejected() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);

        eventBus.subscribe(event -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });

        engine.registerAuction(9L, buildActiveContext(5000));
        engine.placeBid(9L, new Bid("user1", Money.inr(1000))).get(2, TimeUnit.SECONDS); // too low

        boolean fired = latch.await(500, TimeUnit.MILLISECONDS);
        assertFalse(fired, "No event should be published for rejected bid");
        assertEquals(0, eventCount.get());
    }

    // ── Concurrent bids ───────────────────────────────────────────────────────

    @Test
    void placeBid_concurrent_onlyOneWinner() throws Exception {
        engine.registerAuction(10L, buildActiveContext(1000));

        int threadCount = 20;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger(0);

        CompletableFuture<?>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                ready.countDown();
                try {
                    go.await();
                    // All bid same amount — only first CAS wins
                    boolean result = engine.placeBid(10L, new Bid("user" + idx, Money.inr(5000)))
                            .get(2, TimeUnit.SECONDS);
                    if (result) accepted.incrementAndGet();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        go.countDown();
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        // Only 1 bid at exactly 5000 can win (rest are equal, CAS rejects)
        assertEquals(1, accepted.get(), "Only one bid should be accepted at the same price");
    }

    @Test
    void placeBid_concurrent_escalatingBids_allAccepted() throws Exception {
        engine.registerAuction(11L, buildActiveContext(1000));

        // Sequential escalating bids — each should be accepted
        boolean b1 = engine.placeBid(11L, new Bid("u1", Money.inr(2000))).get(2, TimeUnit.SECONDS);
        boolean b2 = engine.placeBid(11L, new Bid("u2", Money.inr(3000))).get(2, TimeUnit.SECONDS);
        boolean b3 = engine.placeBid(11L, new Bid("u3", Money.inr(4000))).get(2, TimeUnit.SECONDS);

        assertTrue(b1);
        assertTrue(b2);
        assertTrue(b3);
        assertEquals(Money.inr(4000), engine.getWinningBid(11L).getPrice());
    }
}