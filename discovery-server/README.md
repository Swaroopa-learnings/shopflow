# discovery-server — Eureka Registry

**Port 8761** · no database · no Kafka

The service registry: every other service registers here on startup and
heartbeats every 30s; callers resolve each other **by name** instead of
hardcoded host:port (`lb://product-service`, Feign's `name = "product-service"`).

## Showcases

- **Service discovery** — [`DiscoveryServerApplication`](src/main/java/com/shopflow/discovery/DiscoveryServerApplication.java) (the class comment covers how registration/heartbeat/eviction work, and why Kubernetes usually replaces Eureka)

## Poke it

- Dashboard: <http://localhost:8761> — all registered instances at a glance
- Raw registry: `curl -H 'Accept: application/json' localhost:8761/eureka/apps`

Start this service **first**; everything else registers with it.
