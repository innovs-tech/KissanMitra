package com.kissanmitra.service;

import com.kissanmitra.domain.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrderStateMachine.
 */
class OrderStateMachineTest {

    @Test
    void testValidTransitions() {
        // DRAFT -> INTEREST_RAISED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DRAFT, OrderStatus.INTEREST_RAISED));

        // INTEREST_RAISED -> UNDER_REVIEW
        assertTrue(OrderStateMachine.canTransition(OrderStatus.INTEREST_RAISED, OrderStatus.UNDER_REVIEW));

        // INTEREST_RAISED -> ACCEPTED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.INTEREST_RAISED, OrderStatus.ACCEPTED));

        // INTEREST_RAISED -> REJECTED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.INTEREST_RAISED, OrderStatus.REJECTED));

        // INTEREST_RAISED -> CANCELLED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.INTEREST_RAISED, OrderStatus.CANCELLED));

        // UNDER_REVIEW -> ACCEPTED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.UNDER_REVIEW, OrderStatus.ACCEPTED));

        // UNDER_REVIEW -> REJECTED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.UNDER_REVIEW, OrderStatus.REJECTED));

        // ACCEPTED -> PICKUP_SCHEDULED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.ACCEPTED, OrderStatus.PICKUP_SCHEDULED));

        // PICKUP_SCHEDULED -> ACTIVE
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PICKUP_SCHEDULED, OrderStatus.ACTIVE));

        // ACTIVE -> COMPLETED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.ACTIVE, OrderStatus.COMPLETED));

        // COMPLETED -> CLOSED
        assertTrue(OrderStateMachine.canTransition(OrderStatus.COMPLETED, OrderStatus.CLOSED));
    }

    @Test
    void testInvalidTransitions() {
        // DRAFT -> ACTIVE (invalid jump)
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DRAFT, OrderStatus.ACTIVE));

        // INTEREST_RAISED -> CLOSED (invalid jump)
        assertFalse(OrderStateMachine.canTransition(OrderStatus.INTEREST_RAISED, OrderStatus.CLOSED));

        // CLOSED -> any state (terminal state)
        assertFalse(OrderStateMachine.canTransition(OrderStatus.CLOSED, OrderStatus.ACTIVE));

        // REJECTED -> any state (terminal state)
        assertFalse(OrderStateMachine.canTransition(OrderStatus.REJECTED, OrderStatus.ACCEPTED));

        // CANCELLED -> any state (terminal state)
        assertFalse(OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.ACCEPTED));

        // Same state (not a transition)
        assertFalse(OrderStateMachine.canTransition(OrderStatus.ACTIVE, OrderStatus.ACTIVE));
    }

    @Test
    void testNullStates() {
        assertFalse(OrderStateMachine.canTransition(null, OrderStatus.ACTIVE));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.ACTIVE, null));
        assertFalse(OrderStateMachine.canTransition(null, null));
    }

    @Test
    void testGetAllowedNextStates() {
        final Set<OrderStatus> draftNext = OrderStateMachine.getAllowedNextStates(OrderStatus.DRAFT);
        assertEquals(1, draftNext.size());
        assertTrue(draftNext.contains(OrderStatus.INTEREST_RAISED));

        final Set<OrderStatus> interestRaisedNext = OrderStateMachine.getAllowedNextStates(OrderStatus.INTEREST_RAISED);
        assertEquals(4, interestRaisedNext.size());
        assertTrue(interestRaisedNext.contains(OrderStatus.UNDER_REVIEW));
        assertTrue(interestRaisedNext.contains(OrderStatus.ACCEPTED));
        assertTrue(interestRaisedNext.contains(OrderStatus.REJECTED));
        assertTrue(interestRaisedNext.contains(OrderStatus.CANCELLED));

        final Set<OrderStatus> underReviewNext = OrderStateMachine.getAllowedNextStates(OrderStatus.UNDER_REVIEW);
        assertEquals(2, underReviewNext.size());
        assertTrue(underReviewNext.contains(OrderStatus.ACCEPTED));
        assertTrue(underReviewNext.contains(OrderStatus.REJECTED));

        final Set<OrderStatus> closedNext = OrderStateMachine.getAllowedNextStates(OrderStatus.CLOSED);
        assertTrue(closedNext.isEmpty());
    }
}

