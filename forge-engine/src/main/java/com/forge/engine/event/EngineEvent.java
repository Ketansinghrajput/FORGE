package com.forge.engine.event;


public sealed interface EngineEvent permits BidPlacedEvent, AuctionEndedEvent {
}