package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.validation.constraints.NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(nullable = false)
    private String name;

    @jakarta.validation.constraints.Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private BigDecimal price; // Base price or price for M size
    
    @jakarta.validation.constraints.Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private BigDecimal priceM;
    
    @jakarta.validation.constraints.Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private BigDecimal priceL;
    private Integer stock;
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_best_seller")
    private Boolean bestSeller;

    @Builder.Default
    @Column(name = "is_recipe_based")
    private Boolean isRecipeBased = false;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Transient
    private BigDecimal salePrice;

    @Transient
    private Integer calculatedStock;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public BigDecimal getEffectivePrice() {
        BigDecimal basePrice = price != null ? price : (priceM != null ? priceM : BigDecimal.ZERO);
        if (promotion != null && promotion.isCurrentlyActive()) {
            if (promotion.getDiscountType() == Promotion.DiscountType.PERCENT) {
                BigDecimal discount = basePrice.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
                return basePrice.subtract(discount);
            } else {
                return basePrice.subtract(promotion.getDiscountValue()).max(BigDecimal.ZERO);
            }
        }
        return basePrice;
    }
}
