package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.Operator;
import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.OperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for operator management.
 *
 * <p>Business Context:
 * - Only Admin can create and manage operators
 * - Operators are assigned to leases by Admin
 */
@RestController
@RequestMapping("/api/v1/admin/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorService operatorService;

    /**
     * Creates a new operator.
     *
     * @param operator operator to create
     * @return created operator
     */
    @PostMapping
    public BaseClientResponse<Operator> createOperator(@Valid @RequestBody final Operator operator) {
        final Operator created = operatorService.createOperator(operator);
        return Response.SUCCESS.buildSuccess(generateRequestId(), created);
    }

    /**
     * Gets operator by ID.
     *
     * @param id operator ID
     * @return operator
     */
    @GetMapping("/{id}")
    public BaseClientResponse<Operator> getOperator(@PathVariable final String id) {
        final Operator operator = operatorService.getOperatorById(id);
        return Response.SUCCESS.buildSuccess(generateRequestId(), operator);
    }

    /**
     * Gets all active operators.
     *
     * @return list of active operators
     */
    @GetMapping
    public BaseClientResponse<List<Operator>> getActiveOperators() {
        final List<Operator> operators = operatorService.getActiveOperators();
        return Response.SUCCESS.buildSuccess(generateRequestId(), operators);
    }

    /**
     * Updates operator.
     *
     * @param id operator ID
     * @param operator operator to update
     * @return updated operator
     */
    @PutMapping("/{id}")
    public BaseClientResponse<Operator> updateOperator(
            @PathVariable final String id,
            @Valid @RequestBody final Operator operator
    ) {
        operator.setId(id);
        final Operator updated = operatorService.updateOperator(operator);
        return Response.SUCCESS.buildSuccess(generateRequestId(), updated);
    }
}

