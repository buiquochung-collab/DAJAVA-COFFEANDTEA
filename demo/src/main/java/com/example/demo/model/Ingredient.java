package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ingredients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String unit; // g, ml, cái, miếng...

    @Column(name = "stock_quantity")
    private Double stockQuantity;

    @Column(name = "cost_per_unit")
    private BigDecimal costPerUnit;

    @Builder.Default
    @Column(name = "min_stock_threshold")
    private Double minStockThreshold = 5.0;
}
