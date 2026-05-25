package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserVoucherRepository;
import com.example.demo.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BirthdayService {

    @Autowired private UserRepository userRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private UserVoucherRepository userVoucherRepository;
    @Autowired private NotificationService notificationService;

    // Chạy vào 1h sáng ngày đầu tiên của mỗi tháng
    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void sendBirthdayGifts() {
        int currentMonth = LocalDate.now().getMonthValue();
        List<User> birthdayUsers = userRepository.findAll().stream()
                .filter(u -> u.getBirthDate() != null && u.getBirthDate().getMonthValue() == currentMonth)
                .toList();

        if (birthdayUsers.isEmpty()) return;

        // Tạo Voucher tặng bánh (Giảm 100% cho 1 sản phẩm bánh)
        // Lưu ý: Logic áp dụng voucher cho 1 món cụ thể cần được xử lý ở Checkout
        // Ở đây ta tạo 1 voucher định danh cho sinh nhật
        Voucher birthdayVoucher = Voucher.builder()
                .code("HPBD" + currentMonth + "_" + System.currentTimeMillis() % 1000)
                .description("Quà tặng sinh nhật tháng " + currentMonth + ": Tặng 1 bánh ngọt miễn phí")
                .discountAmount(BigDecimal.valueOf(100))
                .discountType("PERCENT")
                .minOrderAmount(BigDecimal.ZERO)
                .startDate(LocalDateTime.now())
                .expiryDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(23, 59, 59))
                .terms("Áp dụng tặng 1 bánh ngọt bất kỳ trong tháng sinh nhật của bạn.")
                .build();
        
        voucherRepository.save(birthdayVoucher);

        for (User user : birthdayUsers) {
            userVoucherRepository.save(UserVoucher.builder()
                    .user(user)
                    .voucher(birthdayVoucher)
                    .assignedAt(LocalDateTime.now())
                    .used(false)
                    .build());

            notificationService.sendNotification(user, 
                "Chúc mừng sinh nhật khách hàng thân thiết!", 
                "Chào " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + 
                ", FANDB gửi tặng bạn 1 chiếc bánh ngọt miễn phí trong tháng sinh nhật này. Chúc bạn một ngày thật ý nghĩa!", 
                "SYSTEM", "/user/vouchers");
        }
    }
}
