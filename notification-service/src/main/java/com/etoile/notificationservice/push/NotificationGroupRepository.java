package com.etoile.notificationservice.push;

import com.etoile.notificationservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationGroupRepository extends JpaRepository<NotificationGroup, UUID> {

    Optional<NotificationGroup> findByRecipientAndGroupKey(Role recipient, String groupKey);

    void deleteByRecipientAndGroupKey(Role recipient, String groupKey);
}
