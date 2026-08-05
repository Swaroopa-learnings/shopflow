package com.shopflow.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * SERVICE DISCOVERY (Eureka server).
 *
 * PROBLEM IT SOLVES: in a microservice system, instances come and go (scaling,
 * restarts, crashes) and their IP/port change constantly. Hard-coding
 * "http://localhost:8082" into callers breaks the moment anything moves.
 *
 * HOW IT WORKS:
 *  1. Every service starts up and REGISTERS itself here ("product-service is at
 *     192.168.1.5:8082") and then sends a heartbeat every 30s.
 *  2. Callers ask the registry (or keep a cached copy of it) and pick an
 *     instance - this is CLIENT-SIDE load balancing (Spring Cloud LoadBalancer).
 *     That is why the gateway can route to "lb://product-service" and Feign can
 *     call "product-service" by NAME instead of a URL.
 *  3. Instances that stop heart-beating are evicted from the registry.
 *
 * Dashboard: http://localhost:8761 shows every registered instance.
 *
 * INTERVIEW NOTE: In Kubernetes you usually DON'T run Eureka - K8s Services +
 * DNS provide discovery natively, and a service mesh (see deploy/istio) moves
 * load balancing into a sidecar proxy. Eureka remains common for VM/on-prem
 * Spring stacks, so knowing both answers is valuable.
 */
@SpringBootApplication
@EnableEurekaServer   // one annotation turns this app into the registry
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
