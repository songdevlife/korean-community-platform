package com.dak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the local Vite dev server (localhost:5173) to call this API.
 * Production must restrict this to the real frontend domain once deployed
 * (05 API Spec §2.9: "Production CORS configuration must use an approved
 * list of frontend domains").
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}