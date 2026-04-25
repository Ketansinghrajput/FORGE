package com.forge.engine.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
    // CopyOnWriteArrayList ensures thread-safety during iteration and modification
    private final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    public void subscribe(String topic, Consumer<Object> listener) {
        // computeIfAbsent is atomic. CopyOnWriteArrayList is thread-safe for reads/writes.
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void publish(String topic, Object data) {
        List<Consumer<Object>> topicListeners = listeners.get(topic);
        if (topicListeners != null) {
            for (Consumer<Object> listener : topicListeners) {
                try {
                    // Try-catch ensures one bad listener doesn't kill the whole bus
                    listener.accept(data);
                } catch (Exception e) {
                    System.err.println("❌ EventBus Error in topic [" + topic + "]: " + e.getMessage());
                }
            }
        }
    }

    // Optional: Memory clean-up ke liye method (Interviews mein impress karne ke liye)
    public void unsubscribeAll(String topic) {
        listeners.remove(topic);
    }
}