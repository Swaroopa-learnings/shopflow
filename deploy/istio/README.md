# Service Mesh (Istio) — the "what and why"

This demo runs on `docker compose` + Eureka, so the mesh is **documented rather
than executed** — these manifests show what the same system looks like when
deployed to Kubernetes with Istio. Being able to explain this trade-off is the
interview win; running Istio locally adds nothing to a showcase repo.

## What a service mesh is

A **dedicated infrastructure layer for service-to-service communication**.
Istio injects a **sidecar proxy** (Envoy) into every pod; all traffic in and
out of the service flows through its proxy. The application code stops caring
about retries, TLS, load balancing — the mesh does it.

```
 pod A                          pod B
 ┌───────────────┐             ┌───────────────┐
 │ order-service │             │ payment-svc   │
 │      │        │   mTLS      │       ▲       │
 │  [Envoy] ─────┼─────────────┼──► [Envoy]    │
 └───────────────┘             └───────────────┘
        ▲  control plane (istiod) configures both proxies
```

## Why it matters vs. what we do in code today

| Concern            | In this repo (library approach)         | With a mesh (platform approach)       |
|--------------------|-----------------------------------------|---------------------------------------|
| Service discovery  | Eureka + Spring Cloud LoadBalancer      | K8s DNS + Envoy                       |
| Retries/timeouts   | Resilience4j annotations in Java        | `VirtualService` YAML, language-agnostic |
| Circuit breaking   | Resilience4j `@CircuitBreaker`          | `DestinationRule` outlier detection   |
| Encryption         | none between services (demo)            | automatic mTLS, zero code             |
| Canary releases    | not possible                            | weighted routing (90/10 below)        |
| Observability      | per-service actuator/Micrometer         | uniform L7 metrics/traces for free    |

Key sentence for interviews: *libraries solve resilience per-language inside
the app; a mesh moves those concerns into the platform so every service —
Java, Go, Python — gets them uniformly, at the cost of operational complexity.*

## Files here

- `destination-rule.yaml` — circuit breaking + outlier detection for payment-service (the mesh equivalent of our Resilience4j config)
- `virtual-service.yaml` — canary traffic split: 90% of order-service traffic to v1, 10% to v2
- `peer-authentication.yaml` — enforce STRICT mTLS between all ShopFlow services
