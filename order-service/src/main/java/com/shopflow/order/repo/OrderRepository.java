package com.shopflow.order.repo;

import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Write-model repository (Spring Data JPA - implementation generated at startup). */
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /** Used by the scheduled stale-order sweep: derived query -
     *  "status = ? AND createdAt < ?" straight from the method name. */
    List<OrderEntity> findByStatusAndCreatedAtBefore(OrderStatus status, Instant cutoff);
}
