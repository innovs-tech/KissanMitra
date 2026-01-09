package com.kissanmitra.service;

import com.kissanmitra.domain.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Order state machine for validating state transitions.
 *
 * <p>Business Context:
 * - Orders follow a deterministic lifecycle
 * - Only valid transitions are allowed
 * - State transitions are validated server-side
 *
 * <p>Uber Logic:
 * - Defines allowed transitions for each state
 * - Prevents invalid state jumps
 * - Supports both LEASE and RENT order types
 */
@Component
public final class OrderStateMachine {

    /**
     * Map of valid state transitions.
     * Key: current state, Value: set of allowed next states
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.DRAFT, Set.of(OrderStatus.INTEREST_RAISED),

            OrderStatus.INTEREST_RAISED, Set.of(
                    OrderStatus.UNDER_REVIEW,
                    OrderStatus.ACCEPTED,
                    OrderStatus.REJECTED,
                    OrderStatus.CANCELLED
            ),

            OrderStatus.UNDER_REVIEW, Set.of(
                    OrderStatus.ACCEPTED,
                    OrderStatus.REJECTED
            ),

            OrderStatus.ACCEPTED, Set.of(
                    OrderStatus.PICKUP_SCHEDULED
            ),

            OrderStatus.PICKUP_SCHEDULED, Set.of(
                    OrderStatus.ACTIVE
            ),

            OrderStatus.ACTIVE, Set.of(
                    OrderStatus.COMPLETED
            ),

            OrderStatus.COMPLETED, Set.of(
                    OrderStatus.CLOSED
            )
    );

    /**
     * Checks if a state transition is valid.
     *
     * @param from current state
     * @param to target state
     * @return true if transition is valid, false otherwise
     */
    public static boolean canTransition(final OrderStatus from, final OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }

        // Terminal states cannot transition
        if (from == OrderStatus.CLOSED || from == OrderStatus.REJECTED || from == OrderStatus.CANCELLED) {
            return false;
        }

        // Same state is not a transition
        if (from == to) {
            return false;
        }

        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Gets all allowed next states for a given current state.
     *
     * @param currentState current state
     * @return set of allowed next states
     */
    public static Set<OrderStatus> getAllowedNextStates(final OrderStatus currentState) {
        return TRANSITIONS.getOrDefault(currentState, Set.of());
    }
}

