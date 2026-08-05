package com.shopflow.order.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.domain.OrderEventEntity;
import com.shopflow.order.domain.OrderSummaryEntity;
import com.shopflow.order.repo.OrderEventRepository;
import com.shopflow.order.repo.OrderSummaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * READ-SIDE controller (CQRS): serves ONLY the read model - it never touches
 * the write-model tables. Note there is no service layer here: the query side
 * is allowed to be thin because it contains no business rules at all.
 *
 * Also exposes two event-sourcing "show off" endpoints:
 *   /{id}/events   - the raw immutable history (audit log for free)
 *   /{id}/rebuilt  - current state derived by REPLAYING events through
 *                    OrderAggregate.replay(), proving state == fold(events).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderQueryController {

    private final OrderSummaryRepository summaryRepository;
    private final OrderEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public OrderQueryController(OrderSummaryRepository summaryRepository,
                                OrderEventRepository eventRepository,
                                ObjectMapper objectMapper) {
        this.summaryRepository = summaryRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderSummaryEntity> getOrder(@PathVariable UUID id) {
        return summaryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** "My orders" - the identity comes from the JWT via the gateway, never from a query param. */
    @GetMapping
    public List<OrderSummaryEntity> myOrders(@RequestHeader("X-User-Id") String userId) {
        return summaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Raw event stream - the audit trail event sourcing gives you for free. */
    @GetMapping("/{id}/events")
    public List<Map<String, Object>> getEvents(@PathVariable UUID id) {
        return eventRepository.findByOrderIdOrderBySeqNoAsc(id).stream()
                .map(e -> Map.<String, Object>of(
                        "seqNo", e.getSeqNo(),
                        "eventType", e.getEventType(),
                        "payload", parse(e),
                        "occurredAt", e.getOccurredAt().toString()))
                .toList();
    }

    /** State REBUILT purely from events - event sourcing's party trick. */
    @GetMapping("/{id}/rebuilt")
    public ResponseEntity<OrderAggregate> rebuild(@PathVariable UUID id) {
        List<OrderEventEntity> events = eventRepository.findByOrderIdOrderBySeqNoAsc(id);
        if (events.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(OrderAggregate.replay(events, objectMapper));
    }

    private Object parse(OrderEventEntity e) {
        try {
            return objectMapper.readTree(e.getPayload());
        } catch (Exception ex) {
            return e.getPayload();   // fall back to the raw string
        }
    }
}
