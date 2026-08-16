package com.shopflow.order.repo;

import com.shopflow.order.domain.OrderEventEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for the event store.
 *
 * Worth covering:
 *  - events belonging to another order are not returned
 *  - countByOrderId is what the next sequence number is derived from
 *  - inserting a duplicate (orderId, seqNo) violates the unique constraint -
 *    the guard against two writers appending the same position
 */
@DataJpaTest
class OrderEventRepositoryTest {

    @Autowired
    private OrderEventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void returnsAnOrdersEventsInSequenceOrderRegardlessOfInsertOrder() {
        UUID orderId = UUID.randomUUID();
        // inserted out of order on purpose - ordering must come from the query
        entityManager.persistAndFlush(new OrderEventEntity(orderId, 2, "OrderCompletedEvent", "{}"));
        entityManager.persistAndFlush(new OrderEventEntity(orderId, 1, "OrderCreatedEvent", "{}"));
        entityManager.clear();

        List<OrderEventEntity> history = eventRepository.findByOrderIdOrderBySeqNoAsc(orderId);

        assertThat(history)
                .extracting(OrderEventEntity::getEventType)
                .containsExactly("OrderCreatedEvent", "OrderCompletedEvent");
    }
}
