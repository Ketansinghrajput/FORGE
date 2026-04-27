package com.forge.engine.bidding;

import com.forge.engine.model.Bid;
import com.forge.engine.model.AuctionContext;
import com.forge.engine.event.EventBus;
import com.forge.engine.event.BidPlacedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Slf4j
public class BiddingEngine {

    // THE HEART: Map of all active auctions.
    // Key = AuctionID, Value = Context (PriceTracker, StateMachine, BidBook)
    private final ConcurrentHashMap<Long, AuctionContext> activeAuctions = new ConcurrentHashMap<>();

    private final EventBus eventBus;

    // Java 21 Virtual Threads pool - Har bid ek naye lightweight thread pe chalegi
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public BiddingEngine(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Jab naya auction start hoga, Orchestrator yeh call karega.
     */
    public void registerAuction(Long auctionId, AuctionContext context) {
        activeAuctions.put(auctionId, context);
        log.info("Auction {} registered in Engine. Ready for bids.", auctionId);
    }
    public void unregisterAuction(Long auctionId) {
        // 'auctions' ki jagah 'activeAuctions' use karo
        activeAuctions.remove(auctionId);
        log.info("Auction {} unregistered from Engine. Memory cleared.", auctionId);
    }

    /**
     * 100% Lock-Free, Async Bid Processing
     */
    public CompletableFuture<Boolean> placeBid(Long auctionId, Bid newBid) {
        return CompletableFuture.supplyAsync(() -> {

            // 1. Find the auction
            AuctionContext context = activeAuctions.get(auctionId);
            if (context == null) {
                log.warn("Bid rejected: Auction {} not found in memory.", auctionId);
                return false;
            }

            // 2. READ LOCK: Check if auction is still taking bids (State = ACTIVE)
            if (!context.getStateMachine().isActive()) {
                return false;
            }

            // 3. CAS LOOP (Lock-Free Pricing): Try to update the price
            boolean isAccepted = context.getPriceTracker().updatePrice(newBid);

            if (isAccepted) {
                // 4. Update O(1) BidBook for winner tracking
                context.getBidBook().addBid(newBid);

                // 5. Fire Async Event (Yeh STOMP WebSocket ko trigger karega)
                // Note: Part 5 says we use sealed interface events, not String topics
                eventBus.publish(new BidPlacedEvent(auctionId, newBid));
                return true;
            }

            return false; // Bid amount was too low or CAS failed repeatedly

        }, virtualExecutor); // Pass the Virtual Thread Executor here
    }

    /**
     * WRITE LOCK: Ends the auction and stops new bids from entering
     */
    public void endAuction(Long auctionId) {
        AuctionContext context = activeAuctions.get(auctionId);
        if (context != null) {
            // Write Lock lag jayega, aur is auction ki processing stop
            context.getStateMachine().transitionToEnded();
            activeAuctions.remove(auctionId);
            log.info("Auction {} ended and removed from Engine Memory.", auctionId);
        }
    }
}