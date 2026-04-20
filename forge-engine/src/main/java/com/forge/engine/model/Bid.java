package com.forge.engine.model;

import java.time.Instant;

public record Bid(Long bidderId, Long auctionId, Money amount, Instant timestamp) {}