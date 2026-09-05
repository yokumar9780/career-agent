package com.careeragent.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures CORS settings for cross-origin requests from the frontend.
 */
@Configuration
public class CorsConfig {

    private final String allowedOrigins;
    private final String allowedMethods;
    private final String allowedHeaders;

    /**
     * Explicit constructor for @Value parameter injection (Rule 10).
     */
    public CorsConfig(
            @Value("${cors.allowed-origins:http://localhost:3000}") String allowedOrigins,
            @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}") String allowedMethods,
            @Value("${cors.allowed-headers:Authorization,Content-Type,X-Correlation-ID}") String allowedHeaders) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
    }

    /**
     * Creates the CORS configuration source with allowed origins, methods, and headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of(allowedMethods.split(",")));
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
