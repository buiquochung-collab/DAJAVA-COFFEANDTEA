package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;

    @Builder.Default
    private boolean isRead = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // null for global notifications

    private String type; // ORDER, PROMOTION, SYSTEM, etc.
    
    private String link; // Optional link to redirect when clicked
}
