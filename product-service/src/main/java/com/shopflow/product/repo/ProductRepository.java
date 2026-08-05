package com.shopflow.product.repo;

import com.shopflow.product.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Spring Data MONGO repository - identical programming model to Spring Data JPA
 * (derived queries from method names), different store underneath. This
 * uniformity across SQL/NoSQL is the point of the Spring Data umbrella.
 */
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategoryIgnoreCase(String category);
}
