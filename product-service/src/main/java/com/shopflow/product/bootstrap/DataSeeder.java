package com.shopflow.product.bootstrap;

import com.shopflow.product.domain.Product;
import com.shopflow.product.repo.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

/** Seeds demo products on startup if the catalog is empty. */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedProducts(ProductRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;   // don't duplicate on restart
            }
            Product laptop = new Product("ThinkBook 14", "14-inch developer laptop",
                    "electronics", new BigDecimal("899.99"),
                    Map.of("ram", "16GB", "cpu", "Ryzen 7", "storage", "512GB SSD"));
            laptop.setId("p-1001");

            Product keyboard = new Product("MX Keys", "Wireless mechanical keyboard",
                    "electronics", new BigDecimal("119.50"),
                    Map.of("layout", "US", "connectivity", "Bluetooth"));
            keyboard.setId("p-1002");

            Product tshirt = new Product("ShopFlow Tee", "Cotton t-shirt",
                    "apparel", new BigDecimal("19.99"),
                    Map.of("size", "L", "color", "navy"));
            tshirt.setId("p-1003");

            repo.saveAll(java.util.List.of(laptop, keyboard, tshirt));
            log.info("Seeded {} demo products", repo.count());
        };
    }
}
