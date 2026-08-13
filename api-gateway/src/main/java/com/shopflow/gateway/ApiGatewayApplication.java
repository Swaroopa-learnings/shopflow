package com.shopflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single entry point for clients. Handles routing, token verification and
 * rate limiting once, so the services behind it don't have to.
 *
 * Built on Spring Cloud Gateway, which is reactive - filters here implement
 * GlobalFilter rather than the servlet API.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
