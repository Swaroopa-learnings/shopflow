package com.shopflow.order.repo;

import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.domain.OrderSummaryEntity;
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
 * Persistence tests for the read model.
 *
 * Worth covering:
 *  - results are newest first
 *  - an unknown user id returns an empty list
 *  - updateStatus persists both the status and the reason
 */
@DataJpaTest
class OrderSummaryRepositoryTest {

    @Autowired
    private OrderSummaryRepository summaryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void returnsOnlyTheGivenUsersOrders() {
        OrderSummaryEntity mine = new OrderSummaryEntity(
                UUID.randomUUID(), "user-1", "p-1001", 1,
                new BigDecimal("10.00"), OrderStatus.PENDING, Instant.now());
        OrderSummaryEntity someoneElses = new OrderSummaryEntity(
                UUID.randomUUID(), "user-2", "p-1002", 1,
                new BigDecimal("20.00"), OrderStatus.PENDING, Instant.now());
        entityManager.persistAndFlush(mine);
        entityManager.persistAndFlush(someoneElses);
        entityManager.clear();

        List<OrderSummaryEntity> result = summaryRepository.findByUserIdOrderByCreatedAtDesc("user-1");

        assertThat(result)
                .extracting(OrderSummaryEntity::getOrderId)
                .containsExactly(mine.getOrderId());
    }
}
