# Code Review & Debugging Log

Much of this codebase was generated with AI assistance. This document is the
review layer on top of it: the defects found while running the system, how each
one was diagnosed, and the design changes made afterwards.

The theme running through all three defects is the same — **none of them threw
an error where the symptom appeared.** Every health check was green while the
system did nothing useful. That is the normal shape of failure in a distributed
system, and it's why "no exceptions in the log" is not evidence that anything
works.

---

## 1. Kafka consumers read every message and did nothing

**Severity:** critical — the order saga never progressed
**Component:** all five `@KafkaListener` methods

### Symptom

Placing an order returned `202 Accepted` and the order appeared in the event
store, but:

- `GET /api/v1/orders/{id}` returned **404** for an order that demonstrably existed
- the saga never advanced past step 1
- ten minutes later, the timeout sweep cancelled the order with `"saga timeout"`

No exception was logged anywhere. Every service was up, every consumer group was
registered, and Kafka was healthy.

### Why it was hard

Every signal that is normally used to detect a broken consumer looked correct:

| Check | Result |
|---|---|
| Services running | all 8 up |
| Consumer groups registered | all 5 present |
| Consumer lag | **0** |
| Offsets advancing | yes |
| Errors in logs | none |

A consumer with zero lag that logs no errors looks, by every usual measure,
like a consumer that is working.

### Diagnosis

The step that broke it open was comparing **messages produced against messages
replied, per topic**, rather than looking at any single service:

```
order.events         5 produced   5 consumed   lag 0
inventory.commands   5 produced   5 consumed   lag 0
inventory.events     0 produced   ← the chain breaks here
```

Inventory was consuming commands and never replying. Since offsets were
advancing, the records were definitely being polled — so the handler was
running and doing nothing, or not being invoked with what it expected.

To rule out the producer, a command was injected straight into the topic with
the console producer, with the type header set by hand:

```bash
printf "__TypeId__:com.shopflow.common.events.ReserveInventoryCommand\t{...}\n" \
  | kafka-console-producer.sh --topic inventory.commands --property parse.headers=true
```

The offset advanced. Still no reply. That eliminated the producer, the topic,
the type header and the trusted-packages configuration.

The listener looked like this — and note there is no `else`:

```java
@KafkaListener(topics = Topics.INVENTORY_COMMANDS, groupId = "inventory-service")
public void onCommand(Object command) {
    if (command instanceof ReserveInventoryCommand reserve) {
        inventoryService.reserve(reserve);
    } else if (command instanceof ReleaseInventoryCommand release) {
        inventoryService.release(release);
    }
    // anything else: silently discarded
}
```

Adding a branch that logged the payload's actual class turned an invisible
failure into a one-line answer:

```
ERROR c.s.i.m.InventoryCommandListener : Unhandled command payload of type
  org.apache.kafka.clients.consumer.ConsumerRecord - saga will stall.
  Payload: ConsumerRecord(topic = inventory.commands, partition = 0, offset = 13,
    headers = RecordHeaders(headers = [], isReadOnly = false),
    value = ReserveInventoryCommand[orderId=2222..., productId=p-1002, quantity=1])
```

### Root cause

Deserialization was never the problem — the `value` inside the record is a
correctly deserialized `ReserveInventoryCommand`. The problem was the parameter
type.

Spring Kafka supplies "provided arguments" to a listener method — the raw
`ConsumerRecord`, the `Acknowledgment`, the `Consumer` — and matches them to
parameters **by assignability, before any payload conversion runs**. Every
object is assignable to `Object`, so the parameter received the whole
`ConsumerRecord` rather than the value inside it. Both `instanceof` checks then
failed against the wrapper, and with no `else` branch the message was discarded
without a trace.

### Failed first attempt

The obvious fix — annotating the parameter `@Payload` — **did not work**:

```java
public void onCommand(@Payload Object command)   // still receives ConsumerRecord
```

Confirmed the annotation was actually present in the running bytecode before
concluding anything:

