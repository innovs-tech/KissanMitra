package com.kissanmitra.service;

import com.kissanmitra.entity.Operator;

import java.util.List;

/**
 * Service interface for operator management.
 */
public interface OperatorService {

    /**
     * Creates a new operator.
     *
     * @param operator operator to create
     * @return created operator
     */
    Operator createOperator(Operator operator);

    /**
     * Gets operator by ID.
     *
     * @param id operator ID
     * @return operator
     */
    Operator getOperatorById(String id);

    /**
     * Gets operator by user ID.
     *
     * @param userId user ID
     * @return operator, or null if not found
     */
    Operator getOperatorByUserId(String userId);

    /**
     * Gets all active operators.
     *
     * @return list of active operators
     */
    List<Operator> getActiveOperators();

    /**
     * Updates operator.
     *
     * @param operator operator to update
     * @return updated operator
     */
    Operator updateOperator(Operator operator);
}

