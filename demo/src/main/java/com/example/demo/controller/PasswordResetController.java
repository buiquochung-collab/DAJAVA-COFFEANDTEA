package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String identifier, 
                                       @RequestParam(defaultValue = "EMAIL") String channel,
                                       Model model, HttpSession session) {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);

        if (user == null) {
            model.addAttribute("error", "Không tìm thấy tài khoản với thông tin này.");
            return "forgot-password";
        }

        // Kiểm tra nếu chọn phone mà user chưa có phone
        if ("PHONE".equals(channel) && (user.getPhone() == null || user.getPhone().isEmpty())) {
            model.addAttribute("error", "Tài khoản của bạn chưa đăng ký số điện thoại.");
            return "forgot-password";
        }

        authService.generateOtp(user, channel);
        session.setAttribute("resetEmail", user.getUsername()); // Dùng username làm key định danh
        return "redirect:/auth/verify-otp";
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(HttpSession session, Model model) {
        if (session.getAttribute("resetEmail") == null) return "redirect:/auth/forgot-password";
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(@RequestParam String otp, HttpSession session, Model model) {
        String identifier = (String) session.getAttribute("resetEmail");
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);

        if (user != null && authService.verifyOtp(user, otp)) {
            session.setAttribute("otpVerified", true);
            return "redirect:/auth/reset-password";
        }

        model.addAttribute("error", "Mã OTP không chính xác hoặc đã hết hạn.");
        return "verify-otp";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(HttpSession session) {
        if (session.getAttribute("otpVerified") == null) return "redirect:/auth/forgot-password";
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String password, 
                                      @RequestParam String confirmPassword, 
                                      HttpSession session, 
                                      RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/auth/reset-password";
        }

        String identifier = (String) session.getAttribute("resetEmail");
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);

        if (user != null) {
            authService.resetPassword(user, password);
            session.invalidate();
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        return "redirect:/auth/forgot-password";
    }
}