```bash
javap -v -p InventoryCommandListener.class | grep -A3 RuntimeVisibleParameterAnnotations
#   org.springframework.messaging.handler.annotation.Payload   ← present
```

It was compiled in and still failed, because provided-argument matching happens
before payload resolvers are consulted. The annotation never gets a say. The
only real fix is to stop using a parameter type that matches everything.

### Fix

Concrete parameter types, applied to all five listeners. See §4 for the design
change that followed.

Verified by injecting both command types and confirming each reached its own
handler:

```
Reserved 3 x p-1001 for order 4444... (47 left)
Released 3 x p-1001 for order 4444... (reason: testing @KafkaHandler routing)
```

### Takeaways

- **Advancing offsets prove delivery, not processing.** Lag is a delivery
  metric. It says nothing about whether the handler did its job.
- **The useful signal was produced-vs-replied per topic**, not any single
  service's health.
- **Never let a dispatch fall through silently.** Every type-based dispatch now
  has a default branch that logs the unmatched class. That one line converted a
  multi-hour hunt into an instant answer.
- **Verify the fix is actually deployed before judging it.** Checking the
  bytecode ruled out "stale build" as an explanation and kept the search honest.

---

## 2. Every gateway route returned 500 while the services were healthy

**Severity:** high — the entire API was unreachable through the gateway
**Component:** service discovery

### Symptom

```
POST http://localhost:8080/api/v1/auth/register   → 500
POST http://localhost:8081/api/v1/auth/register   → 201
```

Identical request. Through the gateway it failed; straight to the service it
worked. All seven services were registered in Eureka and reporting healthy.

### Diagnosis

That asymmetry is itself the clue: if bypassing discovery works and going
through it doesn't, the fault is in the routing layer, not the service.

Checking what the registry actually held:

```
instanceId : 172.20.10.7:auth-service:8081
hostName   : 172.20.10.7
status     : UP
```

And the machine's current address:

```
inet 172.20.10.8      ← current
172.20.10.7:8081      ← unreachable, connection refused
```

### Root cause

The laptop's DHCP lease changed (network switch), giving it a new IP. Services
had registered under the previous address, and Eureka still advertised it —
marked `UP`, because heartbeats were still arriving from the process. The
gateway resolved `lb://auth-service` to a host that no longer existed and
returned 500.

### Fix

For local development, register a stable hostname instead of the machine's
address:

```yaml
eureka:
  instance:
    hostname: localhost
    prefer-ip-address: false
```

And on the registry, stop dead instances from lingering:

```yaml
eureka:
  server:
    enable-self-preservation: false     # keeps dead instances during network trouble
    eviction-interval-timer-in-ms: 5000
```

**This fix is environment-specific and deliberately so.** In a real deployment
the opposite is usually correct — pods and VMs have no resolvable hostnames, so
`prefer-ip-address: true` is standard, and self-preservation exists precisely to
avoid mass-evicting healthy instances during a network partition. The fix is
right for a single machine and wrong for a cluster.

### Takeaways

- **Compare the path that works with the path that doesn't.** Direct call
  succeeding while the routed call failed pointed straight at the registry.
- **"Registered and UP" does not mean reachable.** A registry stores what a
  service claimed about itself at registration time.
- Configuration that is correct locally can be wrong in production. Both cases
  are documented in the config rather than silently assumed.

---

## 3. A service that had been running fine started reporting an empty database

**Severity:** medium — the service failed on every message after a period of idleness
**Component:** dev-profile datasource

### Symptom

A service that had been healthy for a long time began throwing this on every
consumed message:

```
org.h2.jdbc.JdbcSQLSyntaxErrorException:
  Table "RESERVATIONS" not found (this database is empty)
```

No deployment, no schema change, no restart. The table had existed and been
working earlier in the same process.

### Root cause

