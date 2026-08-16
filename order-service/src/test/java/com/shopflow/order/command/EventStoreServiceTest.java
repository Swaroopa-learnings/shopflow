package com.shopflow.order.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.Topics;
import com.shopflow.order.domain.OrderEventEntity;
import com.shopflow.order.repo.OrderEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the event store.
 *
 * Worth covering:
 *  - the second event for an order is stored with seqNo 2
 *  - the payload is the event serialized as JSON
 *  - the message is published to order.events keyed by order id
 *    (the key is what keeps one order's events in a single partition)
 */
@ExtendWith(MockitoExtension.class)
class EventStoreServiceTest {

    @Mock
    private OrderEventRepository eventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    /** A real mapper rather than a mock - serialization is part of what is being tested. */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private EventStoreService eventStore;

    @BeforeEach
    void setUp() {
        eventStore = new EventStoreService(eventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void firstEventIsStoredWithSequenceOneAndPublishedKeyedByOrderId() {
        // given no events stored for this order yet
        UUID orderId = UUID.randomUUID();
        when(eventRepository.countByOrderId(orderId)).thenReturn(0);
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, "user-1", "p-1001", 1, new BigDecimal("10.00"), Instant.now());

        // when - the order id is taken from the event, not passed separately
        eventStore.appendAndPublish(event);

        // then it is appended at position 1, tagged with the type replay will look for
        ArgumentCaptor<OrderEventEntity> stored = ArgumentCaptor.forClass(OrderEventEntity.class);
        verify(eventRepository).save(stored.capture());

        assertThat(stored.getValue().getSeqNo()).isEqualTo(1);
        assertThat(stored.getValue().getEventType()).isEqualTo("OrderCreatedEvent");
        assertThat(stored.getValue().getPayload()).contains(orderId.toString());

        // and published with the order id as the key, so one order stays in one partition
        verify(kafkaTemplate).send(Topics.ORDER_EVENTS, orderId.toString(), event);
    }
}
