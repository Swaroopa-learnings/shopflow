package com.shopflow.payment.messaging;

import com.shopflow.common.events.ProcessPaymentCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.payment.service.PaymentProcessor;
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

    private final PaymentProcessor paymentProcessor;

    public PaymentCommandListener(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMMANDS, groupId = "payment-service")
    public void onCommand(Object command) {
        if (command instanceof ProcessPaymentCommand cmd) {
            paymentProcessor.process(cmd);
        }
    }
}
