package com.shopflow.order.command;

import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.ReserveInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.order.client.ProductClient;
import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.repo.OrderRepository;
import com.shopflow.order.web.dto.CreateOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * COMMAND HANDLER (CQRS write side) - the "place order" use-case.
 *
 * Sequence:
 *  1. SYNC call to product-service (Feign) - validate the product & get price.
 *     Price is computed server-side; never trust an amount sent by the client.
 *  2. Persist the PENDING order (write model) - local ACID transaction.
 *  3. Append + publish OrderCreatedEvent (event sourcing).
 *  4. Send ReserveInventoryCommand - the saga's first step. From here on the
 *     flow is fully asynchronous; the HTTP response returns 202-style
 *     "PENDING" immediately and the client polls the query API for the outcome.
 */
@Service
public class OrderCommandService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);

    private final OrderRepository orderRepository;
    private final EventStoreService eventStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductClient productClient;

    public OrderCommandService(OrderRepository orderRepository,
                               EventStoreService eventStore,
                               KafkaTemplate<String, Object> kafkaTemplate,
                               ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.productClient = productClient;
    }

    @Transactional
    public UUID createOrder(String userId, CreateOrderRequest request) {
        // (1) sync price lookup - fails fast with 404->exception if product unknown
        ProductClient.ProductDto product = productClient.getProduct(request.productId());
        BigDecimal total = product.price().multiply(BigDecimal.valueOf(request.quantity()));

        // (2) write model: local transaction, PENDING state
        UUID orderId = UUID.randomUUID();
        orderRepository.save(new OrderEntity(orderId, userId, request.productId(),
                request.quantity(), total));

        // (3) event sourcing: fact recorded + streamed
        eventStore.appendAndPublish(orderId, new OrderCreatedEvent(
                orderId, userId, request.productId(), request.quantity(), total, Instant.now()));

        // (4) saga step 1: ask inventory-service to reserve stock (async, Kafka)
        kafkaTemplate.send(Topics.INVENTORY_COMMANDS, orderId.toString(),
                new ReserveInventoryCommand(orderId, request.productId(), request.quantity()));

        log.info("Order {} created for user {} (total {}), saga started", orderId, userId, total);
        return orderId;
    }
}
