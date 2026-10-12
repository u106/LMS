package com.hosiyar.lms.user.config;

import com.hosiyar.lms.user.security.JwtAuthenticationEntryPoint;
import com.hosiyar.lms.user.security.JwtAuthenticationFilter;
import com.hosiyar.lms.user.security.RestAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Two styles of authorization are in play, deliberately:
 *
 *  - URL-level (here, via requestMatchers) - good for broad, path-shaped
 *    rules that apply to whole sections of the API.
 *  - Method-level (@PreAuthorize, see UserService.findAll()) - good when the
 *    rule belongs to the operation itself rather than to a URL.
 *
 * @EnableMethodSecurity is what makes the second style work at all. Without
 * it, @PreAuthorize is silently ignored - no error, just no protection.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless, token-based API - no server-rendered forms, so the
                // CSRF protection Security 7 enables by default isn't needed here.
                .csrf(AbstractHttpConfigurer::disable)

                // Picks up the CorsConfigurationSource bean from CorsConfig.
                // Without this line the bean exists but Spring Security ignores
                // it, and preflight OPTIONS requests are rejected before the
                // CORS filter ever runs.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Because we configure neither httpBasic nor formLogin, Spring
                // Security would otherwise default to answering 403 for every
                // rejection - even one with no credentials at all. These two
                // handlers restore the real distinction: 401 for "who are you",
                // 403 for "I know, and no".
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        // The demo UI, served from src/main/resources/static.
                        // Without this, Security's anyRequest() rule below
                        // demands a token for the HTML page itself and the
                        // browser gets a 401 instead of the app.
                        .requestMatchers(
                                "/", "/index.html", "/favicon.ico",
                                "/css/**", "/js/**", "/assets/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/users/register",
                                "/api/v1/ping",
                                "/actuator/health"
                        ).permitAll()
                        // URL-level example: listing every user is admin-only.
                        // UserService.findAll() also carries @PreAuthorize, so the
                        // rule holds even if some future controller calls it by
                        // another path.
                        .requestMatchers(HttpMethod.GET,"/","/index.html","/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")

                        // "My courses" is scoped by token, so it must be matched
                        // before the public GET rule below - order matters here.
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/me").authenticated()
                        // The catalogue, individual courses and their lesson lists
                        // are browsable by anyone. Draft visibility is enforced in
                        // the service layer, not here, because it depends on the
                        // specific row rather than the path.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/courses",
                                "/api/v1/courses/*",
                                "/api/v1/courses/*/lessons",
                                "/api/v1/courses/*/lessons/*/file").permitAll()

                        // Writes require the INSTRUCTOR role. Note this is only
                        // half the story - the role rule cannot tell whether the
                        // course is THIS instructor's. That check lives in
                        // CourseService.requireOwnedCourse().
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/**").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/**").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/lessons").hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/lessons/*/file").hasRole("INSTRUCTOR")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
