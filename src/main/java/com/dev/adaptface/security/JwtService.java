package com.dev.adaptface.security;

import com.dev.adaptface.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @PostConstruct
    void validateConfiguration() {
        signingKey();
        if (expiration <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION deve ser maior que zero");
        }
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("userType", user.getUserType().name())
                .claim("authVersion", user.getAuthVersion())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey())
                .compact();
    }

    public String extractSubject(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token, User user) {
        Claims claims = claims(token);
        Object id = claims.get("userId");
        Object version = claims.get("authVersion");
        long tokenVersion = version == null ? 0 : version instanceof Number number ? number.longValue() : -1;
        return id instanceof Number number && user.getId() != null && number.longValue() == user.getId()
                && user.getEmail().equals(claims.getSubject())
                && user.getUserType().name().equals(claims.get("userType"))
                && tokenVersion == user.getAuthVersion()
                && claims.getExpiration() != null && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
