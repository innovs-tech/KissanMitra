package com.kissanmitra.controller.vle;

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
 * VLE controller for order management.
 *
 * <p>Business Context:
 * - VLEs can view all RENT orders assigned to them
 * - VLEs can update order status (reject/accept)
 */
@RestController
@RequestMapping("/api/v1/vles/orders")
@RequiredArgsConstructor
public class VleOrderController {

    private final OrderService orderService;

    /**
     * Gets all RENT orders assigned to current VLE.
     *
     * @return list of RENT orders
     */
    @GetMapping("/rent")
    public BaseClientResponse<List<Order>> getRentOrders() {
        final List<Order> orders = orderService.getRentOrders();
        return Response.SUCCESS.buildSuccess(generateRequestId(), orders);
    }

    /**
     * Rejects a RENT order (VLE only).
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

