package com.shopflow.product.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * MONGO DOCUMENT (compare with the JPA @Entity classes in the other services).
 *
 * - @Document maps to the "products" COLLECTION (not table).
 * - No schema migration needed: adding a field tomorrow just... works.
 * - The `attributes` map is the killer feature here: each product carries
 *   arbitrary category-specific fields ({"ram":"16GB"} vs {"size":"XL"}) -
 *   painful in SQL (EAV tables / JSONB), natural in a document store.
 *
 * Implements Serializable because instances are stored in Redis by the cache
 * (we use JSON serialization, but Serializable keeps options open).
 */
@Document(collection = "products")
public class Product implements Serializable {

    @Id
    private String id;              // Mongo ObjectId as String

    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Map<String, String> attributes;   // schemaless, per-category fields

    public Product() {
    }

    public Product(String name, String description, String category,
                   BigDecimal price, Map<String, String> attributes) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.attributes = attributes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
}
