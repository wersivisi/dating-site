package com.wersiv.datingsite.backend.user.entity;

import com.wersiv.datingsite.backend.user.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.OFFLINE;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "match_notifications", nullable = false)
    private boolean matchNotifications = true;

    @Column(name = "message_notifications", nullable = false)
    private boolean messageNotifications = true;

    @Column(name = "email_news", nullable = false)
    private boolean emailNews = false;

    @Column(name = "show_online", nullable = false)
    private boolean showOnline = true;

    @Column(name = "show_distance", nullable = false)
    private boolean showDistance = true;

    @Column(name = "paused", nullable = false)
    private boolean paused = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
