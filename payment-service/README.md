# payment-service — Payments (Circuit Breaker)

**Port 8085** · H2 (dev) / Postgres `paymentdb` (prod) · Kafka

Charges the customer by calling a deliberately unreliable mock bank, protected
by a Resilience4j circuit breaker + retry. Failures become `PaymentFailedEvent`s
that trigger the saga's compensation.

## Showcases

- **Circuit breaker + retry** — [`PaymentProcessor`](src/main/java/com/shopflow/payment/service/PaymentProcessor.java): CLOSED→OPEN→HALF_OPEN state machine, exponential backoff, fallback feeding the saga; knobs explained in [`application.yml`](src/main/resources/application.yml)
- **The flaky dependency** — [`MockBankClient`](src/main/java/com/shopflow/payment/client/MockBankClient.java): configurable failure rate (`app.bank.failure-rate`)
- **Idempotent consumer** — [`Payment`](src/main/java/com/shopflow/payment/domain/Payment.java): PK = orderId → a redelivered command never double-charges
- **Proxy gotcha** — [`PaymentCommandListener`](src/main/java/com/shopflow/payment/messaging/PaymentCommandListener.java): why the annotations only work across the bean boundary

## Messages

| Direction | Topic | Payload |
|---|---|---|
| in | `payment.commands` | `ProcessPaymentCommand` |
| out | `payment.events` | `PaymentCompletedEvent`, `PaymentFailedEvent` |

## Poke it

```bash
# breaker state live
curl -s localhost:8085/actuator/circuitbreakers | python3 -m json.tool

# force the breaker OPEN: set app.bank.failure-rate: 1.0, restart, place ~6
# orders -> watch instant fallbacks + orders cancelled with stock released
```
