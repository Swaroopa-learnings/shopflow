package com.shopflow.product.web;

import com.shopflow.product.domain.Product;
import com.shopflow.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Version 2 of the catalog API. Adds priceCents and a preformatted
 * displayPrice; v1 is unaffected.
 *
 * Both versions call the same service layer - only the response shape differs.
 */
@RestController
@RequestMapping("/api/v2/products")
public class ProductControllerV2 {

    /** v2 response: v1's fields plus priceCents and displayPrice. */
    public record ProductResponseV2(
            String id,
            String name,
            String description,
            String category,
            BigDecimal price,
            long priceCents,
            String displayPrice,
            Map<String, String> attributes
    ) {
        static ProductResponseV2 from(Product p) {
            return new ProductResponseV2(
                    p.getId(), p.getName(), p.getDescription(), p.getCategory(),
                    p.getPrice(),
                    p.getPrice().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact(),
                    "$" + p.getPrice().setScale(2, RoundingMode.HALF_UP),
                    p.getAttributes());
        }
    }

    private final ProductService service;

    public ProductControllerV2(ProductService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ProductResponseV2 get(@PathVariable String id) {
        return ProductResponseV2.from(service.getProduct(id));
    }
}
