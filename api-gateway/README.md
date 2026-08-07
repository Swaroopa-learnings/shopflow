# api-gateway — Spring Cloud Gateway

**Port 8080** · Redis (rate-limit buckets) · reactive stack (Netty, not servlets)

The single front door. Clients only ever talk to :8080; the gateway routes to
services via Eureka and handles the cross-cutting edge concerns once.

## Showcases

- **API gateway routing** — [`application.yml`](src/main/resources/application.yml): path predicates → `lb://service-name` URIs
- **JWT verification at the edge** — [`JwtAuthenticationGlobalFilter`](src/main/java/com/shopflow/gateway/filter/JwtAuthenticationGlobalFilter.java): rejects bad tokens with 401, forwards identity as `X-User-Id`
- **Rate limiting** — Redis token bucket on the orders route ([`application.yml`](src/main/resources/application.yml)) + per-user [`KeyResolver`](src/main/java/com/shopflow/gateway/config/RateLimiterConfig.java)

## Routes

| Path | Target | Extras |
|---|---|---|
| `/api/v1/auth/**` | auth-service | public (no JWT) |
| `/api/v1|v2/products/**` | product-service | JWT |
| `/api/v1/orders/**` | order-service | JWT + rate limit (5/s, burst 10) |

## Poke it

```bash
# no token -> 401 from the gateway itself
curl -i localhost:8080/api/v1/orders

# hammer the orders route -> 429 + X-RateLimit-* headers
for i in $(seq 1 15); do curl -s -o /dev/null -w '%{http_code} ' \
  localhost:8080/api/v1/orders -H "Authorization: Bearer $TOKEN"; done

# live route table
curl localhost:8080/actuator/gateway/routes
```
