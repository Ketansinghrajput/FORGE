package com.forge.engine.event;

import com.forge.engine.model.Bid;

public record AuctionEndedEvent(
        Long auctionId,
        Bid winningBid,
        int totalBids
) implements EngineEvent {
}