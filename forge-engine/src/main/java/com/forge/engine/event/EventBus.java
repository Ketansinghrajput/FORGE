package com.forge.engine.event;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class EventBus {

    // 1. Thread-safe list of listeners (No more String topics, we use Type checking now)
    private final List<EngineEventListener> listeners = new CopyOnWriteArrayList<>();

    // 2. The Buffer: Engine puts events here instantly.
    private final LinkedBlockingQueue<EngineEvent> eventQueue = new LinkedBlockingQueue<>();

    // 3. Java 21 Virtual Threads Pool
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public EventBus() {
        // Start the background consumer loop the moment EventBus is created
        virtualExecutor.submit(this::processEvents);
    }

    public void subscribe(EngineEventListener listener) {
        listeners.add(listener);
        log.info("New listener subscribed to EventBus");
    }

    /**
     * PRODUCER: Called by BiddingEngine.
     * Extremely fast. Just drops the event in the queue and returns.
     */
    public void publish(EngineEvent event) {
        eventQueue.offer(event);
    }

    /**
     * CONSUMER: Infinite background loop running on a Virtual Thread.
     */
    private void processEvents() {
        log.info("EventBus Background Consumer Loop Started...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // .take() blocks efficiently until an event arrives
                EngineEvent event = eventQueue.take();

                // Fire and forget: Broadcast to all listeners asynchronously
                for (EngineEventListener listener : listeners) {
                    virtualExecutor.submit(() -> {
                        try {
                            listener.onEvent(event);
                        } catch (Exception e) {
                            log.error("Listener failed to process event: {}", event, e);
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("EventBus consumer loop interrupted. Shutting down.");
                break;
            }
        }
    }
}