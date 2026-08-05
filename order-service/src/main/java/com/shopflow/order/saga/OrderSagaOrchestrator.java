package com.shopflow.order.saga;

import com.shopflow.common.events.InventoryRejectedEvent;
import com.shopflow.common.events.InventoryReservedEvent;
import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCompletedEvent;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.common.events.ProcessPaymentCommand;
import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.order.command.EventStoreService;
import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * SAGA PATTERN - orchestration variant. THE distributed-transactions answer.
 *
 * THE PROBLEM: "place an order" must update THREE services' databases (order,
 * inventory, payment). A classic ACID transaction can't span services, and
 * 2PC (two-phase commit / XA) is avoided in microservices: it holds locks
 * across the network, the coordinator is a single point of failure, and most
 * modern brokers/stores don't support it anyway.
 *
 * THE SAGA ANSWER: break the global transaction into a chain of LOCAL
 * transactions, each committed independently. If step N fails, run
 * COMPENSATING actions to semantically undo steps 1..N-1:
 *
 *   OrderCreated
 *     -> [reserve inventory]  ok -> [process payment] ok -> ORDER COMPLETED
 *            | rejected                  | failed
 *            v                           v
 *      ORDER CANCELLED       [release inventory]  <- compensation!
 *                                   -> ORDER CANCELLED
 *
 * ORCHESTRATION vs CHOREOGRAPHY (know both):
 *  - Orchestration (this class): a central coordinator sends commands and
 *    reacts to replies. + Flow is explicit and debuggable in one place.
 *    - Orchestrator is an extra component that knows about the participants.
 *  - Choreography: no coordinator; each service reacts to the previous
 *    service's events. + Fully decoupled. - The flow exists nowhere in code;
 *    understanding "what happens after payment fails" means reading N repos.
 *
 * Eventual consistency is the price: between steps, the system is briefly
 * "inconsistent" (stock reserved, money not yet taken) - and that's FINE,
 * because every intermediate state is an explicit, recoverable status.
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final EventStoreService eventStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                 EventStoreService eventStore,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ------------------------------------------------------------------
    // Replies from inventory-service
    // ------------------------------------------------------------------
    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "order-saga")
    @Transactional
    public void onInventoryReply(Object event) {
        if (event instanceof InventoryReservedEvent reserved) {
            orderRepository.findById(reserved.orderId()).ifPresent(order -> {
                // Guard: only advance from the expected state (a late/duplicate
                // reply must not resurrect a cancelled order).
                if (order.getStatus() != OrderStatus.PENDING) {
                    log.warn("Ignoring InventoryReserved for order {} in state {}",
                            order.getId(), order.getStatus());
                    return;
                }
                order.transitionTo(OrderStatus.AWAITING_PAYMENT);
                // Saga step 2: charge the customer.
                kafkaTemplate.send(Topics.PAYMENT_COMMANDS, order.getId().toString(),
                        new ProcessPaymentCommand(order.getId(), order.getUserId(), order.getTotalAmount()));
                log.info("Saga[{}]: inventory reserved -> requesting payment", order.getId());
            });

        } else if (event instanceof InventoryRejectedEvent rejected) {
            // First step failed: nothing to compensate, just cancel.
            cancelOrder(rejected.orderId(), "Inventory rejected: " + rejected.reason());
        }
    }

    // ------------------------------------------------------------------
    // Replies from payment-service
    // ------------------------------------------------------------------
    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "order-saga")
    @Transactional
    public void onPaymentReply(Object event) {
        if (event instanceof PaymentCompletedEvent completed) {
            orderRepository.findById(completed.orderId()).ifPresent(order -> {
                if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                    log.warn("Ignoring PaymentCompleted for order {} in state {}",
                            order.getId(), order.getStatus());
                    return;
                }
                order.transitionTo(OrderStatus.COMPLETED);
                eventStore.appendAndPublish(order.getId(),
                        new OrderCompletedEvent(order.getId(), order.getUserId(), Instant.now()));
                log.info("Saga[{}]: COMPLETED (payment ref {})", order.getId(), completed.paymentReference());
            });

        } else if (event instanceof PaymentFailedEvent failed) {
            // COMPENSATION: payment failed AFTER stock was reserved -> release it.
            kafkaTemplate.send(Topics.INVENTORY_COMMANDS, failed.orderId().toString(),
                    new ReleaseInventoryCommand(failed.orderId(), "payment failed"));
            cancelOrder(failed.orderId(), "Payment failed: " + failed.reason());
        }
    }

    private void cancelOrder(java.util.UUID orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
                return;   // terminal states are final
            }
            order.transitionTo(OrderStatus.CANCELLED);
            eventStore.appendAndPublish(order.getId(),
                    new OrderCancelledEvent(order.getId(), order.getUserId(), reason, Instant.now()));
            log.info("Saga[{}]: CANCELLED ({})", orderId, reason);
        });
    }
}
