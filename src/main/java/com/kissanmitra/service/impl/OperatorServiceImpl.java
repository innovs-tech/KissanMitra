package com.kissanmitra.service.impl;

import com.kissanmitra.entity.Operator;
import com.kissanmitra.repository.OperatorRepository;
import com.kissanmitra.service.OperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for operator management.
 *
 * <p>Business Context:
 * - Operators are managed only by Admin
 * - Operators are assigned to leases, not devices
 * - Only ACTIVE operators can be assigned
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorServiceImpl implements OperatorService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final OperatorRepository operatorRepository;

    /**
     * Creates a new operator.
     *
     * @param operator operator to create
     * @return created operator
     */
    @Override
    public Operator createOperator(final Operator operator) {
        // BUSINESS DECISION: Validate userId uniqueness
        if (operatorRepository.findByUserId(operator.getUserId()).isPresent()) {
            throw new RuntimeException("Operator already exists for this user");
        }

        final Operator saved = operatorRepository.save(operator);
        log.info("Created operator: {} for user: {}", saved.getId(), saved.getUserId());
        return saved;
    }

    /**
     * Gets operator by ID.
     *
     * @param id operator ID
     * @return operator
     */
    @Override
    public Operator getOperatorById(final String id) {
        return operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found"));
    }

    /**
     * Gets operator by user ID.
     *
     * @param userId user ID
     * @return operator, or null if not found
     */
    @Override
    public Operator getOperatorByUserId(final String userId) {
        return operatorRepository.findByUserId(userId)
                .orElse(null);
    }

    /**
     * Gets all active operators.
     *
     * @return list of active operators
     */
    @Override
    public List<Operator> getActiveOperators() {
        return operatorRepository.findByStatus(ACTIVE_STATUS);
    }

    /**
     * Updates operator.
     *
     * @param operator operator to update
     * @return updated operator
     */
    @Override
    public Operator updateOperator(final Operator operator) {
        if (operator.getId() == null) {
            throw new RuntimeException("Operator ID is required for update");
        }

        final Operator existing = operatorRepository.findById(operator.getId())
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        final Operator updated = existing.toBuilder()
                .status(operator.getStatus() != null ? operator.getStatus() : existing.getStatus())
                .training(operator.getTraining() != null ? operator.getTraining() : existing.getTraining())
                .build();

        return operatorRepository.save(updated);
    }
}

