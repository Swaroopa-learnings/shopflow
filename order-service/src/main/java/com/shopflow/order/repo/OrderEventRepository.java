package com.shopflow.order.repo;

import com.shopflow.order.domain.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Event-store repository: append (save) and replay (ordered read) - nothing else. */
public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {

    List<OrderEventEntity> findByOrderIdOrderBySeqNoAsc(UUID orderId);

    int countByOrderId(UUID orderId);
}
