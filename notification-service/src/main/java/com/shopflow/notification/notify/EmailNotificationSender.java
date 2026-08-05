package com.shopflow.notification.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * One of two NotificationSender beans. @Component("emailSender") gives it an
 * explicit bean NAME that @Qualifier can reference.
 *
 * @Async: the caller returns IMMEDIATELY; the body runs on the "notif-" thread
 * pool defined in AsyncConfig. Right tool for fire-and-forget side work
 * (emails, webhooks) that must not block message processing.
 * Same proxy caveat as @Cacheable/@Transactional: self-invocation bypasses it.
 */
@Component("emailSender")
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Async("notificationExecutor")
    @Override
    public void send(String userId, String subject, String body) {
        simulateLatency();   // a real SMTP round-trip takes time - hence @Async
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
