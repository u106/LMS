package com.hosiyar.lms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Lets a browser front end served from a different origin call this API.
 *
 * Browsers refuse cross-origin requests unless the server says otherwise, and
 * a page on localhost:5500 calling localhost:8080 counts as cross-origin -
 * a different port is a different origin. Without this, every fetch from the
 * demo UI fails before it reaches a controller.
 *
 * Origins are hardcoded rather than read from a property, because the previous
 * approach (@Value on a List<String>) requires a comma-separated value in
 * application.yml and was causing startup failures when the file sync missed
 * that line. These are all localhost variants used during development only.
 *
 * Origins are listed explicitly rather than using "*", because credentials
 * are allowed and the two cannot be combined.
 */
@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://localhost:3000",
            "http://localhost:8000",
            "http://localhost:8080"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
