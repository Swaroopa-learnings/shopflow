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
 * CACHING ANNOTATIONS in action (cache name "products", backed by Redis).
 *
 * The three verbs:
 *  - @Cacheable : read-through. Check cache first; on miss run the method and
 *                 store the result. Subsequent calls skip Mongo entirely.
 *  - @CachePut  : write-through. ALWAYS run the method, then refresh the
 *                 cached entry with the new value.
 *  - @CacheEvict: invalidate. Remove the entry (or all entries) so the next
 *                 read repopulates from the source of truth.
 *
 * HOW IT WORKS UNDER THE HOOD (interview favorite): Spring wraps this bean in
 * a PROXY. The caching logic lives in the proxy, which is why a SELF-call
 * (this.getProduct(id) from another method in this class) BYPASSES the cache -
 * the call never crosses the proxy boundary. Same mechanism and same gotcha
 * as @Transactional and @Async.
 *
 * Watch it work: call GET /api/v1/products/{id} twice - the log line below
 * prints only on the first call.
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
        // If you see this log line, it was a CACHE MISS (method actually executed).
        log.info("CACHE MISS - loading product {} from MongoDB", id);
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    /** Lists are not cached here: low hit-rate + hard to invalidate correctly. */
    public List<Product> byCategory(String category) {
        return repository.findByCategoryIgnoreCase(category);
    }

    public List<Product> all() {
        return repository.findAll();
    }

    public Product create(Product product) {
        // No cache interaction needed: a brand new id can't be cached yet.
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
        return repository.save(existing);   // return value replaces the cache entry
    }

    @CacheEvict(value = "products", key = "#id")
    public void delete(String id) {
        repository.deleteById(id);
    }
}
