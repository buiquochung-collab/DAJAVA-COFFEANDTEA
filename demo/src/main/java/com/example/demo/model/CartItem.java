package com.example.demo.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem implements Serializable {
    private Long productId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal price;
    private String size; // M, L
    private String note; // Ghi chú của khách hàng
    private Integer sweetness; // -10, 0, 10 (ml change)
    private Integer teaCoffeeAmount; // -10, 0, 10 (ml change)
    
    public BigDecimal getSubtotal() {
        return price.multiply(new BigDecimal(quantity));
    }
}
