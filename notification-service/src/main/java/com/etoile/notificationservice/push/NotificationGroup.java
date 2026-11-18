package com.etoile.notificationservice.push;

import com.etoile.notificationservice.security.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_groups",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipient", "group_key"}))
public class NotificationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role recipient;

    @Column(name = "group_key", nullable = false, length = 100)
    private String groupKey;

    @Column(name = "unseen_count", nullable = false)
    private int unseenCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationGroup() {
    }

    public NotificationGroup(Role recipient, String groupKey) {
        this.recipient = recipient;
        this.groupKey = groupKey;
        this.updatedAt = Instant.now();
    }

    public int bump() {
        unseenCount++;
        updatedAt = Instant.now();
        return unseenCount;
    }

    public int getUnseenCount() { return unseenCount; }
}
