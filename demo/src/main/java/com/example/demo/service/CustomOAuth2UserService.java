package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        if (email == null) {
            String id = oauth2User.getAttribute("id");
            email = id + "@" + registrationId + ".com";
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            // Trường hợp tài khoản đã tồn tại với email này (tài khoản A)
            User existingUser = userOpt.get();
            // Cập nhật họ tên nếu tài khoản cũ chưa có
            if (existingUser.getFullName() == null || existingUser.getFullName().isEmpty()) {
                existingUser.setFullName(name);
                userRepository.save(existingUser);
            }
            System.out.println("Đã liên kết đăng nhập Google với tài khoản hiện có: " + existingUser.getUsername());
        } else {
            // Trường hợp chưa có tài khoản nào dùng email này -> Tạo mới
            String username = email;
            // Đảm bảo username không bị trùng (dù email chưa trùng)
            if (userRepository.findByUsername(username).isPresent()) {
                username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);
            }

            User user = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(name)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(User.Role.USER)
                    .status("ACTIVE")
                    .membershipRank("MEMBER")
                    .points(0)
                    .build();
            userRepository.save(user);
            System.out.println("Đã tạo tài khoản mới từ đăng nhập Google: " + username);
        }

        return oauth2User;
    }
}
