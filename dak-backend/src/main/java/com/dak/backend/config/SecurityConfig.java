package com.dak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/sitemap.xml").permitAll()
                // Search logging is open because most searches happen before
                // anyone signs in, and those are the ones worth knowing about.
                // Write-only: there is no GET on this path, so the log cannot be
                // read back through the API.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/search-logs")
                        .permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/business-categories", "/api/v1/business-categories/**").permitAll()

                // Password reset is open by necessity: someone who has forgotten
                // their password cannot authenticate to ask for a new one. Both
                // endpoints answer identically whatever they are given, so being
                // open does not make them a way to learn anything.
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                        "/api/v1/auth/verify-email").permitAll()
                        
                // Any signed-in user may submit a listing. Submissions land in
                // PENDING and an administrator reviews them, so the queue is the
                // spam control rather than the role. Requiring BUSINESS_OWNER
                // needed a role-upgrade path that does not exist, which left no
                // route to submit at all.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/businesses")
                        .authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/businesses", "/api/v1/businesses/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/update-categories", "/api/v1/update-categories/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/update-sources", "/api/v1/update-sources/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/australia-updates", "/api/v1/australia-updates/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/community-posts", "/api/v1/community-posts/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/australia-updates")
                        .hasRole("ADMINISTRATOR")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRATOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/update-sources").hasRole("ADMINISTRATOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/guides", "/api/v1/guides/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/guide-categories").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}