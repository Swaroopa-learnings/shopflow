package com.shopflow.notification.notify;

/** A channel a notification can be sent on. Implemented by email and SMS. */
public interface NotificationSender {

    void send(String userId, String subject, String body);
}
