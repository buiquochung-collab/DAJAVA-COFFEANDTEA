package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.CategoryService;
import com.example.demo.repository.OrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    @Autowired private OrderService orderService;
    @Autowired private CategoryService categoryService;

    @GetMapping("/revenue-7days")
    public ResponseEntity<Map<String, Object>> get7DayRevenue() {
        List<Order> orders = orderService.getAllOrders().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .filter(o -> o.getOrderDate().isAfter(LocalDate.now().minusDays(7).atStartOfDay()))
                .toList();

        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            dailyRevenue.put(LocalDate.now().minusDays(i), BigDecimal.ZERO);
        }

        for (Order order : orders) {
            LocalDate date = order.getOrderDate().toLocalDate();
            dailyRevenue.put(date, dailyRevenue.getOrDefault(date, BigDecimal.ZERO).add(order.getTotalAmount()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", dailyRevenue.keySet().stream().map(LocalDate::toString).collect(Collectors.toList()));
        response.put("data", dailyRevenue.values());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category-distribution")
    public ResponseEntity<Map<String, Object>> getCategoryDistribution() {
        List<Order> orders = orderService.getAllOrders().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .toList();

        Map<String, BigDecimal> categoryRevenue = new HashMap<>();
        for (Order order : orders) {
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    String catName = detail.getProduct().getCategory() != null ? detail.getProduct().getCategory().getName() : "Khác";
                    BigDecimal lineTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    categoryRevenue.put(catName, categoryRevenue.getOrDefault(catName, BigDecimal.ZERO).add(lineTotal));
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", categoryRevenue.keySet());
        response.put("data", categoryRevenue.values());
        return ResponseEntity.ok(response);
    }

    @Autowired private com.example.demo.repository.InventoryLogRepository inventoryLogRepository;

    @GetMapping("/export/revenue")
    public void exportRevenue(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=revenue_report.csv");
        response.setCharacterEncoding("UTF-8");
        
        // Write UTF-8 BOM for Excel to recognize encoding
        response.getWriter().write('\ufeff');
        
        java.io.PrintWriter writer = response.getWriter();
        writer.println("Mã đơn,Ngày đặt,Khách hàng,Tổng tiền,Trạng thái");
        
        List<Order> orders = orderService.getAllOrders();
        for (Order o : orders) {
            writer.println(String.format("%s,%s,%s,%s,%s",
                o.getId(),
                o.getOrderDate(),
                o.getUser().getUsername(),
                o.getTotalAmount(),
                o.getStatus()
            ));
        }
    }

    @GetMapping("/export/inventory")
    public void exportInventory(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=inventory_logs.csv");
        response.setCharacterEncoding("UTF-8");
        
        response.getWriter().write('\ufeff');
        java.io.PrintWriter writer = response.getWriter();
        writer.println("Thời gian,Nguyên liệu,Loại,Số lượng,Đơn giá,Nhà cung cấp,Người thực hiện");
        
        List<com.example.demo.model.InventoryLog> logs = inventoryLogRepository.findAllByOrderByCreatedAtDesc();
        for (com.example.demo.model.InventoryLog log : logs) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                log.getCreatedAt(),
                log.getIngredient().getName(),
                log.getType(),
                log.getQuantity(),
                log.getPricePerUnit(),
                log.getSupplier(),
                log.getPerformer().getUsername()
            ));
        }
    }
}
