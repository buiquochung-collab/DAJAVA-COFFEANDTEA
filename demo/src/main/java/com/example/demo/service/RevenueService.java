package com.example.demo.service;

import com.example.demo.dto.RevenueReportDTO;
import com.example.demo.model.Category;
import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RevenueService {

    @Autowired
    private OrderRepository orderRepository;

    public RevenueReportDTO generateDailyReport(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Order> completedOrders = orderRepository.findByOrderDateBetweenAndStatus(
            startOfDay, endOfDay, Order.OrderStatus.COMPLETED);

        BigDecimal cashTotal = BigDecimal.ZERO;
        BigDecimal transferTotal = BigDecimal.ZERO;
        BigDecimal voucherTotal = BigDecimal.ZERO;
        BigDecimal promoTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        
        int productCount = 0;
        int drinkCount = 0;
        int cakeCount = 0;

        for (Order order : completedOrders) {
            grandTotal = grandTotal.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            
            // Payment method breakdown
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                cashTotal = cashTotal.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            } else {
                transferTotal = transferTotal.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            }
            
            // Voucher discount
            if (order.getDiscountAmount() != null) {
                voucherTotal = voucherTotal.add(order.getDiscountAmount());
            }

            // Products breakdown & Promo calculation
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    productCount += detail.getQuantity();
                    Product p = detail.getProduct();
                    
                    // Categorize: Cake vs Drink
                    if (isCake(p.getCategory())) {
                        cakeCount += detail.getQuantity();
                    } else {
                        drinkCount += detail.getQuantity();
                    }
                    
                    // Calculate Promotional discount (Difference between base price and sold price)
                    BigDecimal basePrice = "L".equalsIgnoreCase(detail.getSize()) && p.getPriceL() != null ? p.getPriceL() : 
                                          (p.getPriceM() != null ? p.getPriceM() : p.getPrice());
                    
                    if (basePrice != null && detail.getPrice() != null && basePrice.compareTo(detail.getPrice()) > 0) {
                        BigDecimal itemPromoDiscount = basePrice.subtract(detail.getPrice()).multiply(BigDecimal.valueOf(detail.getQuantity()));
                        promoTotal = promoTotal.add(itemPromoDiscount);
                    }
                }
            }
        }

        return RevenueReportDTO.builder()
                .reportDate(date)
                .storeInfo("TEA & COFFEE HERITAGE - CN Thủ Đức")
                .cashTotal(cashTotal)
                .transferTotal(transferTotal)
                .voucherTotal(voucherTotal)
                .promoTotal(promoTotal)
                .grandTotal(grandTotal)
                .orderCount(completedOrders.size())
                .totalProductCount(productCount)
                .drinkCount(drinkCount)
                .cakeCount(cakeCount)
                .build();
    }
    
    private boolean isCake(Category category) {
        if (category == null) return false;
        String name = category.getName().toLowerCase();
        if (name.contains("bánh")) return true;
        if (category.getParentCategory() != null && category.getParentCategory().getName().toLowerCase().contains("bánh")) return true;
        return false;
    }
}
