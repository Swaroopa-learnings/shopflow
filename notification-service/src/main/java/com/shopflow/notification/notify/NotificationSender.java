package com.shopflow.notification.notify;

/**
 * Strategy interface with MULTIPLE implementations (email, SMS).
 * See NotificationDispatcher for how @Qualifier picks between them.
 */
public interface NotificationSender {

    void send(String userId, String subject, String body);
}
