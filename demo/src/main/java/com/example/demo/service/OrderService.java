package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.User;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemSettingService systemSettingService;

    @Transactional
    public Order createOrder(Order order, List<OrderDetail> orderDetails) {
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentDeadline(LocalDateTime.now().plusMinutes(3));
        order.setStatus(Order.OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            orderDetailRepository.save(detail);
        }
        
        // Deduct stock immediately when order is created
        inventoryService.deductStock(orderDetails);
        
        if (order.getUser() != null) {
            notificationService.sendNotification(order.getUser(), 
                "Đặt hàng thành công", 
                "Đơn hàng #" + savedOrder.getId() + " của bạn đã được tiếp nhận.", 
                "ORDER", "/user/orders");
        }
        
        return savedOrder;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .filter(o -> o.getPaymentDeadline() != null && o.getPaymentDeadline().isBefore(LocalDateTime.now()))
                .toList();
        
        for (Order order : expiredOrders) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            // Revert stock when cancelled
            inventoryService.refundStock(order.getOrderDetails());
        }
    }

    public List<Order> findByUser(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    public List<Order> searchOrders(User user, Long orderId, String dateStr) {
        if (orderId != null) {
            return orderRepository.findByUserAndId(user, orderId);
        }
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.atTime(23, 59, 59);
                return orderRepository.findByUserAndOrderDateBetween(user, start, end);
            } catch (Exception e) {
                // Return default if date parsing fails
            }
        }
        return findByUser(user);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        if (order != null) {
            Order.OrderStatus oldStatus = order.getStatus();
            if (oldStatus == status) return order;

            order.setStatus(status);
            Order savedOrder = orderRepository.save(order);
            
            // Send notification for status update
            String statusMsg = "";
            switch (status) {
                case PROCESSING -> statusMsg = "đang được chế biến.";
                case SHIPPING -> statusMsg = "đang được giao đến bạn.";
                case COMPLETED -> statusMsg = "đã hoàn thành. Chúc bạn ngon miệng!";
                case CANCELLED -> statusMsg = "đã bị hủy.";
            }
            notificationService.sendNotification(order.getUser(), 
                "Cập nhật đơn hàng #" + order.getId(), 
                "Đơn hàng của bạn " + statusMsg, 
                "ORDER", "/user/orders");

            // Refund stock if order is cancelled
            if (status == Order.OrderStatus.CANCELLED && oldStatus != Order.OrderStatus.CANCELLED) {
                inventoryService.refundStock(orderDetailRepository.findByOrder(savedOrder));
            }

            // Award points if order is completed and user exists
            if (status == Order.OrderStatus.COMPLETED && oldStatus != Order.OrderStatus.COMPLETED && order.getUser() != null) {
                double pointRate = systemSettingService.getPointRate();
                int points = order.getTotalAmount().divide(new java.math.BigDecimal(pointRate)).intValue();
                if (points > 0) {
                    userService.addPoints(order.getUser(), points, "Tích điểm từ đơn hàng #" + order.getId());
                }
            }            
            return savedOrder;
        }
        return null;
    }

    @Transactional
    public boolean cancelOrder(Long orderId, User user) {
        Order order = getOrderById(orderId);
        if (order != null && order.getUser().getId().equals(user.getId())) {
            // Only allow cancellation if status is PENDING or PROCESSING (if processing hasn't started yet)
            // But requirement says: "Khóa Hủy: Ngay khi nhân viên chuyển sang Đang xử lý, nút Hủy sẽ biến mất"
            // So on the service side, we check if it's PENDING.
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
                return true;
            }
        }
        return false;
    }
}
