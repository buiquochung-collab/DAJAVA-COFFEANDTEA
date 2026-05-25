package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;
    
    private String description;
    
    @Column(name = "discount_amount")
    private BigDecimal discountAmount;
    
    @Column(name = "discount_type")
    private String discountType; // "PERCENT" or "FIXED"
    
    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(columnDefinition = "TEXT")
    private String terms;
    
    @Column(name = "max_discount")
    private BigDecimal maxDiscount;
}
