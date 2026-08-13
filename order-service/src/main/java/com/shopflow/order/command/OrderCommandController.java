package com.shopflow.order.command;

import com.shopflow.order.web.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Write side of the order API - commands only; reads live in OrderQueryController.
 *
 * Returns 202 Accepted because fulfillment is asynchronous: the saga decides
 * COMPLETED or CANCELLED later and the client polls for the result.
 * X-User-Id is set by the gateway from the verified JWT.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCommandController {

    private final OrderCommandService commandService;

    public OrderCommandController(OrderCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {

        UUID orderId = commandService.createOrder(userId, request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "orderId", orderId,
                "status", "PENDING",
                "message", "Order accepted; fulfillment in progress. Poll GET /api/v1/orders/" + orderId));
    }
}
