package com.shopflow.product.repo;

import com.shopflow.product.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Product repository, backed by MongoDB. */
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategoryIgnoreCase(String category);
}
