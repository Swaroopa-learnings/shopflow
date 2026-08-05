package com.shopflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API GATEWAY (Spring Cloud Gateway).
 *
 * WHY A GATEWAY? It is the single front door for all clients. Cross-cutting
 * concerns are handled ONCE here instead of in every microservice:
 *  - Routing:        /api/v1/orders/** -> order-service (resolved via Eureka)
 *  - Authentication: verify the JWT before traffic ever reaches a service
 *  - Rate limiting:  Redis-backed token bucket per user/IP
 *  - (also common:   TLS termination, CORS, request logging, response shaping)
 *
 * Clients only ever know ONE address (http://localhost:8080); the internal
 * topology can change freely behind it.
 *
 * NOTE: Spring Cloud Gateway is REACTIVE (Netty + WebFlux). Filters here
 * implement GlobalFilter/GatewayFilter, not the Servlet API - compare with
 * order-service which shows classic Servlet filters. Knowing that difference
 * is a frequent interview probe.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
