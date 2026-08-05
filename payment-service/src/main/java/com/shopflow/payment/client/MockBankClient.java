package com.shopflow.payment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in for a third-party payment gateway (Stripe/Adyen/a bank).
 * Configurably unreliable so the circuit breaker has something to break on:
 *
 *   app.bank.failure-rate=0.3   -> ~30% of calls throw BankUnavailableException
 *
 * Set it to 1.0 and place a few orders to watch the breaker OPEN in
 * /actuator/circuitbreakers, then recover through HALF_OPEN.
 */
@Component
public class MockBankClient {

    public static class BankUnavailableException extends RuntimeException {
        public BankUnavailableException(String message) {
            super(message);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MockBankClient.class);

    private final double failureRate;

    public MockBankClient(@Value("${app.bank.failure-rate:0.3}") double failureRate) {
        this.failureRate = failureRate;
    }

    /** Simulates the remote charge call: some latency, sometimes an outage. */
    public String charge(UUID orderId, BigDecimal amount) {
        sleep(ThreadLocalRandom.current().nextLong(50, 200));   // network latency

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.warn("Bank gateway TIMEOUT/ERROR for order {} (simulated)", orderId);
            throw new BankUnavailableException("bank gateway unavailable");
        }

        String reference = "BANK-" + orderId.toString().substring(0, 8).toUpperCase();
        log.info("Bank CAPTURED {} for order {} (ref {})", amount, orderId, reference);
        return reference;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
