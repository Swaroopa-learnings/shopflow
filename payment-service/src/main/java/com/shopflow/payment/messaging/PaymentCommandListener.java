package com.shopflow.payment.messaging;

import com.shopflow.common.events.ProcessPaymentCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment commands and delegates to PaymentProcessor.
 *
 * The call goes through the Spring proxy, which is what makes the @Retry and
 * @CircuitBreaker annotations on the processor take effect.
 */
@Component
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final PaymentProcessor paymentProcessor;

    public PaymentCommandListener(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    /**
     * One command type on this topic, so a concrete parameter is enough for
     * Spring to resolve the payload.
     */
    @KafkaListener(topics = Topics.PAYMENT_COMMANDS, groupId = "payment-service")
    public void onCommand(ProcessPaymentCommand command) {
        log.info("Received payment command for order {}", command.orderId());
        paymentProcessor.process(command);
    }
}
