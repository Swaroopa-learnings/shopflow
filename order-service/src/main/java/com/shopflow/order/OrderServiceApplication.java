package com.shopflow.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order service: accepts orders, records them as an event stream, and
 * orchestrates the saga across inventory- and payment-service.
 *
 * Reads are served from a separate read model built by OrderProjection.
 */
@SpringBootApplication
@EnableFeignClients    // picks up ProductClient
@EnableScheduling      // enables MaintenanceJobs
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
