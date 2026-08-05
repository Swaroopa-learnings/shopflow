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
 * V2 API - BACKWARD COMPATIBILITY, part 2.
 *
 * v2 evolved the contract: response gains `displayPrice` (pre-formatted, with
 * currency) and `priceCents` (integer, no floating point ambiguity for
 * clients). v1 keeps serving its original shape untouched - existing clients
 * never notice v2 exists.
 *
 * Note the pattern: BOTH versions delegate to the SAME service layer; only the
 * response mapping differs. Versioning is a presentation concern - never fork
 * business logic per API version.
 */
@RestController
@RequestMapping("/api/v2/products")
public class ProductControllerV2 {

    /** v2 response shape - additive evolution of v1's. */
    public record ProductResponseV2(
            String id,
            String name,
            String description,
            String category,
            BigDecimal price,
            long priceCents,        // NEW in v2
            String displayPrice,    // NEW in v2
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
