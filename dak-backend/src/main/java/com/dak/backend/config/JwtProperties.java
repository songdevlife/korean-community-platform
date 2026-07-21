package com.dak.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the app.jwt.* values from application.yml / application-dev.yml.
 * See application.yml: access-token-expiry-minutes, refresh-token-expiry-days, secret (in application-dev.yml).
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiryMinutes;
    private long refreshTokenExpiryDays;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenExpiryMinutes() { return accessTokenExpiryMinutes; }
    public void setAccessTokenExpiryMinutes(long v) { this.accessTokenExpiryMinutes = v; }

    public long getRefreshTokenExpiryDays() { return refreshTokenExpiryDays; }
    public void setRefreshTokenExpiryDays(long v) { this.refreshTokenExpiryDays = v; }
}