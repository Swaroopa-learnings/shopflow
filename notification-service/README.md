# notification-service — Email/SMS Fan-out

**Port 8086** · no database · Kafka (consumer only)

Listens to the order event stream and "sends" email/SMS (log lines standing in
for SMTP/Twilio). The whole service was added without touching a single
producer — the payoff of event-driven fan-out.

## Showcases

- **Qualifiers** — [`NotificationDispatcher`](src/main/java/com/shopflow/notification/notify/NotificationDispatcher.java): two `NotificationSender` beans, `@Qualifier` picks email vs SMS; `@Primary` and `List<T>` injection discussed in the comment
- **@Async + thread pool** — [`EmailNotificationSender`](src/main/java/com/shopflow/notification/notify/EmailNotificationSender.java) runs on the bounded executor in [`AsyncConfig`](src/main/java/com/shopflow/notification/config/AsyncConfig.java) (CallerRunsPolicy = backpressure)
- **Kafka consumer-group fan-out** — [`OrderEventsListener`](src/main/java/com/shopflow/notification/messaging/OrderEventsListener.java): own `groupId` = own full copy of `order.events`, independent of the CQRS projection

## Messages

| Direction | Topic | Reaction |
|---|---|---|
| in | `order.events` | created → email · completed → email · cancelled → email **+ SMS** (urgent) |

## Watch it

Place an order, then watch this service's logs: notifications appear on
`notif-1`/`notif-2` threads — proof the Kafka listener thread isn't blocked.
