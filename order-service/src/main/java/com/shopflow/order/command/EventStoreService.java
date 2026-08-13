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
 * Appends each domain event to the event store, then publishes it to Kafka.
 *
 * Known gap: the database write and the Kafka publish are not atomic, so a
 * crash between them stores the event without streaming it. A transactional
 * outbox would close that.
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
            // Append with the next sequence number for this order.
            int nextSeq = eventRepository.countByOrderId(orderId) + 1;
            String json = objectMapper.writeValueAsString(event);
            eventRepository.save(new OrderEventEntity(
                    orderId, nextSeq, event.getClass().getSimpleName(), json));

            // Key by orderId so one order's events share a partition and stay ordered.
            kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), event);

            log.info("Event {} (seq {}) stored + published for order {}",
                    event.getClass().getSimpleName(), nextSeq, orderId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
