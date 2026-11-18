package com.etoile.notificationservice.love;

import com.etoile.notificationservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoveStateRepository extends JpaRepository<LoveState, UUID> {

    Optional<LoveState> findByRecipient(Role recipient);
}
