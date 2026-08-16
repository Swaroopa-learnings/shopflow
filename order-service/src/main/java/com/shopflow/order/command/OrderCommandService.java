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
 * Handles the "place order" command: looks up the price, saves the order as
 * PENDING, records OrderCreatedEvent, and starts the saga.
 *
 * Everything after this point is asynchronous - the caller gets PENDING back
 * and polls the query API for the outcome.
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
        // Price comes from product-service, never from the client.
        ProductClient.ProductDto product = productClient.getProduct(request.productId());
        BigDecimal total = product.price().multiply(BigDecimal.valueOf(request.quantity()));

        UUID orderId = UUID.randomUUID();
        orderRepository.save(new OrderEntity(orderId, userId, request.productId(),
                request.quantity(), total));

        eventStore.appendAndPublish(new OrderCreatedEvent(
                orderId, userId, request.productId(), request.quantity(), total, Instant.now()));


        // Saga step 1: ask inventory-service to reserve stock.
        kafkaTemplate.send(Topics.INVENTORY_COMMANDS, orderId.toString(),
                new ReserveInventoryCommand(orderId, request.productId(), request.quantity()));
        log.info("Sent command to inventory to reserve stock");
        log.info("Order {} created for user {} (total {}), saga started", orderId, userId, total);
        return orderId;
    }
}
