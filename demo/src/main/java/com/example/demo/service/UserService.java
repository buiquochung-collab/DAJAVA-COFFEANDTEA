package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.demo.repository.PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.USER);
        user.setStatus("ACTIVE");
        user.setPoints(0);
        user.setMembershipRank("MEMBER");
        return userRepository.save(user);
    }

    @jakarta.transaction.Transactional
    public void addPoints(User user, int pointsToAdd, String description) {
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(currentPoints + pointsToAdd);
        updateMembershipRank(user);
        userRepository.save(user);

        // Record history
        com.example.demo.model.PointHistory history = com.example.demo.model.PointHistory.builder()
                .user(user)
                .amount(pointsToAdd)
                .description(description)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        pointHistoryRepository.save(history);
    }

    public List<com.example.demo.model.PointHistory> getPointHistory(User user) {
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @jakarta.transaction.Transactional
    public boolean deductPoints(User user, int pointsToDeduct, String description) {
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (currentPoints >= pointsToDeduct) {
            user.setPoints(currentPoints - pointsToDeduct);
            userRepository.save(user);

            // Record history
            com.example.demo.model.PointHistory history = com.example.demo.model.PointHistory.builder()
                    .user(user)
                    .amount(-pointsToDeduct)
                    .description(description)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            pointHistoryRepository.save(history);
            return true;
        }
        return false;
    }

    private void updateMembershipRank(User user) {
        int points = user.getPoints();
        if (points >= 3000) {
            user.setMembershipRank("DIAMOND");
        } else if (points >= 1000) {
            user.setMembershipRank("VIP");
        } else {
            user.setMembershipRank("MEMBER");
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @jakarta.transaction.Transactional
    public void toggleUserStatus(Long id) {
        User user = findById(id);
        if (user != null) {
            if ("LOCKED".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
            } else {
                user.setStatus("LOCKED");
            }
            userRepository.save(user);
        }
    }

    public Optional<User> findByUsernameOrPhone(String key) {
        return userRepository.findByUsername(key)
            .or(() -> userRepository.findAll().stream()
                .filter(u -> key.equals(u.getPhone()))
                .findFirst());
    }

    public long count() {
        return userRepository.count();
    }
}
