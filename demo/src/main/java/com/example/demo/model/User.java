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

    @jakarta.validation.constraints.NotBlank(message = "Tên đăng nhập không được để trống")
    @Column(nullable = false, unique = true)
    private String username;

    @jakarta.validation.constraints.Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Column(nullable = false)
    private String password;

    @jakarta.validation.constraints.Email(message = "Email không hợp lệ")
    @Column(unique = true)
    private String email;

    private String otpCode;
    private java.time.LocalDateTime otpExpiry;

    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]*$", message = "Số điện thoại chỉ được chứa chữ số")
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
