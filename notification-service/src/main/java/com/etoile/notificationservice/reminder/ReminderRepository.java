package com.etoile.notificationservice.reminder;

import com.etoile.notificationservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByKindAndRecipientAndRefKey(ReminderKind kind, Role recipient, String refKey);

    void deleteByKindAndRecipientAndRefKey(ReminderKind kind, Role recipient, String refKey);

    void deleteByKindAndRefKey(ReminderKind kind, String refKey);
}
