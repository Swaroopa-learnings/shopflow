package com.shopflow.notification.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends email notifications (logged here rather than actually sent).
 *
 * @Async so the caller returns immediately and sending happens on the pool
 * defined in AsyncConfig.
 */
@Component("emailSender")
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Async("notificationExecutor")
    @Override
    public void send(String userId, String subject, String body) {
        simulateLatency();   // stands in for an SMTP round-trip
        log.info("EMAIL to user {} [{}]: {} (sent on thread {})",
                userId, subject, body, Thread.currentThread().getName());
    }

    private void simulateLatency() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
