package com.shopsphere.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final String TYPE_CLAIM = "type";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String build(String email, String type, long ttlMillis) {
        return Jwts.builder()
                .subject(email)
                .claim(TYPE_CLAIM, type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(String email) {
        return build(email, ACCESS, expiration);
    }

    public String generateRefreshToken(String email) {
        return build(email, REFRESH, refreshExpiration);
    }

    /** Verifies signature + expiry and returns the claims (throws JwtException). */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return ACCESS.equals(claims.get(TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return REFRESH.equals(claims.get(TYPE_CLAIM, String.class));
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }
}
