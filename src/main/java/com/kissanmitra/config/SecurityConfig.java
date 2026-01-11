package com.kissanmitra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;

    /**
     * Security filter chain configuration.
     *
     * <p>Business Context:
     * - Public endpoints: auth and discovery (no authentication required)
     * - All other endpoints require JWT authentication
     * - Stateless session management
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll() // Master data dropdowns
                        .requestMatchers("/public/**").permitAll()
                        // Actuator endpoints (for health checks and monitoring)
                        .requestMatchers("/actuator/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            final BaseClientResponse<?> errorResponse = BaseClientResponse.builder()
                                    .success(false)
                                    .message("Authentication required")
                                    .errorDetails(authException.getMessage())
                                    .correlationId(UUID.randomUUID().toString())
                                    .timestamp(Instant.now())
                                    .build();

                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            final BaseClientResponse<?> errorResponse = BaseClientResponse.builder()
                                    .success(false)
                                    .message("Access denied")
                                    .errorDetails(accessDeniedException.getMessage())
                                    .correlationId(UUID.randomUUID().toString())
                                    .timestamp(Instant.now())
                                    .build();

                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
