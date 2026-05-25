package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/{id}/details")
    @jakarta.transaction.Transactional
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order != null) {
            // Chủ động nạp các collection và proxy để tránh LazyInitializationException
            if (order.getOrderDetails() != null) {
                order.getOrderDetails().forEach(detail -> {
                    if (detail.getProduct() != null) {
                        detail.getProduct().getName(); // Initialize product proxy
                    }
                });
            }
            if (order.getUser() != null) {
                order.getUser().getUsername(); // Initialize user proxy
            }
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        Map<String, Object> response = new HashMap<>();
        if (order != null) {
            response.put("status", order.getStatus().toString());
            // Trả về true nếu đơn hàng đã được Admin xác nhận (chuyển sang PROCESSING hoặc cao hơn)
            response.put("isConfirmed", order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CANCELLED);
        } else {
            response.put("error", "Order not found");
        }
        return ResponseEntity.ok(response);
    }
}
