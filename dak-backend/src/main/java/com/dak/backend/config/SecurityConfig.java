package com.dak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/business-categories", "/api/v1/business-categories/**").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/businesses")
                        .hasAnyRole("BUSINESS_OWNER", "ADMINISTRATOR")
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
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}