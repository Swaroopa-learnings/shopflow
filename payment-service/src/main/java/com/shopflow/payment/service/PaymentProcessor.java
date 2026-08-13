package com.shopflow.payment.service;

import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.common.events.ProcessPaymentCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.payment.client.MockBankClient;
import com.shopflow.payment.domain.Payment;
import com.shopflow.payment.repo.PaymentRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Charges the customer through the bank gateway, wrapped in a retry and a
 * circuit breaker so a failing gateway doesn't tie up threads here.
 *
 * The breaker opens after too many failures and rejects calls immediately for
 * a while, then lets a few through to test recovery. Thresholds are in
 * application.yml. When calls fail or the breaker is open, the fallback
 * publishes a PaymentFailedEvent so the saga can compensate.
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final MockBankClient bankClient;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentProcessor(MockBankClient bankClient,
                            PaymentRepository paymentRepository,
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.bankClient = bankClient;
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Retry(name = "bankGateway")
    @CircuitBreaker(name = "bankGateway", fallbackMethod = "paymentFallback")
    public void process(ProcessPaymentCommand cmd) {
        // Order id is the primary key, so a redelivered command can't double-charge.
        if (paymentRepository.existsById(cmd.orderId())) {
            log.info("Payment for order {} already processed - re-emitting outcome", cmd.orderId());
            paymentRepository.findById(cmd.orderId()).ifPresent(this::reEmit);
            return;
        }

        String bankRef = bankClient.charge(cmd.orderId(), cmd.amount());

        paymentRepository.save(Payment.captured(cmd.orderId(), cmd.userId(), cmd.amount(), bankRef));
        kafkaTemplate.send(Topics.PAYMENT_EVENTS, cmd.orderId().toString(),
                new PaymentCompletedEvent(cmd.orderId(), bankRef, cmd.amount()));
    }

    /**
     * Runs when retries are exhausted or the breaker is open. Records the
     * failure and lets the saga compensate.
     */
    @SuppressWarnings("unused") // invoked reflectively by Resilience4j
    private void paymentFallback(ProcessPaymentCommand cmd, Throwable cause) {
        String reason = cause instanceof CallNotPermittedException
                ? "bank gateway circuit OPEN (failing fast)"
                : "bank gateway error: " + cause.getMessage();
        log.warn("Payment FAILED for order {}: {}", cmd.orderId(), reason);

        if (!paymentRepository.existsById(cmd.orderId())) {
            paymentRepository.save(Payment.failed(cmd.orderId(), cmd.userId(), cmd.amount(), reason));
        }
        kafkaTemplate.send(Topics.PAYMENT_EVENTS, cmd.orderId().toString(),
                new PaymentFailedEvent(cmd.orderId(), reason));
    }

    private void reEmit(Payment payment) {
        if ("CAPTURED".equals(payment.getStatus())) {
            kafkaTemplate.send(Topics.PAYMENT_EVENTS, payment.getOrderId().toString(),
                    new PaymentCompletedEvent(payment.getOrderId(), payment.getBankReference(), payment.getAmount()));
        } else {
            kafkaTemplate.send(Topics.PAYMENT_EVENTS, payment.getOrderId().toString(),
                    new PaymentFailedEvent(payment.getOrderId(), payment.getFailureReason()));
        }
    }
}
