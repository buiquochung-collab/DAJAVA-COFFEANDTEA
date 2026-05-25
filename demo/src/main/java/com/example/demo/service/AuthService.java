package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    public String generateOtp(User user, String target, String channel) {
        // Tạo mã ngẫu nhiên 6 chữ số (từ 000000 đến 999999)
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        
        if ("PHONE".equalsIgnoreCase(channel)) {
            System.out.println("[SMS-SYSTEM] Mã OTP ngẫu nhiên: " + otp + " đang gửi tới: " + target);
        } else {
            try {
                org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
                message.setTo(target);
                message.setSubject("Mã xác thực OTP - FANDB Tea & Coffee");
                message.setText("Chào bạn,\n\nHệ thống FANDB vừa tạo mã xác thực OTP ngẫu nhiên cho yêu cầu của bạn.\n\nMã xác thực của bạn là: " + otp + "\n\nMã này có hiệu lực trong 5 phút. Vui lòng không cung cấp mã này cho bất kỳ ai.\n\nTrân trọng,\nFANDB Tea & Coffee Team");
                mailSender.send(message);
                System.out.println("[EMAIL-SYSTEM] Đã gửi mã OTP ngẫu nhiên đến: " + target);
            } catch (Exception e) {
                System.err.println("[ERROR] Lỗi hệ thống khi gửi mail: " + e.getMessage());
                System.out.println("[CONSOLE-DEBUG] OTP của bạn là: " + otp);
            }
        }
        return otp;
    }

    public String generateOtp(User user, String channel) {
        String target = "PHONE".equalsIgnoreCase(channel) ? user.getPhone() : user.getEmail();
        return generateOtp(user, target, channel);
    }

    public String generateOtp(User user) {
        return generateOtp(user, user.getEmail(), "EMAIL");
    }

    public boolean verifyOtp(User user, String code) {
        if (user.getOtpCode() != null && user.getOtpCode().equals(code) 
                && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
            // Xóa OTP sau khi xác thực thành công
            user.setOtpCode(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public void resetPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
