package com.shopflow.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Payment service: charges customers through a mock bank gateway, protected by
 * a retry and circuit breaker.
 *
 * Breaker state: http://localhost:8085/actuator/circuitbreakers
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
