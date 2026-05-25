package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrder(Order order);

    @Query("SELECT od.product.id FROM OrderDetail od " +
           "WHERE od.order.orderDate >= :startDate " +
           "AND od.order.status = 'COMPLETED' " +
           "GROUP BY od.product.id " +
           "HAVING SUM(od.quantity) >= 100")
    List<Long> findTopSellingProductIds(@Param("startDate") LocalDateTime startDate);
}
