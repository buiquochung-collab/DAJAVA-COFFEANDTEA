package com.example.demo.repository;

import com.example.demo.model.InventoryLog;
import com.example.demo.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByIngredientOrderByCreatedAtDesc(Ingredient ingredient);
    List<InventoryLog> findAllByOrderByCreatedAtDesc();
}
