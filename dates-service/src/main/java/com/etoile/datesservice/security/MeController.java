package com.etoile.datesservice.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public CurrentUser me(@AuthenticationPrincipal Jwt jwt) {
        return CurrentUser.from(jwt);
    }
}
