package com.shopflow.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * Calls product-service for pricing. Synchronous because an order can't be
 * created without the price.
 *
 * "product-service" is the Eureka service id, not a URL - Spring Cloud
 * LoadBalancer resolves it to a live instance at call time.
 */
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {

    /** Only the fields order-service needs; other fields are ignored. */
    record ProductDto(String id, String name, BigDecimal price) {
    }

    @GetMapping("/{id}")
    ProductDto getProduct(@PathVariable("id") String id);
}
