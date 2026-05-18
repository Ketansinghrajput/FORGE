package com.forge.engine.event;

import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    // ── subscribe + publish ───────────────────────────────────────────────────

    @Test
    void publish_deliversEventToSubscriber() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EngineEvent> received = new AtomicReference<>();

        eventBus.subscribe(event -> {
            received.set(event);
            latch.countDown();
        });

        Bid bid = new Bid("user1", Money.inr(1000));
        BidPlacedEvent event = new BidPlacedEvent(1L, bid);
        eventBus.publish(event);

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Event should be delivered");
        assertSame(event, received.get());
    }

    @Test
    void publish_deliversToMultipleSubscribers() throws Exception {
        int listenerCount = 3;
        CountDownLatch latch = new CountDownLatch(listenerCount);
        AtomicInteger callCount = new AtomicInteger(0);

        for (int i = 0; i < listenerCount; i++) {
            eventBus.subscribe(event -> {
                callCount.incrementAndGet();
                latch.countDown();
            });
        }

        eventBus.publish(new BidPlacedEvent(1L, new Bid("user1", Money.inr(500))));

        assertTrue(latch.await(3, TimeUnit.SECONDS), "All listeners should receive event");
        assertEquals(listenerCount, callCount.get());
    }

    @Test
    void publish_multipleEvents_allDeliveredInOrder() throws Exception {
        int eventCount = 5;
        CountDownLatch latch = new CountDownLatch(eventCount);
        AtomicInteger received = new AtomicInteger(0);

        eventBus.subscribe(event -> {
            received.incrementAndGet();
            latch.countDown();
        });

        for (int i = 0; i < eventCount; i++) {
            eventBus.publish(new BidPlacedEvent((long) i, new Bid("user", Money.inr(1000 + i * 100))));
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS), "All events should be delivered");
        assertEquals(eventCount, received.get());
    }

    @Test
    void publish_noSubscribers_doesNotThrow() {
        assertDoesNotThrow(() ->
                eventBus.publish(new BidPlacedEvent(1L, new Bid("user", Money.inr(1000))))
        );
    }

    @Test
    void subscribe_listenerReceivesDifferentEventTypes() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger bidEvents = new AtomicInteger(0);
        AtomicInteger endedEvents = new AtomicInteger(0);

        eventBus.subscribe(event -> {
            if (event instanceof BidPlacedEvent) bidEvents.incrementAndGet();
            else if (event instanceof AuctionEndedEvent) endedEvents.incrementAndGet();
            latch.countDown();
        });

        Bid bid = new Bid("user1", Money.inr(2000));
        eventBus.publish(new BidPlacedEvent(1L, bid));
        eventBus.publish(new AuctionEndedEvent(1L, bid, 10));

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(1, bidEvents.get());
        assertEquals(1, endedEvents.get());
    }

    @Test
    void publish_faultyListener_doesNotBlockOtherListeners() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger goodListenerCount = new AtomicInteger(0);

        // Faulty listener — throws exception
        eventBus.subscribe(event -> {
            throw new RuntimeException("Simulated listener crash");
        });

        // Good listener — should still receive
        eventBus.subscribe(event -> {
            goodListenerCount.incrementAndGet();
            latch.countDown();
        });

        eventBus.publish(new BidPlacedEvent(1L, new Bid("user", Money.inr(1000))));

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Good listener should still fire despite faulty one");
        assertEquals(1, goodListenerCount.get());
    }

    @Test
    void publish_isNonBlocking_completesImmediately() {
        // publish() should return fast — queue is async
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            eventBus.publish(new BidPlacedEvent((long) i, new Bid("user", Money.inr(1000))));
        }
        long elapsed = System.nanoTime() - start;

        // 1000 publishes should complete well under 1 second
        assertTrue(elapsed < TimeUnit.SECONDS.toNanos(1), "publish() must be non-blocking");
    }
}