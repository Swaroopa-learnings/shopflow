# common-events — Shared Kafka Contracts

Plain library jar (not a runnable app): the commands and events every Kafka
producer/consumer agrees on, plus the topic names.

## Showcases

- **Message contracts** — [`Topics`](src/main/java/com/shopflow/common/events/Topics.java): commands vs events, topic design rationale
- **Backward compatibility for events** — every record is a tolerant reader via `@JsonIgnoreProperties(ignoreUnknown = true)`; evolution rules in [`OrderCreatedEvent`](src/main/java/com/shopflow/common/events/OrderCreatedEvent.java)
- **Compensation semantics** — [`ReleaseInventoryCommand`](src/main/java/com/shopflow/common/events/ReleaseInventoryCommand.java): the saga's "undo"

## Message flow at a glance

```
order.events        order-service ──► projection / notification-service (fan-out)
inventory.commands  order-service ──► inventory-service   (reserve / release)
inventory.events    inventory-service ──► saga orchestrator
payment.commands    order-service ──► payment-service     (charge)
payment.events      payment-service ──► saga orchestrator
```

Deliberately framework-free (only Jackson annotations). The trade-off vs a
schema registry is discussed in [`pom.xml`](pom.xml).
