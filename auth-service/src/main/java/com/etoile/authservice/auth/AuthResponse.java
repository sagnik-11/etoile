package com.etoile.authservice.auth;

import com.etoile.authservice.user.Role;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String displayName,
        Role role
) {
}
