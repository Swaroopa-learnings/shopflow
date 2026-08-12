package com.shopflow.payment.messaging;

import com.shopflow.common.events.ProcessPaymentCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Async entry point. NOTE: the listener delegates to PaymentProcessor through
 * the Spring proxy - the @CircuitBreaker/@Retry aspects only apply on calls
 * that cross the bean boundary (same self-invocation rule as @Cacheable).
 * Had process() lived in THIS class and been called directly, the annotations
 * would be silently ignored.
 */
@Component
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final PaymentProcessor paymentProcessor;

    public PaymentCommandListener(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    /**
     * This topic carries only ONE command type, so a plain method-level listener
     * with a CONCRETE parameter type is enough - Spring resolves the payload
     * unambiguously. (A bare {@code Object} parameter would instead receive the
     * raw ConsumerRecord; see InventoryCommandListener for that trap.)
     */
    @KafkaListener(topics = Topics.PAYMENT_COMMANDS, groupId = "payment-service")
    public void onCommand(ProcessPaymentCommand command) {
        log.info("Received payment command for order {}", command.orderId());
        paymentProcessor.process(command);
    }
}
