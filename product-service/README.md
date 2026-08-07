# product-service — Catalog (MongoDB + Redis Cache)

**Port 8082** · MongoDB `productdb` · Redis (cache)

The read-heavy product catalog. Seeds three demo products on startup
(`p-1001`, `p-1002`, `p-1003`).

## Showcases

- **Non-relational DB** — [`Product`](src/main/java/com/shopflow/product/domain/Product.java) document with a schemaless `attributes` map (why catalogs fit document stores), [`ProductRepository`](src/main/java/com/shopflow/product/repo/ProductRepository.java) (same Spring Data model as JPA)
- **Caching annotations** — [`ProductService`](src/main/java/com/shopflow/product/service/ProductService.java): `@Cacheable` / `@CachePut` / `@CacheEvict` + the proxy self-invocation gotcha
- **Redis cache config** — [`RedisCacheConfig`](src/main/java/com/shopflow/product/config/RedisCacheConfig.java): TTL, JSON serialization, why shared cache beats per-instance
- **Backward compatibility** — [`ProductControllerV1`](src/main/java/com/shopflow/product/web/ProductControllerV1.java) vs [`ProductControllerV2`](src/main/java/com/shopflow/product/web/ProductControllerV2.java): additive-only evolution, both versions over one service layer

## Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/products/{id}` | cached read |
| GET | `/api/v1/products?category=` | list (uncached — see comment for why) |
| POST/PUT/DELETE | `/api/v1/products...` | mutations manage the cache |
| GET | `/api/v2/products/{id}` | v2 shape: adds `priceCents`, `displayPrice` |

## Poke it

```bash
# call twice: "CACHE MISS" log line appears only the first time
curl -s localhost:8080/api/v1/products/p-1001 -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/api/v1/products/p-1001 -H "Authorization: Bearer $TOKEN"

# see the cached entry in Redis
docker exec shopflow-redis redis-cli KEYS 'shopflow:products:*'
```
