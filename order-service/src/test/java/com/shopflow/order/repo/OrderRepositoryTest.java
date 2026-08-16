package com.shopflow.order.repo;

import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for the write model, against an embedded database.
 *
 * findByStatusAndCreatedAtBefore is the query worth testing, because the
 * timeout sweep depends on it and a wrong method name silently changes it.
 *
 * Worth covering:
 *  - excludes orders in that status created *after* the cutoff
 *  - excludes orders in a different status
 *  - returns empty when nothing matches
 *  - saving and reloading preserves status and total (enum stored as a string)
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsPendingOrdersCreatedBeforeTheCutoff() {
        OrderEntity order = new OrderEntity(
                UUID.randomUUID(), "user-1", "p-1001", 1, new BigDecimal("10.00"));
        entityManager.persistAndFlush(order);
        entityManager.clear();   // so the read comes from the database, not the persistence context

        List<OrderEntity> stale = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, Instant.now().plusSeconds(60));

        assertThat(stale)
                .extracting(OrderEntity::getId)
                .containsExactly(order.getId());
    }
}
