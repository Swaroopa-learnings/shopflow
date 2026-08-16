package com.shopflow.order.command;

import com.shopflow.order.client.ProductClient;
import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.repo.OrderRepository;
import com.shopflow.order.web.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the "place order" command handler.
 *
 * Worth covering:
 *  - saves the order as PENDING
 *  - total = product price x quantity (price comes from product-service, not the request)
 *  - appends an OrderCreatedEvent
 *  - sends a ReserveInventoryCommand keyed by order id
 *  - a failure from product-service propagates and nothing is saved
 */
@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventStoreService eventStore;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderCommandService commandService;

    @Test
    void savesOrderAsPendingWithTotalTakenFromProductService() {
        // given a product priced at 100.00 and an order for 2 of them
        when(productClient.getProduct("p-1001"))
                .thenReturn(new ProductClient.ProductDto("p-1001", "ThinkBook 14", new BigDecimal("100.00")));

        // when
        UUID orderId = commandService.createOrder("user-1", new CreateOrderRequest("p-1001", 2, "USD"));

        // then the saved order carries the server-calculated total, not anything from the request
        ArgumentCaptor<OrderEntity> saved = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(saved.capture());

        assertThat(saved.getValue().getId()).isEqualTo(orderId);
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("200.00");
    }
}
