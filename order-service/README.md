# order-service — Orders, CQRS, Event Sourcing, Saga

**Port 8083** · H2 (dev) / Postgres `orderdb` (prod) · Redis (idempotency) · Kafka

The heart of the system: accepts orders, records them as an event stream, and
orchestrates the fulfillment saga across inventory- and payment-service.

## Showcases (the dense one)

**Request path**
- Servlet filters — [`RequestLoggingFilter`](src/main/java/com/shopflow/order/web/filter/RequestLoggingFilter.java) (correlation IDs + MDC), [`IdempotencyFilter`](src/main/java/com/shopflow/order/web/filter/IdempotencyFilter.java) (Stripe-style key + response replay via Redis)
- Interceptor — [`TimingInterceptor`](src/main/java/com/shopflow/order/web/interceptor/TimingInterceptor.java) (filter-vs-interceptor explained here), registered in [`WebMvcConfig`](src/main/java/com/shopflow/order/config/WebMvcConfig.java)
- JWT + Spring Security — [`JwtAuthFilter`](src/main/java/com/shopflow/order/security/JwtAuthFilter.java) → [`SecurityConfig`](src/main/java/com/shopflow/order/security/SecurityConfig.java) (defense in depth vs the gateway check)
- Validation — [`CreateOrderRequest`](src/main/java/com/shopflow/order/web/dto/CreateOrderRequest.java) + custom [`@AllowedCurrency`](src/main/java/com/shopflow/order/web/dto/validation/AllowedCurrency.java)

**Core patterns**
- CQRS — write: [`OrderCommandService`](src/main/java/com/shopflow/order/command/OrderCommandService.java) · read: [`OrderProjection`](src/main/java/com/shopflow/order/query/OrderProjection.java) → [`OrderQueryController`](src/main/java/com/shopflow/order/query/OrderQueryController.java)
- Event sourcing — [`OrderEventEntity`](src/main/java/com/shopflow/order/domain/OrderEventEntity.java) (append-only store), [`EventStoreService`](src/main/java/com/shopflow/order/command/EventStoreService.java) (store + publish, dual-write/outbox discussion), [`OrderAggregate`](src/test/java/com/shopflow/order/query/OrderAggregate.java) (test-only: proves state is derivable from the log)
- Saga orchestration — [`OrderSagaOrchestrator`](src/main/java/com/shopflow/order/saga/OrderSagaOrchestrator.java) (compensation, orchestration vs choreography, why not 2PC)
- Sync call — [`ProductClient`](src/main/java/com/shopflow/order/client/ProductClient.java) (Feign via Eureka; sync-vs-async rules)

**Operations**
- Scheduling — [`MaintenanceJobs`](src/main/java/com/shopflow/order/scheduled/MaintenanceJobs.java) (fixedRate / fixedDelay saga-timeout sweep / cron) on the pool from [`SchedulingConfig`](src/main/java/com/shopflow/order/config/SchedulingConfig.java)
- Profiles + HikariCP — [`application-dev.yml`](src/main/resources/application-dev.yml) vs [`application-prod.yml`](src/main/resources/application-prod.yml) (every pool knob commented)
- Actuator — health/metrics/prometheus/scheduledtasks in [`application.yml`](src/main/resources/application.yml)

## Endpoints

| Method | Path | Side |
|---|---|---|
| POST | `/api/v1/orders` | command → 202 + saga starts |
| GET | `/api/v1/orders/{id}` | query (read model) |
| GET | `/api/v1/orders` | query — my orders |
| GET | `/api/v1/orders/{id}/events` | event history (audit log) |

## Poke it

```bash
# place an order; repeat the SAME command -> replay (X-Idempotent-Replay: true)
curl -si -X POST localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: k1' \
  -d '{"productId":"p-1001","quantity":2,"currency":"USD"}'

# watch the saga finish, then inspect the event stream
curl -s localhost:8080/api/v1/orders/<orderId>/events -H "Authorization: Bearer $TOKEN"

# saga failure demo: p-1003 has only 2 units in stock
curl -s -X POST localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"productId":"p-1003","quantity":3,"currency":"USD"}'
```
