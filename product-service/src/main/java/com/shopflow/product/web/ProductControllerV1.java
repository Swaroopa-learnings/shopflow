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
 * Version 1 of the catalog API, kept unchanged for existing clients while v2
 * serves the newer response shape.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductControllerV1 {

    private final ProductService service;

    public ProductControllerV1(ProductService service) {
        this.service = service;
    }

    /** Returns the stored document as-is. */
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
