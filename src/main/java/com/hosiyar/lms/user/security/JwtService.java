package com.hosiyar.lms.user.security;

import com.hosiyar.lms.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // Without this, an access token and a refresh token are structurally
    // identical - meaning someone could hand an access token to the refresh
    // endpoint and it would happily work. Tagging each token with its type
    // lets the refresh endpoint reject anything that isn't a refresh token.
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${lms.jwt.secret}") String secret,
            @Value("${lms.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${lms.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiryMs, TYPE_ACCESS);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiryMs, TYPE_REFRESH);
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    /**
     * Valid for authenticating a request. Refresh tokens deliberately fail
     * this check - they're only good at the refresh endpoint.
     */
    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))
                    && claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(User user, long expiryMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
