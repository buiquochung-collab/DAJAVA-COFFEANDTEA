package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RevenueReportDTO {
    private LocalDate reportDate;
    private String storeInfo;
    
    // Revenue Breakdown
    private BigDecimal cashTotal; // COD
    private BigDecimal transferTotal; // MOMO, BANK_TRANSFER
    private BigDecimal voucherTotal; // Discount from vouchers
    private BigDecimal promoTotal; // Discount from promotions (Price difference)
    private BigDecimal grandTotal; // Final actual revenue
    
    // Statistics
    private int orderCount;
    private int totalProductCount;
    
    // Category Breakdown
    private int drinkCount; // Trà sữa, Cà phê, Trà trái cây, Đá xay, Thức uống...
    private int cakeCount; // Bánh ngọt, Bánh mặn...
}
