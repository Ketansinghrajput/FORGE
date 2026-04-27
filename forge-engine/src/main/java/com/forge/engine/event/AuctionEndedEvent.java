package com.forge.engine.event;

public record AuctionEndedEvent(Long auctionId) implements EngineEvent {
}