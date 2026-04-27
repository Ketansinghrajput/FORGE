package com.forge.engine.event;

// Sealed interface ka matlab hai iske alawa aur koi class isko implement nahi kar sakti.
// Yeh Java 21 ka strict pattern matching feature hai.
public sealed interface EngineEvent permits BidPlacedEvent, AuctionEndedEvent {
}