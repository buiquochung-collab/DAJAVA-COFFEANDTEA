package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User performer; // Người thực hiện nhập/xuất

    private String type; // IN (Nhập), OUT (Xuất - dùng cho điều chỉnh thủ công)
    
    private Double quantity;
    
    private BigDecimal pricePerUnit; // Giá nhập của đợt này
    
    private String supplier;
    
    private LocalDateTime createdAt;
    
    private String note;
}
