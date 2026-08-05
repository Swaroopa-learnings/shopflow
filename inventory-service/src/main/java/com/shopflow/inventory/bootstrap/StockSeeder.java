package com.shopflow.inventory.bootstrap;

import com.shopflow.inventory.domain.InventoryItem;
import com.shopflow.inventory.repo.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Seeds stock matching product-service's demo catalog.
 * p-1003 gets only 2 units ON PURPOSE: order 3+ of it to watch the saga's
 * rejection path (order ends CANCELLED with "insufficient stock").
 */
@Configuration
public class StockSeeder {

    private static final Logger log = LoggerFactory.getLogger(StockSeeder.class);

    @Bean
    CommandLineRunner seedStock(InventoryRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.saveAll(List.of(
                    new InventoryItem("p-1001", 50),
                    new InventoryItem("p-1002", 200),
                    new InventoryItem("p-1003", 2)     // scarce on purpose - demo the failure path
            ));
            log.info("Seeded stock for {} products", repo.count());
        };
    }
}
