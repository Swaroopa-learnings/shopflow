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
 * CIRCUIT BREAKER + RETRY around the flaky bank call.
 *
 * WHY: without a breaker, when the bank is down every payment thread blocks on
 * a doomed remote call. Threads pile up, THIS service dies too, then its
 * callers... - a cascading failure. The breaker fails FAST instead, giving
 * the sick dependency room to recover.
 *
 * THE STATE MACHINE (config in application.yml):
 *   CLOSED     normal; calls pass, failures are counted in a sliding window
 *      └─ failure rate >= 50% over last 10 calls -> OPEN
 *   OPEN       calls are rejected IMMEDIATELY (CallNotPermittedException) -
 *              no thread waits on the dead bank; after 10s -> HALF_OPEN
 *   HALF_OPEN  3 trial calls allowed: all good -> CLOSED, any bad -> OPEN
 *
 * ANNOTATION ORDER: @Retry wraps @CircuitBreaker here - each attempt is
 * recorded by the breaker; when the breaker opens mid-retry the retry gives
 * up immediately (CallNotPermittedException is not in retryExceptions).
 *
 * The fallbackMethod is the graceful degradation path: signature = original
 * params + the exception. Our fallback turns the failure into a proper
 * PaymentFailedEvent so the SAGA can compensate - resilience and the saga
 * pattern clicking together.
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
        // Idempotent consumer: PK = orderId; a redelivered command must not double-charge.
        if (paymentRepository.existsById(cmd.orderId())) {
            log.info("Payment for order {} already processed - re-emitting outcome (idempotent)", cmd.orderId());
            paymentRepository.findById(cmd.orderId()).ifPresent(this::reEmit);
            return;
        }

        String bankRef = bankClient.charge(cmd.orderId(), cmd.amount());   // may throw -> retry/breaker

        paymentRepository.save(Payment.captured(cmd.orderId(), cmd.userId(), cmd.amount(), bankRef));
        kafkaTemplate.send(Topics.PAYMENT_EVENTS, cmd.orderId().toString(),
                new PaymentCompletedEvent(cmd.orderId(), bankRef, cmd.amount()));
    }

    /**
     * FALLBACK - runs when retries are exhausted OR the breaker is OPEN.
     * Failing a payment is a legitimate business outcome; we record it and let
     * the saga compensate (release stock, cancel order).
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
