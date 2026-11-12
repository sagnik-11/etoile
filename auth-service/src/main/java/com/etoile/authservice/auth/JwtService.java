package com.etoile.authservice.auth;

import com.etoile.authservice.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expiryDays;

    public JwtService(JwtEncoder jwtEncoder, @Value("${jwt.expiry-days}") long expiryDays) {
        this.jwtEncoder = jwtEncoder;
        this.expiryDays = expiryDays;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("etoile-auth-service")
                .issuedAt(now)
                .expiresAt(now.plus(expiryDays, ChronoUnit.DAYS))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
