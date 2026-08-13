package com.shopflow.order.scheduled;

import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.order.command.EventStoreService;
import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Background jobs for order-service, using all three @Scheduled trigger styles:
 *
 *  - fixedRate:  every N ms, measured start to start
 *  - fixedDelay: N ms after the previous run finishes, so runs never overlap
 *  - cron:       at a wall-clock time
 *
 * Spring cron takes six fields: second minute hour day-of-month month day-of-week.
 * They run on the pool defined in SchedulingConfig.
 */
@Component
public class MaintenanceJobs {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceJobs.class);

    private final OrderRepository orderRepository;
    private final EventStoreService eventStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MaintenanceJobs(OrderRepository orderRepository,
                           EventStoreService eventStore,
                           KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Logs the current order count every 60s. */
    @Scheduled(fixedRate = 60_000, initialDelay = 15_000)
    public void logOrderStats() {
        log.info("[fixedRate] order count = {} (thread: {})",
                orderRepository.count(), Thread.currentThread().getName());
    }

    /**
     * Cancels orders stuck for more than 10 minutes and releases any stock they
     * hold. Without this a saga would wait forever when a service never replies.
     * Uses fixedDelay so two sweeps can't overlap and double-cancel.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    @Transactional
    public void cancelStaleOrders() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        List<OrderEntity> stale = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);
        stale.addAll(orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.AWAITING_PAYMENT, cutoff));

        for (OrderEntity order : stale) {
            log.warn("[fixedDelay] Order {} stuck in {} since {} - cancelling (saga timeout)",
                    order.getId(), order.getStatus(), order.getCreatedAt());
            // Safe to send even if nothing was reserved - the release is a no-op then.
            kafkaTemplate.send(Topics.INVENTORY_COMMANDS, order.getId().toString(),
                    new ReleaseInventoryCommand(order.getId(),"saga timeout"));
            order.transitionTo(OrderStatus.CANCELLED);
            eventStore.appendAndPublish(order.getId(), new OrderCancelledEvent(
                    order.getId(), order.getUserId(), "saga timeout", Instant.now()));
        }
    }

    /** Daily reconciliation report at 02:00. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void dailyReconciliationReport() {
        long total = orderRepository.count();
        log.info("[cron 02:00] Daily reconciliation: {} orders on record", total);
    }
}
