package com.kissanmitra.controller.admin;

import com.kissanmitra.entity.Order;
import com.kissanmitra.enums.Response;
import com.kissanmitra.request.RejectOrderRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Admin controller for order management.
 *
 * <p>Business Context:
 * - Admins can view all LEASE orders
 * - Admins can update order status (reject/accept)
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Gets all LEASE orders (for Admin).
     *
     * @return list of LEASE orders
     */
    @GetMapping("/lease")
    public BaseClientResponse<List<Order>> getLeaseOrders() {
        final List<Order> orders = orderService.getLeaseOrders();
        return Response.SUCCESS.buildSuccess(generateRequestId(), orders);
    }

    /**
     * Rejects a LEASE order (Admin only).
     *
     * @param id order ID
     * @param request rejection request (optional note)
     * @return rejected order
     */
    @PostMapping("/{id}/reject")
    public BaseClientResponse<Order> rejectOrder(
            @PathVariable final String id,
            @RequestBody(required = false) final RejectOrderRequest request
    ) {
        final String note = request != null ? request.getNote() : null;
        final Order order = orderService.rejectOrder(id, note);
        return Response.SUCCESS.buildSuccess(generateRequestId(), order);
    }
}