`jdbc:h2:mem:inventorydb` — an in-memory H2 database exists only while **at
least one connection to it is open**. HikariCP retires idle connections after
`idle-timeout`, and when the pool closed its last one, H2 discarded the entire
database. The next query then ran against an empty one.

The message "this database is empty" is precise, and it points at lifecycle
rather than at the query.

### Fix

```yaml
url: jdbc:h2:mem:inventorydb;DB_CLOSE_DELAY=-1
```

`DB_CLOSE_DELAY=-1` keeps the database alive for the lifetime of the JVM.
Applied to all four services using H2.

### Takeaways

- **"It worked earlier and nothing changed" points at lifecycle or state**, not
  at logic.
- Dev-profile configuration deserves the same scrutiny as production. This one
  would have reappeared on any demo left running for a while.
- Read the error literally. *"This database is empty"* is a different statement
  from *"table not found"*, and the difference is the whole diagnosis.

---

## 4. Design review: type dispatch in Kafka listeners

Not a defect report — a review of a pattern used in five places, and the
change made as a result.

### What the code did

Each listener took a generic payload and branched on the runtime type:

```java
public void onCommand(Object command) {
    if (command instanceof ReserveInventoryCommand reserve) { ... }
    else if (command instanceof ReleaseInventoryCommand release) { ... }
}
```

### Review findings

1. **Silent failure.** No default branch, so an unrecognised payload was
   discarded with no log line. This is what made §1 take hours instead of
   minutes.
2. **Ambiguous parameter type.** `Object` matches every provided argument, which
   is the root cause in §1. The signature invites the bug.
3. **Grows badly.** Adding an event type means extending a conditional chain in
   every listener that cares.
4. **No type safety.** The compiler cannot check that the handled types are the
   ones the topic actually carries.

### Change

Class-level `@KafkaListener` with one `@KafkaHandler` per concrete type, and a
default handler that logs anything unmatched:

```java
@Component
@KafkaListener(topics = Topics.INVENTORY_COMMANDS, groupId = "inventory-service")
public class InventoryCommandListener {

    @KafkaHandler
    public void onReserve(ReserveInventoryCommand command) { ... }

    @KafkaHandler
    public void onRelease(ReleaseInventoryCommand command) { ... }

    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.error("Unhandled payload type {}", payload.getClass().getName());
    }
}
```

Spring routes each message to the handler whose parameter type matches, so the
`instanceof` chains disappear and the ambiguous-parameter problem cannot recur —
`ConsumerRecord` is not assignable to `ReserveInventoryCommand`.

Where a topic carries only one type, a plain method-level listener with a
concrete parameter is enough, and payment-service uses that simpler form.

### Effect on readability

The saga orchestrator was the clearest win. It went from two methods containing
four branches to four methods named after the four saga transitions:

```
onInventoryReserved   →  reserve succeeded, request payment
onInventoryRejected   →  out of stock, cancel
onPaymentCompleted    →  paid, complete the order
onPaymentFailed       →  release stock, cancel
```

The class now reads as the state machine it implements, which is the form it
should have had from the start.

---

## Review checklist for event-driven code

Derived from the findings above; applied to this repo and reusable elsewhere.

**Consumers**

- [ ] Does every type-based dispatch have a default branch that logs?
- [ ] Are handler parameters concrete types rather than `Object`?
- [ ] Is the handler safe to run twice? (delivery is at-least-once)
- [ ] What happens to a message that fails repeatedly — retry, dead-letter, or silent loss?

**Producers**

- [ ] Is a partition key set where ordering matters?
- [ ] Is the database write and the publish atomic, or can one happen without the other?

**Configuration**

- [ ] Is any config correct only for local development, and is that written down?
- [ ] Do in-memory datasources survive an idle connection pool?
- [ ] Does anything register or cache an address that can change?

**Observability**

- [ ] Can a request be followed across services by a single id?
- [ ] Would a consumer that reads everything and does nothing be visible from the outside?

That last question is the one this codebase originally failed, and it's the
first thing worth asking of any message-driven service.
