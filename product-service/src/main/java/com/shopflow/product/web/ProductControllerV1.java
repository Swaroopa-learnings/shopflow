package com.shopflow.product.web;

import com.shopflow.product.domain.Product;
import com.shopflow.product.service.ProductService;
import com.shopflow.product.web.dto.ProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V1 API - BACKWARD COMPATIBILITY, part 1.
 *
 * THE RULE: once clients depend on an API you may only make ADDITIVE changes.
 * Renaming/removing fields, changing types, or changing semantics BREAKS
 * callers you don't control (mobile apps in the field, partner integrations).
 *
 * When a breaking change is unavoidable, you version the API and run BOTH
 * versions side by side (this class + ProductControllerV2), migrate clients,
 * then sunset v1 after a deprecation window. URL versioning (/api/v1/..) is
 * used here because it's explicit and cache/proxy friendly; header or
 * media-type versioning are the common alternatives.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductControllerV1 {

    private final ProductService service;

    public ProductControllerV1(ProductService service) {
        this.service = service;
    }

    /** v1 exposes the raw document - the shape v1 clients were built against. */
    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return service.getProduct(id);
    }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return category == null ? service.all() : service.byCategory(category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return service.create(new Product(
                request.name(), request.description(), request.category(),
                request.price(), request.attributes()));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, new Product(
                request.name(), request.description(), request.category(),
                request.price(), request.attributes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
