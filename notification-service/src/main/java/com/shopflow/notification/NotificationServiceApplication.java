package com.shopflow.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NOTIFICATION-SERVICE - a pure event CONSUMER.
 *
 * It subscribes to the same ORDER_EVENTS stream the CQRS projection reads,
 * under its own consumer group - order-service doesn't know or care that
 * notifications exist. Adding this whole service required ZERO changes to any
 * producer: that is the decoupling payoff of event-driven architecture.
 *
 * Also demonstrates @Qualifier (choosing between multiple NotificationSender
 * beans) and @Async (fire-and-forget sending on a dedicated thread pool).
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
