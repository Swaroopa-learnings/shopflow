package com.shopflow.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * PRODUCT-SERVICE - the read-heavy product catalog.
 *
 * Demonstrates:
 *  - NON-RELATIONAL DB integration: catalog lives in MongoDB. Products are
 *    document-shaped (every category has different attributes - a laptop has
 *    RAM/CPU, a shirt has size/color) which fits a schemaless document store
 *    far better than a rigid relational schema with sparse columns.
 *  - DISTRIBUTED CACHING: Redis via Spring's @Cacheable annotations
 *    (cache-aside pattern) - see ProductService.
 *  - BACKWARD COMPATIBILITY: /api/v1 and /api/v2 served side by side - see
 *    the two controllers.
 */
@SpringBootApplication
@EnableCaching   // switches on the cache abstraction; without it @Cacheable is silently ignored
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
