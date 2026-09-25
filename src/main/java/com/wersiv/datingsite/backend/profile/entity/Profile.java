package com.wersiv.datingsite.backend.profile.entity;

import com.wersiv.datingsite.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Public profile data (handle, display name, bio, preferences, avatar)
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false, length = 32)
    private String handle;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(columnDefinition = "text")
    private String preferences;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Profile(User user, String handle, String displayName, String bio, String preferences) {
        this.user = user;
        this.handle = handle;
        this.displayName = displayName;
        this.bio = bio;
        this.preferences = preferences;
    }

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
