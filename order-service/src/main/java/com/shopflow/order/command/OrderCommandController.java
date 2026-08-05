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
 * WRITE-SIDE controller (CQRS): accepts commands only. All reads live in
 * OrderQueryController - the separation is visible right at the API surface.
 *
 * Returns 202 ACCEPTED (not 201): the order is recorded, but fulfillment is
 * asynchronous - the saga decides COMPLETED/CANCELLED later. The client polls
 * GET /api/v1/orders/{id} to observe the outcome. This is the honest status
 * code for async processing.
 *
 * The X-User-Id header is injected by the API gateway from the verified JWT -
 * the client cannot spoof it (gateway overwrites any inbound value... in this
 * demo trust chain; a mesh would enforce it with mTLS).
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
