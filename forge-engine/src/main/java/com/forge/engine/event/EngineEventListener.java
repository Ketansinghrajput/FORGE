package com.forge.engine.event;

/**
 * Ye ek Functional Interface है.
 * Jo bhi class engine ke events (jaise STOMP WebSocket bridge) sunna chahti hai,
 * usko ye implement karna padega.
 */
@FunctionalInterface
public interface EngineEventListener {
    void onEvent(EngineEvent event);
}