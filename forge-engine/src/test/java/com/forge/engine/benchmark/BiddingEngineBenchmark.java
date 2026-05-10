package com.forge.engine.benchmark;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.bidding.BidBook;
import com.forge.engine.model.AuctionContext;
import com.forge.engine.model.AuctionStateMachine;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import com.forge.engine.tracker.PriceTracker;
import com.forge.engine.event.EventBus;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(5) // 🔥 SENSEI: Same process mein chalane ke liye 0 zaroori hai
public class BiddingEngineBenchmark {

    private BiddingEngine engine;
    private AtomicLong bidSequence;
    private Long auctionId = 101L;

    @Setup
    public void setup() {
        EventBus eventBus = new EventBus();
        engine = new BiddingEngine(eventBus);

        Money startPrice = new Money(new BigDecimal("10000.00"), "INR");
        Bid initialBid = new Bid("SYSTEM", startPrice);

        AuctionContext context = new AuctionContext(
                new PriceTracker(initialBid),
                new AuctionStateMachine(),
                new BidBook()
        );

        engine.registerAuction(auctionId, context);
        bidSequence = new AtomicLong(10000);
    }

    @Benchmark
    @Threads(8)
    public Object placeBid() {
        long amount = bidSequence.incrementAndGet();
        Bid newBid = new Bid("Sensei-" + Thread.currentThread().getId(), new Money(BigDecimal.valueOf(amount), "INR"));
        return engine.placeBid(auctionId, newBid).join();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(BiddingEngineBenchmark.class.getSimpleName())
                .forks(0) // 🔥 SENSEI FIX: Isko 1 se 0 karna mandatory tha
                .build();

        new Runner(opt).run();
    }
}