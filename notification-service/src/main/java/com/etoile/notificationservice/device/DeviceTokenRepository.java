package com.etoile.notificationservice.device;

import com.etoile.notificationservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByRole(Role role);

    long countByRole(Role role);

    void deleteByToken(String token);
}
