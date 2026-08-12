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
 * SCHEDULED TASKS - all three trigger styles, side by side.
 *
 *  - fixedRate:  "start every N ms" measured start-to-START. If a run takes
 *                longer than the period, the next run fires immediately after
 *                (runs can pile up on a busy pool).
 *  - fixedDelay: "start N ms after the previous run FINISHES" - guarantees a
 *                gap; the right choice when runs must never overlap.
 *  - cron:       calendar-based expression, for "at a wall-clock time" jobs.
 *
 * CRON EXPRESSION anatomy (Spring uses SIX fields - the leading seconds field
 * is the difference from classic 5-field Unix cron):
 *
 *      "0 0 2 * * *"
 *       │ │ │ │ │ └─ day of week (MON-SUN or 0-7; * = any)
 *       │ │ │ │ └─── month (1-12)
 *       │ │ │ └───── day of month (1-31)
 *       │ │ └─────── hour (2 = 02:00)
 *       │ └───────── minute
 *       └─────────── second
 *
 *      more examples: "0 *&#47;15 9-17 * * MON-FRI" = every 15 min, 9am-5pm, weekdays
 *                     "0 0 0 1 * *"                = midnight on the 1st of each month
 *
 * These methods run on the 4-thread pool from SchedulingConfig (watch the
 * "sched-N" thread names in the logs).
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

    /** FIXED RATE: lightweight heartbeat/metrics every 60s, aligned start-to-start. */
    @Scheduled(fixedRate = 60_000, initialDelay = 15_000)
    public void logOrderStats() {
        log.info("[fixedRate] order count = {} (thread: {})",
                orderRepository.count(), Thread.currentThread().getName());
    }

    /**
     * FIXED DELAY: the SAGA TIMEOUT sweep - this job is load-bearing, not a toy!
     *
     * If inventory- or payment-service never replies (crashed, Kafka issue),
     * an order would sit PENDING/AWAITING_PAYMENT forever. Sagas therefore
     * need a TIMEOUT path: this sweep cancels orders stuck >10 minutes and
     * releases any stock they may hold. fixedDelay (not fixedRate) because two
     * overlapping sweeps could double-cancel.
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
            // Compensate defensively: releasing a non-existent reservation is a no-op.
            kafkaTemplate.send(Topics.INVENTORY_COMMANDS, order.getId().toString(),
                    new ReleaseInventoryCommand(order.getId(),"saga timeout"));
            order.transitionTo(OrderStatus.CANCELLED);
            eventStore.appendAndPublish(order.getId(), new OrderCancelledEvent(
                    order.getId(), order.getUserId(), "saga timeout", Instant.now()));
        }
    }

    /** CRON: daily reconciliation report at 02:00 (quiet hours - typical for batch). */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void dailyReconciliationReport() {
        long total = orderRepository.count();
        log.info("[cron 02:00] Daily reconciliation: {} orders on record. "
                + "(In a real shop: compare captured payments vs completed orders, alert on drift.)", total);
    }
}
