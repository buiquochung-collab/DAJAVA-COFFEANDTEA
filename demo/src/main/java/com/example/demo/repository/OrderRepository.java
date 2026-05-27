package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"orderDetails", "orderDetails.product"})
    List<Order> findByUserOrderByOrderDateDesc(User user);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"orderDetails", "orderDetails.product"})
    List<Order> findByUserAndId(User user, Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"orderDetails", "orderDetails.product"})
    List<Order> findByUserAndOrderDateBetween(User user, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"orderDetails", "orderDetails.product"})
    @Override
    java.util.Optional<Order> findById(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"orderDetails", "orderDetails.product", "orderDetails.product.category"})
    List<Order> findByOrderDateBetweenAndStatus(java.time.LocalDateTime start, java.time.LocalDateTime end, Order.OrderStatus status);
}
