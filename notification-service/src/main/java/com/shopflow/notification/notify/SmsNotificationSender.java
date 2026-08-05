package com.shopflow.notification.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** The second NotificationSender implementation - see NotificationDispatcher for the @Qualifier wiring. */
@Component("smsSender")
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Async("notificationExecutor")
    @Override
    public void send(String userId, String subject, String body) {
        log.info("SMS to user {}: {} (sent on thread {})",
                userId, body, Thread.currentThread().getName());
    }
}
