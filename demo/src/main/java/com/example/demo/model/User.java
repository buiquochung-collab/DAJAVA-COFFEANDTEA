package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.validation.constraints.NotBlank(message = "Vui lòng nhập tên đăng nhập của bạn")
    @Column(nullable = false, unique = true)
    private String username;

    @jakarta.validation.constraints.Size(min = 6, message = "Mật khẩu cần tối thiểu 6 ký tự để đảm bảo an toàn")
    @Column(nullable = false)
    private String password;

    @jakarta.validation.constraints.Email(message = "Địa chỉ email không đúng định dạng (Ví dụ: name@example.com)")
    @Column(unique = true)
    private String email;

    private String otpCode;
    private java.time.LocalDateTime otpExpiry;

    @jakarta.validation.constraints.Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$", 
            message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)")
    private String phone;
    private String fullName;
    private String gender;
    private String cccd;
    private java.time.LocalDate birthDate;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;

    // Membership fields
    @Builder.Default
    private Integer points = 0;
    @Builder.Default
    private String membershipRank = "MEMBER"; // MEMBER, VIP, DIAMOND
    private java.time.LocalDateTime pointsExpiry;

    private String status;
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER, ADMIN, CASHIER, STOCK_MANAGER
    }
}
