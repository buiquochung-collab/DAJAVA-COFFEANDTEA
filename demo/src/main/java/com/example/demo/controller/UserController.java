package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;

    @Autowired
    private com.example.demo.service.OrderService orderService;

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            return userService.findByEmail(email);
        }
        return userService.findByUsername(authentication.getName());
    }

    @GetMapping("/user/orders")
    public String orderHistory(@RequestParam(required = false) Long orderId,
                              @RequestParam(required = false) String date,
                              Authentication authentication, 
                              Model model) {
        User user = getAuthenticatedUser(authentication);
        model.addAttribute("orders", orderService.searchOrders(user, orderId, date));
        model.addAttribute("searchOrderId", orderId);
        model.addAttribute("searchDate", date);
        return "order-history";
    }

    @PostMapping("/user/orders/cancel")
    public String cancelOrder(@RequestParam Long orderId, Authentication authentication, RedirectAttributes redirectAttributes) {
        orderService.cancelOrder(orderId, getAuthenticatedUser(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng thành công");
        return "redirect:/user/orders";
    }

    @Autowired
    private com.example.demo.service.AuthService authService;

    @PostMapping("/user/profile/request-email-otp")
    @ResponseBody
    public String requestEmailOtp(@RequestParam String email, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (userRepository.findByEmail(email).isPresent()) {
            return "Email này đã được sử dụng bởi tài khoản khác.";
        }
        // Gửi OTP đến đúng Email mới mà khách vừa gõ vào
        authService.generateOtp(user, email, "EMAIL");
        return "success";
    }

    @PostMapping("/user/profile/verify-email-otp")
    public String verifyEmailOtp(@RequestParam String email, @RequestParam String otp, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(authentication);
        if (authService.verifyOtp(user, otp)) {
            user.setEmail(email);
            userService.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Xác thực và đồng bộ Email thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không chính xác hoặc đã hết hạn.");
        }
        return "redirect:/user/profile";
    }

    @GetMapping("/user/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("user", getAuthenticatedUser(authentication));
        return "profile"; 
    }

    @PostMapping("/user/profile/update")
    public String updateProfile(@ModelAttribute @Valid User updatedUser, BindingResult bindingResult, Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", updatedUser);
            return "profile";
        }
        
        User user = getAuthenticatedUser(authentication);
        user.setFullName(updatedUser.getFullName());
        user.setPhone(updatedUser.getPhone());
        user.setGender(updatedUser.getGender());
        user.setCccd(updatedUser.getCccd());
        user.setBirthDate(updatedUser.getBirthDate());
        user.setProvince(updatedUser.getProvince());
        user.setDistrict(updatedUser.getDistrict());
        user.setWard(updatedUser.getWard());
        user.setAddressDetail(updatedUser.getAddressDetail());
        
        userService.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công");
        return "redirect:/user/profile";
    }

    @GetMapping("/user/membership")
    public String userMembership(Authentication authentication, Model model) {
        User user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("pointHistory", userService.getPointHistory(user));
        return "user-membership";
    }

    @GetMapping("/user/vouchers")
    public String userVouchers(Authentication authentication, Model model) {
        User user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        // userVouchers đã được GlobalControllerAdvice nạp vào Model nên không cần nạp lại ở đây
        return "user-vouchers";
    }

    @GetMapping("/membership")
    public String membership() {
        return "membership";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") @Valid User user, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        if (userService.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại");
            return "register";
        }
        
        // Lưu toàn bộ thông tin cơ bản
        User newUser = User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status("ACTIVE")
                .role(User.Role.USER)
                .membershipRank("MEMBER")
                .points(0)
                .build();
                
        userService.register(newUser);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}
