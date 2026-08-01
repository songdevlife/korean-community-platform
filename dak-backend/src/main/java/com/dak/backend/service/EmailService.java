package com.dak.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends transactional mail through Resend.
 *
 * Chosen over the alternatives for a permanent free tier that covers this
 * volume many times over, and set up on a sending subdomain so that the SPF
 * and MX records the domain already carries for receiving are untouched — a
 * second SPF record on the same name invalidates both, and the failure is
 * silent.
 *
 * Sending is best-effort by design. A caller that treats a failed send as a
 * failed operation would tell the world which addresses are registered, so
 * every method here returns rather than throws, and the failure is a log line.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String API_URL = "https://api.resend.com/emails";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.email.api-key:}")
    private String apiKey;

    @Value("${app.email.from:}")
    private String from;

    @Value("${app.email.reply-to:}")
    private String replyTo;

    @Value("${app.email.enabled:true}")
    private boolean enabled;

    /**
     * @return true if the provider accepted the message. False means it was not
     *         sent, for any reason — the caller decides whether that matters,
     *         and for password reset it deliberately does not.
     */
    public boolean send(String to, String subject, String html) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            // Not an error condition. Locally there is usually no key, and the
            // flow that calls this logs what it would have sent.
            log.info("Email sending disabled or unconfigured; skipping message to {}", to);
            return false;
        }

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("from", from);
            payload.put("subject", subject);
            payload.put("html", html);
            payload.set("to", objectMapper.createArrayNode().add(to));

            if (replyTo != null && !replyTo.isBlank()) {
                payload.put("reply_to", replyTo);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }

            // Body rather than status alone: Resend explains refusals, and an
            // unverified domain or a malformed from address both return 4xx.
            log.warn("Email send to {} refused with {}: {}",
                    to, response.statusCode(), response.body());
            return false;

        } catch (Exception e) {
            log.warn("Email send to {} failed: {}", to, e.getMessage());
            return false;
        }
    }
}