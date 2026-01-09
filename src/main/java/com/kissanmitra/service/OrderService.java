package com.kissanmitra.service;

import com.kissanmitra.entity.Order;
import com.kissanmitra.request.CreateOrderRequest;
import com.kissanmitra.request.UpdateOrderStatusRequest;

import java.util.List;

/**
 * Service interface for order management.
 */
public interface OrderService {

    /**
     * Creates a new order.
     *
     * @param request order creation request
     * @return created order
     */
    Order createOrder(CreateOrderRequest request);

    /**
     * Gets order by ID.
     *
     * @param id order ID
     * @return order
     */
    Order getOrderById(String id);

    /**
     * Gets orders for current user.
     *
     * @return list of orders
     */
    List<Order> getMyOrders();

    /**
     * Updates order status.
     *
     * @param id order ID
     * @param request status update request
     * @return updated order
     */
    Order updateOrderStatus(String id, UpdateOrderStatusRequest request);

    /**
     * Cancels an order by requester.
     *
     * @param id order ID
     * @param note cancellation note
     * @return cancelled order
     */
    Order cancelOrder(String id, String note);

    /**
     * Rejects an order by handler (Admin for LEASE, VLE for RENT).
     *
     * @param id order ID
     * @param note rejection note
     * @return rejected order
     */
    Order rejectOrder(String id, String note);

    /**
     * Gets all LEASE orders (for Admin).
     *
     * @return list of LEASE orders
     */
    List<Order> getLeaseOrders();

    /**
     * Gets all RENT orders assigned to current VLE.
     *
     * @return list of RENT orders
     */
    List<Order> getRentOrders();
}

