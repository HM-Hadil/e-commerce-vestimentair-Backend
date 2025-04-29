package com.veststore.veststoreback.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.engine.jdbc.Size;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private ProductSize size;

    private String color;

    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonBackReference

    private Category category;
    @Lob
    @Basic(fetch = FetchType.EAGER) // Add this annotation
    private byte[] imageUrl;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    public boolean hasLowStock() {
        return stock <= lowStockThreshold;
    }

    public boolean hasEnoughStock(int quantity) {
        return stock >= quantity;
    }
}
