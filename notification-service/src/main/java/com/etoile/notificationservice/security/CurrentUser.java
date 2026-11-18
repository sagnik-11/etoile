package com.etoile.notificationservice.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record CurrentUser(UUID id, Role role, String name) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                Role.valueOf(jwt.getClaimAsString("role")),
                jwt.getClaimAsString("name"));
    }
}
