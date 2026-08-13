package com.shopflow.product.service;

import com.shopflow.product.domain.Product;
import com.shopflow.product.repo.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Product lookups, cached in Redis under the "products" cache.
 *
 *  - @Cacheable  serves from cache, running the method only on a miss
 *  - @CachePut   always runs and refreshes the cached entry
 *  - @CacheEvict removes the entry
 *
 * Caching is applied by a proxy, so calls from inside this class bypass it.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(String id) {
        // Only logged on a cache miss, since a hit never runs this method.
        log.info("CACHE MISS - loading product {} from MongoDB", id);
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    /** Not cached: lists have a low hit rate and are awkward to invalidate. */
    public List<Product> byCategory(String category) {
        return repository.findByCategoryIgnoreCase(category);
    }

    public List<Product> all() {
        return repository.findAll();
    }

    public Product create(Product product) {
        // Nothing to evict - a new id can't be cached yet.
        return repository.save(product);
    }

    @CachePut(value = "products", key = "#id")
    public Product update(String id, Product incoming) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setCategory(incoming.getCategory());
        existing.setPrice(incoming.getPrice());
        existing.setAttributes(incoming.getAttributes());
        return repository.save(existing);   // the returned value replaces the cache entry
    }

    @CacheEvict(value = "products", key = "#id")
    public void delete(String id) {
        repository.deleteById(id);
    }
}
