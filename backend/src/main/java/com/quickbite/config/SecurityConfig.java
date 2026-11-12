package com.quickbite.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.common.dto.ErrorResponse;
import com.quickbite.security.FirebaseTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
            FirebaseTokenFilter firebaseTokenFilter,
            ObjectMapper objectMapper,
            @Value("${quickbite.cors.allowed-origin-patterns:*}") String allowedOriginPatternsRaw) {
        this.firebaseTokenFilter = firebaseTokenFilter;
        this.objectMapper = objectMapper;
        List<String> parsed = Arrays.stream((allowedOriginPatternsRaw != null ? allowedOriginPatternsRaw : "*").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        this.allowedOriginPatterns = parsed.isEmpty() ? List.of("*") : parsed;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        // Public / Health / Firebase-handshake Endpoints
                        .requestMatchers(
                                "/health",
                                "/api/v1/health",
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/api/v1/payments/webhook",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/marketplace/shops/**", "/api/v1/marketplace/products/**").permitAll()

                        // Role-specific endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/storage/**").hasAnyRole("SHOP_OWNER", "ADMIN")
                        .requestMatchers("/api/v1/marketplace/owner/**", "/api/v1/orders/shop/**").hasRole("SHOP_OWNER")
                        .requestMatchers("/api/v1/delivery/**").hasRole("DELIVERY_PARTNER")
                        .requestMatchers("/api/v1/orders/checkout", "/api/v1/orders/buyer/**").hasRole("BUYER")

                        // Authenticated endpoints
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    authException != null && authException.getMessage() != null
                            ? authException.getMessage()
                            : "Authentication required. Please sign in with Firebase.",
                    request.getRequestURI()
            );
            objectMapper.writeValue(response.getWriter(), error);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
