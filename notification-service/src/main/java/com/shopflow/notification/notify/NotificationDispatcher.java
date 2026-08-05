package com.shopflow.notification.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * QUALIFIERS - resolving "which bean?" when an interface has many implementations.
 *
 * THE PROBLEM: two beans implement NotificationSender (emailSender, smsSender).
 * Injecting plain `NotificationSender` would fail at startup with
 * NoUniqueBeanDefinitionException - Spring cannot guess.
 *
 * THE OPTIONS (know all three):
 *  - @Qualifier("beanName")  - pick explicitly at the injection point (used here)
 *  - @Primary on one bean    - "the default unless a qualifier says otherwise"
 *  - inject List<NotificationSender> / Map<String, NotificationSender>
 *    - receive ALL of them (great for strategy registries / broadcast)
 *
 * Type-safe upgrade worth mentioning: define a custom annotation
 * (@interface Email, meta-annotated with @Qualifier) instead of a string name.
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

    /** Business routing: routine updates -> email; urgent ones -> email + SMS. */
    public void dispatch(String userId, String subject, String body, boolean urgent) {
        emailSender.send(userId, subject, body);
        if (urgent) {
            smsSender.send(userId, subject, body);
        }
    }
}
