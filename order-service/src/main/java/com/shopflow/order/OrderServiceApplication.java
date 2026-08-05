package com.shopflow.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ORDER-SERVICE - the heart of the system. One request to POST /api/v1/orders
 * exercises almost every pattern in this repo:
 *
 *  HTTP in  -> RequestLoggingFilter (Servlet Filter)
 *           -> JwtAuthFilter (Spring Security)
 *           -> IdempotencyFilter (Redis-backed dedupe)
 *           -> TimingInterceptor (HandlerInterceptor)
 *           -> OrderCommandController (@Valid request validation)
 *           -> OrderCommandService: SYNC Feign call to product-service (price),
 *              event appended to the event store (EVENT SOURCING),
 *              OrderCreatedEvent published to Kafka (ASYNC)
 *           -> OrderSagaOrchestrator drives inventory -> payment (SAGA),
 *              compensating on failure (DISTRIBUTED TRANSACTIONS without 2PC)
 *           -> OrderProjection consumes the event log into a read model (CQRS)
 *
 * Meanwhile MaintenanceJobs runs @Scheduled tasks (fixedRate / fixedDelay /
 * cron) on a dedicated ThreadPoolTaskScheduler.
 */
@SpringBootApplication
@EnableFeignClients    // scan for @FeignClient interfaces (ProductClient)
@EnableScheduling      // activate @Scheduled methods (MaintenanceJobs)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
