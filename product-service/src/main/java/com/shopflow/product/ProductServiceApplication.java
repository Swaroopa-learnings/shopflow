package com.shopflow.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Product catalog service. Stores products in MongoDB, caches reads in Redis,
 * and serves both v1 and v2 of the catalog API.
 */
@SpringBootApplication
@EnableCaching   // required, otherwise @Cacheable is ignored
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
