package com.forge.engine.bidding;

import com.forge.engine.model.Bid;
import com.forge.engine.model.BidKey;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class BidBook {
    // Thread-safe O(log n) sorting
    private final NavigableMap<BidKey, Bid> bids = new ConcurrentSkipListMap<>();

    public void addBid(Bid bid) {
        bids.put(new BidKey(bid.amount(), bid.timestamp()), bid);
    }

    public Bid getBestBid() {
        return bids.isEmpty() ? null : bids.firstEntry().getValue();
    }
}