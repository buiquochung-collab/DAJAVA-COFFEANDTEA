package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            return userService.findByEmail(email);
        }
        return userService.findByUsername(authentication.getName());
    }

    @PostMapping("/notifications/read/{id}")
    public String markAsRead(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        notificationService.markAsRead(id);
        return "redirect:/";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        User user = getAuthenticatedUser(authentication);
        if (user != null) {
            notificationService.markAllAsRead(user);
        }
        return "redirect:/";
    }

    @GetMapping("/notifications/read-and-redirect/{id}")
    public String readAndRedirect(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        User user = getAuthenticatedUser(authentication);
        
        com.example.demo.model.Notification notification = notificationService.getNotificationById(id);
        if (notification != null && (notification.getUser() == null || (user != null && notification.getUser().getId().equals(user.getId())))) {
            notificationService.markAsRead(id);
            if (notification.getLink() != null && !notification.getLink().isEmpty()) {
                return "redirect:" + notification.getLink();
            }
        }
        return "redirect:/";
    }
}
