package com.kissanmitra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kissanmitra.response.BaseClientResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header, let it continue (might be public endpoint)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (jwtUtil.validateToken(token)) {
                // Store token as principal for UserContext to extract
                final UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(token, null, null);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } else {
                // Token is invalid - return 401 Unauthorized with proper error message
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired token", "Authentication failed");
            }
        } catch (Exception e) {
            // Any error during token validation - return 401
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token validation failed", e.getMessage());
        }
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
