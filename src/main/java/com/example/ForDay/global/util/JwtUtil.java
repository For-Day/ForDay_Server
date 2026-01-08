package com.example.ForDay.global.util;

import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Long accessExpiration;
    private final Long refreshExpiration;

    public JwtUtil(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.access-expiration}") Long accessExpiration,
            @Value("${spring.jwt.refresh-expiration}") Long refreshExpiration
    ) {
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );

        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return getClaims(token).get("username", String.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public String createAccessToken(String username, Role role, SocialType socialType) {
        return Jwts.builder()
                .claim("username", username)
                .claim("role", role)
                .claim("socialType", socialType)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis() +
                                        accessExpiration * 60 * 1000
                        )
                )
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String username) {
        return Jwts.builder()
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis() +
                                        refreshExpiration * 24 * 60 * 60 * 1000
                        )
                )
                .signWith(secretKey)
                .compact();
    }

    public boolean validate(String token) {
        try {
            return !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
