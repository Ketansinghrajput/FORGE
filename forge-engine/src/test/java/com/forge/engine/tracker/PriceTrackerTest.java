package com.forge.engine.tracker;

import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PriceTrackerTest {

    private PriceTracker tracker;

    @BeforeEach
    void setUp() {
        Bid startingBid = new Bid("system", Money.inr(1000));
        tracker = new PriceTracker(startingBid);
    }

    // ── Basic correctness ─────────────────────────────────────────────────────

    @Test
    void updatePrice_returnsTrue_whenBidHigherThanCurrent() {
        Bid newBid = new Bid("user1", Money.inr(2000));
        assertTrue(tracker.updatePrice(newBid));
    }

    @Test
    void updatePrice_returnsFalse_whenBidLowerThanCurrent() {
        Bid lowBid = new Bid("user1", Money.inr(500));
        assertFalse(tracker.updatePrice(lowBid));
    }

    @Test
    void updatePrice_returnsFalse_whenBidEqualsCurrentPrice() {
        Bid equalBid = new Bid("user1", Money.inr(1000));
        assertFalse(tracker.updatePrice(equalBid));
    }

    @Test
    void getCurrentHighestBid_returnsStartingBid_initially() {
        Bid current = tracker.getCurrentHighestBid();
        assertNotNull(current);
        assertEquals(Money.inr(1000), current.amount());
        assertEquals("system", current.getBidderId());
    }

    @Test
    void getCurrentHighestBid_updatesAfterAcceptedBid() {
        tracker.updatePrice(new Bid("user1", Money.inr(3000)));

        Bid current = tracker.getCurrentHighestBid();
        assertEquals(Money.inr(3000), current.amount());
        assertEquals("user1", current.getBidderId());
    }

    @Test
    void updatePrice_chainsCorrectly_escalatingBids() {
        assertTrue(tracker.updatePrice(new Bid("u1", Money.inr(2000))));
        assertTrue(tracker.updatePrice(new Bid("u2", Money.inr(3000))));
        assertTrue(tracker.updatePrice(new Bid("u3", Money.inr(4000))));

        // Going back should fail
        assertFalse(tracker.updatePrice(new Bid("u4", Money.inr(3500))));

        assertEquals(Money.inr(4000), tracker.getCurrentHighestBid().amount());
    }

    @Test
    void updatePrice_nullStartingBid_acceptsFirstBid() {
        PriceTracker nullTracker = new PriceTracker(null);

        Bid firstBid = new Bid("user1", Money.inr(500));
        assertTrue(nullTracker.updatePrice(firstBid));
        assertEquals(Money.inr(500), nullTracker.getCurrentHighestBid().amount());
    }

    // ── Concurrency (CAS loop) ────────────────────────────────────────────────

    @Test
    void updatePrice_concurrent_onlyHighestBidWins() throws Exception {
        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger(0);

        // Thread i bids (i+1)*100 — highest bid = thread 49 bidding 5000
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final double price = (i + 1) * 100.0 + 1000; // 1100..6000
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return tracker.updatePrice(new Bid("user", Money.inr(price)));
            }));
        }

        ready.await();
        go.countDown();

        for (Future<Boolean> f : results) {
            if (f.get(3, TimeUnit.SECONDS)) accepted.incrementAndGet();
        }

        pool.shutdown();

        // Every escalating bid should be accepted (50 unique prices, each higher than last)
        // But concurrent writes mean CAS retries; final price must be max
        Bid winner = tracker.getCurrentHighestBid();
        assertNotNull(winner);
        // Highest possible bid was 6000
        assertEquals(Money.inr(6000), winner.amount());
    }

    @Test
    void updatePrice_concurrent_samePriceRace_onlyOneAccepted() throws Exception {
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger(0);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            results.add(pool.submit(() -> {
                go.await();
                return tracker.updatePrice(new Bid("user" + idx, Money.inr(5000)));
            }));
        }

        go.countDown();

        for (Future<Boolean> f : results) {
            if (f.get(3, TimeUnit.SECONDS)) accepted.incrementAndGet();
        }

        pool.shutdown();

        assertEquals(1, accepted.get(), "CAS must allow only one winner at same price");
    }

    @Test
    void updatePrice_concurrent_noRaceCondition_stateConsistent() throws Exception {
        int threadCount = 100;
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger totalAccepted = new AtomicInteger(0);

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final double price = 1001 + i * 10.0;
            futures.add(pool.submit(() -> {
                go.await();
                return tracker.updatePrice(new Bid("user", Money.inr(price)));
            }));
        }

        go.countDown();
        for (Future<Boolean> f : futures) {
            if (f.get(5, TimeUnit.SECONDS)) totalAccepted.incrementAndGet();
        }

        pool.shutdown();

        // Final state must be consistent — getCurrentHighestBid must never be null
        Bid finalBid = tracker.getCurrentHighestBid();
        assertNotNull(finalBid);
        // And total accepted must be > 0
        assertTrue(totalAccepted.get() > 0);
    }
}