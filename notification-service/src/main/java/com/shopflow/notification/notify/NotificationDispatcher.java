package com.shopflow.notification.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Decides which channels a notification goes out on.
 *
 * Two beans implement NotificationSender, so @Qualifier picks the one wanted at
 * each injection point.
 */
@Service
public class NotificationDispatcher {

    private final NotificationSender emailSender;
    private final NotificationSender smsSender;

    public NotificationDispatcher(@Qualifier("emailSender") NotificationSender emailSender,
                                  @Qualifier("smsSender") NotificationSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    /** Routine updates go by email; urgent ones also go by SMS. */
    public void dispatch(String userId, String subject, String body, boolean urgent) {
        emailSender.send(userId, subject, body);
        if (urgent) {
            smsSender.send(userId, subject, body);
        }
    }
}
