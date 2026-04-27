package com.forge.engine.model; // Package apne hisaab se adjust kar lena

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuctionStateMachine {

    private AuctionState currentState;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public AuctionStateMachine() {
        // Default state jab auction create hota hai
        this.currentState = AuctionState.DRAFT;
    }


    public boolean isActive() {
        lock.readLock().lock();
        try {
            return this.currentState == AuctionState.ACTIVE;
        } finally {
            lock.readLock().unlock();
        }
    }

    public AuctionState getCurrentState() {
        lock.readLock().lock();
        try {
            return this.currentState;
        } finally {
            lock.readLock().unlock();
        }
    }


    public boolean transitionTo(AuctionState newState) {
        lock.writeLock().lock();
        try {
            if (isValidTransition(this.currentState, newState)) {
                this.currentState = newState;
                return true;
            }
            return false; // Transition reject ho gaya (e.g., ENDED se wapas ACTIVE jana)
        } finally {
            lock.writeLock().unlock();
        }
    }
    // Yeh method AuctionStateMachine.java mein add kar
    public void transitionToEnded() {
        boolean success = transitionTo(AuctionState.ENDED);
        if (!success) {
            throw new IllegalStateException("Cannot transition to ENDED from current state.");
        }
    }

    // Strict validation taaki engine galat state mein na chala jaye
    private boolean isValidTransition(AuctionState current, AuctionState target) {
        return switch (current) {
            case DRAFT -> target == AuctionState.ACTIVE || target == AuctionState.CANCELLED;
            case ACTIVE -> target == AuctionState.ENDED || target == AuctionState.CANCELLED;
            case ENDED, CANCELLED -> false; // Terminal states, yahan se aage kuch nahi
        };
    }
}