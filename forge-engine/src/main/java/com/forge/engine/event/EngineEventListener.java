package com.forge.engine.event;


@FunctionalInterface
public interface EngineEventListener {
    void onEvent(EngineEvent event);
}