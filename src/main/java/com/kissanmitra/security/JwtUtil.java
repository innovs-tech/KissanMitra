package com.kissanmitra.security;

import com.kissanmitra.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JWT token generation and validation.
 *
 * <p>JWT Structure:
 * <ul>
 *   <li>sub: userId</li>
 *   <li>mobile: phone number</li>
 *   <li>roles: array of user roles</li>
 *   <li>activeRole: currently active role (nullable)</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_MOBILE = "mobile";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_ACTIVE_ROLE = "activeRole";

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") final String secret,
            @Value("${jwt.expiration}") final long expirationMs
    ) {
        this.key = createSecretKey(secret);
        this.expirationMs = expirationMs;
    }

    private SecretKey createSecretKey(final String secret) {
        try {
            // Try to decode as base64 first
            byte[] decoded = java.util.Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(decoded);
        } catch (IllegalArgumentException e) {
            // If not base64, generate a proper key using SHA-256 hash
            // For HS256, we need exactly 256 bits (32 bytes)
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
                return new SecretKeySpec(hash, "HmacSHA256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("Failed to create JWT secret key", ex);
            }
        }
    }

    /**
     * Generates a JWT token for a user.
     *
     * @param userId user ID (subject)
     * @param phoneNumber mobile phone number
     * @param roles list of user roles
     * @param activeRole currently active role (can be null)
     * @return JWT token string
     */
    public String generateToken(
            final String userId,
            final String phoneNumber,
            final List<UserRole> roles,
            final UserRole activeRole
    ) {
        final List<String> roleNames = roles.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_MOBILE, phoneNumber)
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_ACTIVE_ROLE, activeRole != null ? activeRole.name() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts user ID from JWT token.
     *
     * @param token JWT token
     * @return user ID
     */
    public String extractUserId(final String token) {
        final Claims claims = getClaims(token);
        return claims.get(CLAIM_USER_ID, String.class);
    }

    /**
     * Extracts phone number from JWT token.
     *
     * @param token JWT token
     * @return phone number
     */
    public String extractPhoneNumber(final String token) {
        final Claims claims = getClaims(token);
        return claims.get(CLAIM_MOBILE, String.class);
    }

    /**
     * Extracts roles from JWT token.
     *
     * @param token JWT token
     * @return list of user roles
     */
    @SuppressWarnings("unchecked")
    public List<UserRole> extractRoles(final String token) {
        final Claims claims = getClaims(token);
        final List<String> roleNames = claims.get(CLAIM_ROLES, List.class);
        if (roleNames == null) {
            return List.of();
        }
        return roleNames.stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Extracts active role from JWT token.
     *
     * @param token JWT token
     * @return active role, or null if not set
     */
    public UserRole extractActiveRole(final String token) {
        final Claims claims = getClaims(token);
        final String activeRoleName = claims.get(CLAIM_ACTIVE_ROLE, String.class);
        return activeRoleName != null ? UserRole.valueOf(activeRoleName) : null;
    }

    /**
     * Validates JWT token.
     *
     * @param token JWT token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(final String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a new token with updated active role.
     *
     * @param token existing JWT token
     * @param newActiveRole new active role
     * @return new JWT token with updated active role
     */
    public String updateActiveRole(final String token, final UserRole newActiveRole) {
        final Claims claims = getClaims(token);
        final String userId = claims.get(CLAIM_USER_ID, String.class);
        final String phoneNumber = claims.get(CLAIM_MOBILE, String.class);
        @SuppressWarnings("unchecked")
        final List<String> roleNames = claims.get(CLAIM_ROLES, List.class);
        final List<UserRole> roles = roleNames.stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toList());

        return generateToken(userId, phoneNumber, roles, newActiveRole);
    }

    private Claims getClaims(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
