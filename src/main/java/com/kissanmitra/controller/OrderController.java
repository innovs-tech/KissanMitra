package com.kissanmitra.controller;

import com.kissanmitra.entity.Order;
import com.kissanmitra.enums.Response;
import com.kissanmitra.request.CreateOrderRequest;
import com.kissanmitra.request.UpdateOrderStatusRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Controller for order management.
 *
 * <p>Business Context:
 * - Orders represent intent (separate from Leases)
 * - Order type automatically derived from thresholds
 * - State machine enforces valid transitions
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new order.
     *
     * @param request order creation request
     * @return created order
     */
    @PostMapping
    public BaseClientResponse<Order> createOrder(@Valid @RequestBody final CreateOrderRequest request) {
        final Order order = orderService.createOrder(request);
        return Response.SUCCESS.buildSuccess(generateRequestId(), order);
    }

    /**
     * Gets order by ID.
     *
     * @param id order ID
     * @return order
     */
    @GetMapping("/{id}")
    public BaseClientResponse<Order> getOrder(@PathVariable final String id) {
        final Order order = orderService.getOrderById(id);
        return Response.SUCCESS.buildSuccess(generateRequestId(), order);
    }

    /**
     * Gets orders for current user.
     *
     * @return list of orders
     */
    @GetMapping
    public BaseClientResponse<List<Order>> getMyOrders() {
        final List<Order> orders = orderService.getMyOrders();
        return Response.SUCCESS.buildSuccess(generateRequestId(), orders);
    }

    /**
     * Updates order status.
     *
     * @param id order ID
     * @param request status update request
     * @return updated order
     */
    @PatchMapping("/{id}/status")
    public BaseClientResponse<Order> updateOrderStatus(
            @PathVariable final String id,
            @Valid @RequestBody final UpdateOrderStatusRequest request
    ) {
        final Order order = orderService.updateOrderStatus(id, request);
        return Response.SUCCESS.buildSuccess(generateRequestId(), order);
    }
}

