package com.dak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the local frontend dev servers to call this API.
 *
 * Two origins during the Next.js migration: 5173 is the Vite build being
 * replaced, 3000 the Next.js one replacing it. Both stay until the old project
 * is retired, since the two are run side by side to compare screens.
 *
 * Note that only browser-initiated requests need this. Next.js fetches public
 * content server-side, which is why the migration reached the login page before
 * CORS came up at all — that was the first request a browser made directly.
 *
 * Production must restrict this to the real frontend domain once deployed
 * (05 API Spec §2.9: "Production CORS configuration must use an approved
 * list of frontend domains").
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Origins come from configuration rather than being listed here, because
        // the deployed frontend's address is not knowable at compile time and
        // will change again when the real domain replaces the platform one.
        // Falls back to the local dev servers so nothing has to be set to run
        // this on a laptop.
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}