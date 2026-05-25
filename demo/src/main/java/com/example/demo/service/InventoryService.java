package com.example.demo.service;

import com.example.demo.model.Ingredient;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Product;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repository.IngredientRepository;
import com.example.demo.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class InventoryService {
    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecipeRepository recipeRepository;
    
    @Autowired
    private com.example.demo.repository.InventoryLogRepository inventoryLogRepository;

    @jakarta.transaction.Transactional
    public void restock(Long ingredientId, Double quantity, java.math.BigDecimal pricePerUnit, String supplier, User performer, String note) {
        Ingredient ing = ingredientRepository.findById(ingredientId).orElseThrow();
        
        // Tính toán giá vốn trung bình mới (Moving Average)
        java.math.BigDecimal currentStockValue = ing.getCostPerUnit().multiply(java.math.BigDecimal.valueOf(ing.getStockQuantity()));
        java.math.BigDecimal newStockValue = pricePerUnit.multiply(java.math.BigDecimal.valueOf(quantity));
        Double totalQuantity = ing.getStockQuantity() + quantity;
        
        if (totalQuantity > 0) {
            java.math.BigDecimal newAvgCost = currentStockValue.add(newStockValue).divide(java.math.BigDecimal.valueOf(totalQuantity), 2, java.math.RoundingMode.HALF_UP);
            ing.setCostPerUnit(newAvgCost);
        }
        
        ing.setStockQuantity(totalQuantity);
        ingredientRepository.save(ing);
        
        // Lưu nhật ký
        com.example.demo.model.InventoryLog log = com.example.demo.model.InventoryLog.builder()
                .ingredient(ing)
                .performer(performer)
                .type("IN")
                .quantity(quantity)
                .pricePerUnit(pricePerUnit)
                .supplier(supplier)
                .createdAt(java.time.LocalDateTime.now())
                .note(note)
                .build();
        inventoryLogRepository.save(log);
    }

    @Autowired
    private com.example.demo.repository.ProductRepository productRepository;

    @Transactional
    public void deductStock(List<OrderDetail> details) {
        for (OrderDetail detail : details) {
            Product product = detail.getProduct();
            if (product == null) continue;

            if (Boolean.TRUE.equals(product.getIsRecipeBased())) {
                // Recipe-based: deduct ingredients
                List<Recipe> recipes = recipeRepository.findByProduct(product);
                for (Recipe recipe : recipes) {
                    Ingredient ingredient = recipe.getIngredient();
                    if (ingredient != null) {
                        double totalNeeded = recipe.getQuantityRequired() * detail.getQuantity();
                        ingredient.setStockQuantity((ingredient.getStockQuantity() != null ? ingredient.getStockQuantity() : 0.0) - totalNeeded);
                        ingredientRepository.save(ingredient);
                    }
                }
            } else {
                // Direct-based: deduct product stock
                if (product.getStock() != null) {
                    product.setStock(product.getStock() - detail.getQuantity());
                    productRepository.save(product);
                }
            }
        }
    }

    @Transactional
    public void refundStock(List<OrderDetail> details) {
        for (OrderDetail detail : details) {
            Product product = detail.getProduct();
            if (product == null) continue;

            if (Boolean.TRUE.equals(product.getIsRecipeBased())) {
                List<Recipe> recipes = recipeRepository.findByProduct(product);
                for (Recipe recipe : recipes) {
                    Ingredient ingredient = recipe.getIngredient();
                    if (ingredient != null) {
                        double totalRefund = recipe.getQuantityRequired() * detail.getQuantity();
                        ingredient.setStockQuantity((ingredient.getStockQuantity() != null ? ingredient.getStockQuantity() : 0.0) + totalRefund);
                        ingredientRepository.save(ingredient);
                    }
                }
            } else {
                if (product.getStock() != null) {
                    product.setStock(product.getStock() + detail.getQuantity());
                    productRepository.save(product);
                }
            }
        }
    }

    public int getCalculatedStock(Product product) {
        if (Boolean.TRUE.equals(product.getIsRecipeBased())) {
            List<Recipe> recipes = recipeRepository.findByProduct(product);
            if (recipes.isEmpty()) return 0;
            
            double minStock = Double.MAX_VALUE;
            for (Recipe recipe : recipes) {
                Ingredient ingredient = recipe.getIngredient();
                if (ingredient == null || ingredient.getStockQuantity() == null || ingredient.getStockQuantity() <= 0) return 0;
                
                double possibleQty = ingredient.getStockQuantity() / recipe.getQuantityRequired();
                if (possibleQty < minStock) {
                    minStock = possibleQty;
                }
            }
            return (int) Math.floor(minStock);
        } else {
            return product.getStock() != null ? product.getStock() : 0;
        }
    }

    public List<Ingredient> getLowStockIngredients() {
        return ingredientRepository.findAll().stream()
                .filter(i -> i.getStockQuantity() != null && i.getStockQuantity() <= (i.getMinStockThreshold() != null ? i.getMinStockThreshold() : 5.0))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }
}
