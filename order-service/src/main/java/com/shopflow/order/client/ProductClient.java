package com.shopflow.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * SYNCHRONOUS inter-service communication - OpenFeign declarative client.
 *
 * name = "product-service" is the EUREKA service id, not a URL. At runtime
 * Spring Cloud LoadBalancer resolves it to a live instance - the second half
 * of the service-discovery story (register in Eureka, then call by name).
 *
 * SYNC vs ASYNC - when each is used in this project:
 *  - SYNC (this call): order creation NEEDS the price before it can proceed;
 *    the answer is part of the request/response cycle. Cost: temporal
 *    coupling - if product-service is down, order creation degrades.
 *  - ASYNC (Kafka): the saga steps and notifications don't need an immediate
 *    answer; messaging decouples availability (inventory-service can be down
 *    for a minute and the saga simply resumes when it returns).
 * "Query synchronously, command asynchronously" is a decent rule of thumb.
 */
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {

    /** Tolerant-reader DTO: only the fields order-service cares about. */
    record ProductDto(String id, String name, BigDecimal price) {
    }

    @GetMapping("/{id}")
    ProductDto getProduct(@PathVariable("id") String id);
}
