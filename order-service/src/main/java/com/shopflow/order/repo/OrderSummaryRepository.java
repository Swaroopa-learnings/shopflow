package com.shopflow.order.repo;

import com.shopflow.order.domain.OrderSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Read-model repository: read by the query API, written by the projection. */
public interface OrderSummaryRepository extends JpaRepository<OrderSummaryEntity, UUID> {

    List<OrderSummaryEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
