package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.service.ProductService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReviewController {
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            return userService.findByEmail(email);
        }
        return userService.findByUsername(authentication.getName());
    }

    @PostMapping("/review/add")
    public String addReview(@RequestParam Long productId, 
                           @RequestParam Integer rating, 
                           @RequestParam String comment, 
                           org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        User user = getAuthenticatedUser(authentication);
        Product product = productService.getProductById(productId);
        
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(rating)
                .comment(comment)
                .createdAt(java.time.LocalDateTime.now())
                .status("APPROVED") // Tự động duyệt hoặc để nhân viên duyệt sau
                .build();
        reviewService.saveReview(review);
        return "redirect:/product/" + productId;
    }
}
