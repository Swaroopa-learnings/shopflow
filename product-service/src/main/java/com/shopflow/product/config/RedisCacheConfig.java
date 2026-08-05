package com.shopflow.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * REDIS CACHE tuning.
 *
 * WHY REDIS OVER AN IN-MEMORY CACHE (Caffeine/ConcurrentHashMap)?
 * With 3 replicas of product-service, an in-memory cache gives 3 inconsistent
 * caches and 3x the misses. Redis is a shared, out-of-process cache: one
 * eviction is seen by every replica instantly.
 *
 * TTL MATTERS: entries expire after 10 minutes even if never explicitly
 * evicted - the safety net that bounds staleness when some other writer
 * forgets to evict. Choosing TTL = "how stale can this data acceptably be?"
 */
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                // JSON instead of JDK serialization: human-readable in redis-cli,
                // and no Serializable/classpath coupling between app versions.
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
    }
}
