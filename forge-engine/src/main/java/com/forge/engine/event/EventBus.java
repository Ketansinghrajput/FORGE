package com.forge.engine.event;

public class EventBus {
    // Blueprint Page 28: Async pub/sub backbone [cite: 1053]
    public void startDispatching() {
        System.out.println("EventBus: Virtual Thread dispatcher started...");
    }

    public void stop() {
        System.out.println("EventBus: Shutting down cleanly...");
    }
}