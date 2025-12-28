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
}

