package com.forge.engine.tracker;

import com.forge.engine.model.Bid;
import java.util.concurrent.atomic.AtomicReference;

public class PriceTracker {

    private final AtomicReference<Bid> currentHighestBid;

    public PriceTracker(Bid startingBid) {
        this.currentHighestBid = new AtomicReference<>(startingBid);
    }


    public boolean updatePrice(Bid newBid) {
        while (true) {
            // Step 1: Memory se current state read karo
            Bid currentBid = currentHighestBid.get();

            // Step 2: Business Validation - Naya bid purane se bada hona chahiye
            if (currentBid != null && newBid.amount().compareTo(currentBid.amount()) <= 0) {
                return false; // Bid rejected, price chota hai
            }

            // Step 3: CAS Operation (Hardware level atomic instruction)
            // Agar memory mein abhi bhi 'currentBid' hi hai, toh usko 'newBid' se replace kar do.
            if (currentHighestBid.compareAndSet(currentBid, newBid)) {
                return true; // Bid accepted and updated
            }

            // Step 1 aur Step 3 ke beech mein price badha diya.
        }
    }

    public Bid getCurrentHighestBid() {
        return currentHighestBid.get();
    }
}