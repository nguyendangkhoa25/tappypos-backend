package com.barbershop.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JwtTokenProvider - Generates, validates, and extracts information from JWT tokens
 * Compatible with JJWT 0.13.0
 * <p>
 * This component handles all JWT operations including:
 * - Token generation (access tokens)
 * - Refresh token generation
 * - Token validation and claims extraction
 * - Expiration checking
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationMs;

    /**
     * -- GETTER --
     *  Get refresh token expiration time in milliseconds
     *
     * @return refresh token expiration time in milliseconds
     */
    @Getter
    @Value("${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpirationMs;

    /**
     * Generate JWT access token with just username
     * Used for basic token generation
     *
     * @param username the username to encode in token
     * @return JWT token string
     */
    public String generateToken(String username) {
        return generateTokenWithClaims(username, new HashMap<>());
    }

    /**
     * Generate JWT token with additional claims
     * Allows passing extra information in the token payload
     *
     * @param username the username to encode in token
     * @param claims   additional claims to include in token
     * @return JWT token string
     */
    public String generateTokenWithClaims(String username, Map<String, Object> claims) {
        log.debug("Generating JWT token for user: {}", username);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Instant now = Instant.now();
        Instant expirationTime = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(key)
                .compact();
    }

    /**
     * Generate refresh token for "remember me" functionality
     * Refresh tokens have longer expiration (7 days vs 24 hours)
     *
     * @param username the username to encode in refresh token
     * @return refresh token string
     */
    public String generateRefreshToken(String username) {
        log.debug("Generating refresh token for user: {}", username);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Instant now = Instant.now();
        Instant expirationTime = now.plusMillis(refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(key)
                .compact();
    }

    /**
     * Extract username (subject) from token
     * Token must be valid, throws exception if invalid
     *
     * @param token JWT token string
     * @return username extracted from token
     */
    public String getUsernameFromToken(String token) {
        log.debug("Extracting username from token");
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extract expiration date from token
     * Token must be valid, throws exception if invalid
     *
     * @param token JWT token string
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        log.debug("Extracting expiration date from token");
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * Extract all claims from token
     * Validates signature before extracting claims
     *
     * @param token JWT token string
     * @return Claims object containing all token data
     */
    public Claims getClaimsFromToken(String token) {
        log.debug("Extracting claims from token");

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate JWT token
     * Checks signature and expiration without throwing exceptions
     *
     * @param token JWT token string to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            log.debug("Token validation successful");
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired
     * Returns true without throwing exceptions
     *
     * @param token JWT token string
     * @return true if token is expired, false if still valid
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true; // Consider expired if error occurs
        }
    }

    /**
     * Get access token expiration time in milliseconds
     *
     * @return expiration time in milliseconds
     */
    public long getTokenExpirationMs() {
        return jwtExpirationMs;
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}

