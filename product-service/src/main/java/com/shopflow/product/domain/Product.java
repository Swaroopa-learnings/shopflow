package com.shopflow.product.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * A product in the catalog, stored as a MongoDB document.
 *
 * The attributes map holds category-specific fields - a laptop has RAM and CPU,
 * a shirt has size and colour - without needing a fixed schema.
 */
@Document(collection = "products")
public class Product implements Serializable {

    @Id
    private String id;

    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Map<String, String> attributes;   // category-specific fields

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
