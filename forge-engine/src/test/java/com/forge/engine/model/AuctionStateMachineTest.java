package com.forge.engine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuctionStateMachineTest {

    private AuctionStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new AuctionStateMachine();
    }

    @Test
    void shouldStartInDraftState() {
        assertEquals(AuctionState.DRAFT, stateMachine.getCurrentState());
    }

    @Test
    void shouldTransitionFromDraftToActive() {
        assertTrue(stateMachine.transitionTo(AuctionState.ACTIVE));
        assertEquals(AuctionState.ACTIVE, stateMachine.getCurrentState());
    }

    @Test
    void shouldTransitionFromDraftToCancelled() {
        assertTrue(stateMachine.transitionTo(AuctionState.CANCELLED));
        assertEquals(AuctionState.CANCELLED, stateMachine.getCurrentState());
    }

    @Test
    void shouldTransitionFromActiveToEnded() {
        stateMachine.transitionTo(AuctionState.ACTIVE);
        assertTrue(stateMachine.transitionTo(AuctionState.ENDED));
        assertEquals(AuctionState.ENDED, stateMachine.getCurrentState());
    }

    @Test
    void shouldRejectInvalidTransitionFromEndedToActive() {
        stateMachine.transitionTo(AuctionState.ACTIVE);
        stateMachine.transitionTo(AuctionState.ENDED);
        assertFalse(stateMachine.transitionTo(AuctionState.ACTIVE));
    }

    @Test
    void shouldRejectInvalidTransitionFromCancelledToActive() {
        stateMachine.transitionTo(AuctionState.CANCELLED);
        assertFalse(stateMachine.transitionTo(AuctionState.ACTIVE));
    }

    @Test
    void shouldBeActiveOnlyInActiveState() {
        assertFalse(stateMachine.isActive());
        stateMachine.transitionTo(AuctionState.ACTIVE);
        assertTrue(stateMachine.isActive());
        stateMachine.transitionTo(AuctionState.ENDED);
        assertFalse(stateMachine.isActive());
    }
}