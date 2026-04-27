package com.forge.engine.event;

import com.forge.engine.model.Bid;

public record BidPlacedEvent(Long auctionId, Bid bid) implements EngineEvent {
}