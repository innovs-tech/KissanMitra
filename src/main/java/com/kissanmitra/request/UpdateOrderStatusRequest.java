package com.kissanmitra.request;

import com.kissanmitra.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for updating order status.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    /**
     * Target status.
     */
    @NotNull(message = "Status is required")
    private OrderStatus toState;

    /**
     * Optional note for the status change.
     */
    private String note;
}

