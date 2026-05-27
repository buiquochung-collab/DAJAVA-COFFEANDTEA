package com.example.demo.service;

import com.example.demo.model.Category;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.example.demo.repository.OrderDetailRepository orderDetailRepository;

    @Autowired
    private InventoryService inventoryService;

    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::enrichProduct);
        return products;
    }

    public Product getProductById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) enrichProduct(product);
        return product;
    }

    private void enrichProduct(Product product) {
        product.setSalePrice(product.getEffectivePrice());
        product.setCalculatedStock(inventoryService.getCalculatedStock(product));
    }

    public List<Product> getProductsByCategory(Category category) {
        List<Product> products = productRepository.findByCategory(category);
        products.forEach(this::enrichProduct);
        return products;
    }

    public List<Product> sortProducts(List<Product> products, String sort) {
        if (sort == null || sort.isEmpty()) return products;
        
        java.util.Comparator<Product> comparator = null;
        switch (sort) {
            case "price_asc":
                comparator = java.util.Comparator.comparing(Product::getSalePrice);
                break;
            case "price_desc":
                comparator = java.util.Comparator.comparing(Product::getSalePrice).reversed();
                break;
            case "popularity":
                // Bestseller first, then by ID desc
                comparator = (p1, p2) -> {
                    boolean isB1 = Boolean.TRUE.equals(p1.getBestSeller());
                    boolean isB2 = Boolean.TRUE.equals(p2.getBestSeller());
                    if (isB1 && !isB2) return -1;
                    if (!isB1 && isB2) return 1;
                    return p2.getId().compareTo(p1.getId());
                };
                break;
        }
        
        if (comparator != null) {
            products.sort(comparator);
        }
        return products;
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.isEmpty()) return getAllProducts();
        
        List<Product> startsWith = productRepository.findByNameStartingWithIgnoreCase(query);
        List<Product> contains = productRepository.findByNameContainingIgnoreCase(query);
        
        // Merge lists without duplicates, keeping 'startsWith' items first
        java.util.Set<Long> seenIds = new java.util.HashSet<>();
        List<Product> results = new java.util.ArrayList<>();
        
        for (Product p : startsWith) {
            if (seenIds.add(p.getId())) {
                enrichProduct(p);
                results.add(p);
            }
        }
        for (Product p : contains) {
            if (seenIds.add(p.getId())) {
                enrichProduct(p);
                results.add(p);
            }
        }
        
        return results;
    }

    public List<Product> getBestSellers() {
        // Trả về danh sách đã được đánh dấu là bestseller trong DB
        // Logic tự động cập nhật nên được tách ra thành một Scheduled Task hoặc gọi thủ công từ Admin
        List<Product> bestSellers = productRepository.findByBestSellerTrue();
        if (bestSellers.isEmpty()) {
            // Nếu trống, có thể lấy tạm một số sản phẩm mới nhất hoặc ngẫu nhiên để trang chủ không bị trống
            return productRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 4)).getContent();
        }
        bestSellers.forEach(this::enrichProduct);
        return bestSellers;
    }

    // @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?") // Chạy vào 0h mỗi ngày
    @org.springframework.transaction.annotation.Transactional
    public void updateBestSellers() {
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        List<Long> topSellingIds = orderDetailRepository.findTopSellingProductIds(thirtyDaysAgo);
        
        List<Product> currentBestSellers = productRepository.findByBestSellerTrue();
        for (Product p : currentBestSellers) {
            p.setBestSeller(false);
            productRepository.save(p);
        }
        
        for (Long id : topSellingIds) {
            productRepository.findById(id).ifPresent(p -> {
                p.setBestSeller(true);
                productRepository.save(p);
            });
        }
    }

    public Product saveProduct(Product product) {
        Product saved = productRepository.save(product);
        enrichProduct(saved);
        return saved;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
