package com.kissanmitra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kissanmitra.response.BaseClientResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT authentication filter.
 *
 * <p>Extracts JWT token from Authorization header and validates it.
 * Sets authentication in SecurityContext for downstream filters and controllers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestPath = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header, let it continue (might be public endpoint)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // BUSINESS DECISION: If token is empty/blank, treat as unauthenticated
        // This allows public endpoints to work without token, and authenticated endpoints
        // will fail later if they require authentication
        if (token == null || token.isEmpty()) {
            // No token provided - continue as unauthenticated user
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtUtil.validateToken(token)) {
                // Store token as principal for UserContext to extract
                final UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(token, null, null);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } else {
                // Token is invalid - clear any existing authentication
                SecurityContextHolder.clearContext();
                
                // Token is invalid - for public endpoints, continue as unauthenticated
                // For protected endpoints, Spring Security will handle 401
                // Check if this is a public endpoint
                if (isPublicEndpoint(requestPath)) {
                    // Public endpoint - continue as unauthenticated
                    filterChain.doFilter(request, response);
                } else {
                    // Protected endpoint - return 401
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid or expired token", "Authentication failed");
                }
            }
        } catch (Exception e) {
            // Token parsing error - clear any existing authentication
            SecurityContextHolder.clearContext();
            
            // Token parsing error - for public endpoints, continue as unauthenticated
            if (isPublicEndpoint(requestPath)) {
                // Public endpoint - continue as unauthenticated (invalid token ignored)
                filterChain.doFilter(request, response);
            } else {
                // Protected endpoint - return 401
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Token validation failed", e.getMessage());
            }
        }
    }

    /**
     * Checks if the request path is a public endpoint.
     *
     * @param path request path
     * @return true if public endpoint
     */
    private boolean isPublicEndpoint(final String path) {
        return path != null && (
                path.startsWith("/api/v1/auth/") ||
                path.startsWith("/api/v1/public/") ||
                path.startsWith("/public/")
        );
    }

    private void sendErrorResponse(HttpServletResponse response, int status,
                                   String message, String details) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final BaseClientResponse<?> errorResponse = BaseClientResponse.builder()
                .success(false)
                .message(message)
                .errorDetails(details)
                .correlationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
