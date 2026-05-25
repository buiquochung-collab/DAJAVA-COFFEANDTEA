package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.CategoryService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.example.demo.repository.UserVoucherRepository userVoucherRepository;

    @ModelAttribute
    public void addAttributes(Model model, Authentication authentication, jakarta.servlet.http.HttpSession session) {
        model.addAttribute("categories", categoryService.getAllCategories());
        
        // Add cart item count
        java.util.List<?> cart = (java.util.List<?>) session.getAttribute("cart_items");
        model.addAttribute("cartItemCount", cart != null ? cart.size() : 0);
        
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            User user = null;
            
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String email = oauth2User.getAttribute("email");
                if (email != null) {
                    user = userService.findByEmail(email);
                }
            } else {
                user = userService.findByUsername(authentication.getName());
            }
            
            if (user != null) {
                model.addAttribute("currentUser", user);
                model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCount(user));
                
                java.util.List<com.example.demo.model.Notification> allNotifications = notificationService.getNotificationsForUser(user);
                java.util.List<com.example.demo.model.Notification> orderNotifications = allNotifications.stream()
                    .filter(n -> "ORDER".equals(n.getType()))
                    .toList();
                java.util.List<com.example.demo.model.Notification> newsNotifications = allNotifications.stream()
                    .filter(n -> !"ORDER".equals(n.getType()))
                    .toList();
                    
                model.addAttribute("orderNotifications", orderNotifications);
                model.addAttribute("newsNotifications", newsNotifications);
                
                model.addAttribute("userVouchers", userVoucherRepository.findByUserAndUsedFalse(user));
            }
        }
    }
}
