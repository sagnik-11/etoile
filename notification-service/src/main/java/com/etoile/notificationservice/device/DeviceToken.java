package com.etoile.notificationservice.device;

import com.etoile.notificationservice.security.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() {
    }

    public DeviceToken(String token, Role role, UUID userId) {
        this.token = token;
        assignTo(role, userId);
    }

    public void assignTo(Role role, UUID userId) {
        this.role = role;
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public String getToken() { return token; }
    public Role getRole() { return role; }
}
