package com.shopflow.inventory.service;

import com.shopflow.common.events.InventoryRejectedEvent;
import com.shopflow.common.events.InventoryReservedEvent;
import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.ReserveInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.inventory.domain.InventoryItem;
import com.shopflow.inventory.domain.Reservation;
import com.shopflow.inventory.repo.InventoryRepository;
import com.shopflow.inventory.repo.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The participant's local transaction. Each method is one ACID unit of work on
 * THIS service's database - the saga chains these local transactions together.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryService(InventoryRepository inventoryRepository,
                            ReservationRepository reservationRepository,
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void reserve(ReserveInventoryCommand cmd) {
        // IDEMPOTENCY CHECK: PK = orderId. A redelivered command finds the
        // existing reservation and re-sends the (idempotent) success reply
        // WITHOUT touching stock a second time.
        if (reservationRepository.existsById(cmd.orderId())) {
            log.info("Duplicate reserve command for order {} - replying reserved again (idempotent)", cmd.orderId());
            reply(new InventoryReservedEvent(cmd.orderId(), cmd.productId(), cmd.quantity()));
            return;
        }

        InventoryItem item = inventoryRepository.findById(cmd.productId()).orElse(null);
        if (item == null || !item.tryReserve(cmd.quantity())) {
            String reason = item == null
                    ? "unknown product " + cmd.productId()
                    : "insufficient stock (available: " + item.getAvailable() + ", wanted: " + cmd.quantity() + ")";
            log.warn("Reservation REJECTED for order {}: {}", cmd.orderId(), reason);
            reply(new InventoryRejectedEvent(cmd.orderId(), reason));
            return;
        }

        reservationRepository.save(new Reservation(cmd.orderId(), cmd.productId(), cmd.quantity()));
        log.info("Reserved {} x {} for order {} ({} left)",
                cmd.quantity(), cmd.productId(), cmd.orderId(), item.getAvailable());
        reply(new InventoryReservedEvent(cmd.orderId(), cmd.productId(), cmd.quantity()));
    }

    /** COMPENSATION: undo a reservation (payment failed or saga timed out). */
    @Transactional
    public void release(ReleaseInventoryCommand cmd) {
        reservationRepository.findById(cmd.orderId()).ifPresentOrElse(reservation -> {
            if (reservation.isReleased()) {
                log.info("Reservation for order {} already released (idempotent no-op)", cmd.orderId());
                return;
            }
            inventoryRepository.findById(reservation.getProductId())
                    .ifPresent(item -> item.release(reservation.getQuantity()));
            reservation.markReleased();
            log.info("Released {} x {} for order {} (reason: {})",
                    reservation.getQuantity(), reservation.getProductId(), cmd.orderId(), cmd.reason());
        }, () ->
            // Nothing was reserved (e.g. timeout fired before the reserve command
            // arrived). Releasing nothing is a valid no-op - compensations must
            // tolerate being called for work that never happened.
            log.info("Release for order {}: no reservation found - no-op", cmd.orderId())
        );
    }

    private void reply(Object event) {
        kafkaTemplate.send(Topics.INVENTORY_EVENTS, event);
    }
}
