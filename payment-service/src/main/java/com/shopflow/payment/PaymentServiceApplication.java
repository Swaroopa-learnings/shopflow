package com.shopflow.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PAYMENT-SERVICE - saga participant that "charges the customer" by calling a
 * deliberately unreliable MockBankClient, protected by a Resilience4j
 * CIRCUIT BREAKER + RETRY (see PaymentProcessor).
 *
 * Watch the breaker live: http://localhost:8085/actuator/circuitbreakers
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
