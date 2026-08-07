# inventory-service — Stock (Saga Participant)

**Port 8084** · H2 (dev) / Postgres `inventorydb` (prod) · Kafka

Reserves and releases stock in response to Kafka commands. Has no HTTP API and
no idea a "saga" exists — participants stay simple on purpose.

## Showcases

- **Saga participant** — [`InventoryCommandListener`](src/main/java/com/shopflow/inventory/messaging/InventoryCommandListener.java) → [`InventoryService`](src/main/java/com/shopflow/inventory/service/InventoryService.java): local ACID transaction per command, reply with an event
- **Compensation** — `release()` in [`InventoryService`](src/main/java/com/shopflow/inventory/service/InventoryService.java): the semantic "undo", tolerant of work that never happened
- **Idempotent consumer** — [`Reservation`](src/main/java/com/shopflow/inventory/domain/Reservation.java): PK = orderId, so at-least-once redelivery can't double-reserve
- **Optimistic locking** — `@Version` on [`InventoryItem`](src/main/java/com/shopflow/inventory/domain/InventoryItem.java): two buyers racing for the last unit can't oversell

## Messages

| Direction | Topic | Payload |
|---|---|---|
| in | `inventory.commands` | `ReserveInventoryCommand`, `ReleaseInventoryCommand` |
| out | `inventory.events` | `InventoryReservedEvent`, `InventoryRejectedEvent` |

## Demo data

[`StockSeeder`](src/main/java/com/shopflow/inventory/bootstrap/StockSeeder.java): p-1001 → 50 units, p-1002 → 200, **p-1003 → 2 (scarce on purpose — order 3 to trigger the rejection path)**.
