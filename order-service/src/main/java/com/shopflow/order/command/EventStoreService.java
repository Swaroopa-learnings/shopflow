package com.shopflow.order.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.events.Topics;
import com.shopflow.order.domain.OrderEventEntity;
import com.shopflow.order.repo.OrderEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * EVENT SOURCING glue: every domain event is
 *   (1) APPENDED to the relational event store (durable history), then
 *   (2) PUBLISHED to the Kafka ORDER_EVENTS topic (live stream for the
 *       CQRS projection, notification-service, and any future consumer).
 *
 * HONEST LIMITATION (great interview talking point): DB append and Kafka
 * publish are NOT atomic here - if the process dies between the two, the
 * event is stored but never streamed ("dual-write problem"). The production
 * fix is the TRANSACTIONAL OUTBOX pattern: write the event into an outbox
 * table INSIDE the same DB transaction, and let a relay (e.g. Debezium CDC)
 * publish it to Kafka afterwards - at-least-once, no distributed transaction.
 */
@Service
public class EventStoreService {

    private static final Logger log = LoggerFactory.getLogger(EventStoreService.class);

    private final OrderEventRepository eventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventStoreService(OrderEventRepository eventRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void appendAndPublish(UUID orderId, Object event) {
        try {
            // 1. Append to the event store with the next sequence number.
            int nextSeq = eventRepository.countByOrderId(orderId) + 1;
            String json = objectMapper.writeValueAsString(event);
            eventRepository.save(new OrderEventEntity(
                    orderId, nextSeq, event.getClass().getSimpleName(), json));

            // 2. Publish to Kafka. KEY = orderId so all events of one order land
            //    in the SAME partition => consumers see them IN ORDER. (Kafka
            //    only guarantees ordering within a partition.)
            kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), event);

            log.info("Event {} (seq {}) stored + published for order {}",
                    event.getClass().getSimpleName(), nextSeq, orderId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
