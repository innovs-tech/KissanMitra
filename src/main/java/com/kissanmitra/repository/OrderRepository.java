package com.kissanmitra.repository;

import com.kissanmitra.entity.Order;
import com.kissanmitra.domain.enums.OrderStatus;
import com.kissanmitra.domain.enums.OrderType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order entity.
 */
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * Finds orders by requester and status.
     *
     * @param requestedBy requester user ID
     * @param status order status
     * @return list of orders
     */
    List<Order> findByRequestedByAndStatus(String requestedBy, OrderStatus status);

    /**
     * Finds orders by device ID.
     *
     * @param deviceId device ID
     * @return list of orders
     */
    List<Order> findByDeviceId(String deviceId);

    /**
     * Finds orders by order type and status.
     *
     * @param orderType order type
     * @param status order status
     * @return list of orders
     */
    List<Order> findByOrderTypeAndStatus(OrderType orderType, OrderStatus status);
}

