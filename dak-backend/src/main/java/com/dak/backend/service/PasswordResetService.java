package com.dak.backend.service;

import com.dak.backend.domain.PasswordResetToken;
import com.dak.backend.domain.User;
import com.dak.backend.dto.ForgotPasswordRequest;
import com.dak.backend.dto.ResetPasswordRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.PasswordResetTokenRepository;
import com.dak.backend.repository.SessionRepository;
import com.dak.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Issuing and spending password reset tokens.
 *
 * The governing constraint is that none of this may reveal whether an address
 * is registered. Every response on the request side is identical, which means
 * an unknown address, a rate-limited repeat and a successful send are
 * indistinguishable from outside.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    // Long enough that a link left open in a tab still works, short enough that
    // an email sitting in a shared inbox stops being a key fairly quickly.
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    // A second request inside this window is answered normally and sends
    // nothing. Without it, one address can be used to send unlimited mail at
    // DAK's expense and to someone else's inbox.
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.site.base-url}")
    private String baseUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 SessionRepository sessionRepository,
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Always returns normally, whatever happened.
     *
     * An unknown address, an address that asked a moment ago, and a real send
     * all look the same to the caller. Reporting "no such account" here would
     * turn this endpoint into a way to test whether someone has registered.
     */
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();

        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(email);
        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for an address with no account");
            return;
        }

        User user = maybeUser.get();
        OffsetDateTime now = OffsetDateTime.now();

        // Cooldown, answered in the database: the rows already record when each
        // token was issued, so no counter is needed.
        boolean recentlySent = tokenRepository.existsRecentForUser(
            user.getId(), now.minus(RESEND_COOLDOWN));

    if (recentlySent) {
            log.info("Password reset for {} suppressed by cooldown", user.getId());
            return;
        }

        // Any link already in an inbox stops working now. Two live tokens would
        // mean an email delivered to the wrong place stays usable after the
        // owner has already reset.
        tokenRepository.invalidateAllForUser(user.getId(), now);

        String token = generateToken();
        tokenRepository.save(PasswordResetToken.createNew(
                user, hash(token), now.plus(TOKEN_LIFETIME)));

        String link = baseUrl + "/reset-password?token=" + token;
        boolean sent = emailService.send(user.getEmail(), "DAK 비밀번호 재설정", buildEmail(link));

        if (!sent) {
            // Local development, or a provider outage. The token is real and the
            // link works, so print it rather than leaving the flow untestable.
            log.info("Reset email not sent. Link for {}: {}", user.getEmail(), link);
        }
    }

    /**
     * Spends a token and sets the new password.
     *
     * Unlike the request side, this reports failure plainly: someone holding a
     * link needs to know whether it expired, and the token is already secret.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_RESET_TOKEN", "This reset link is not valid."));

        if (!token.isUsable()) {
            throw ApiException.badRequest("EXPIRED_RESET_TOKEN",
                    "This reset link has expired or has already been used.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        token.setUsedAt(OffsetDateTime.now());
        tokenRepository.invalidateAllForUser(user.getId(), OffsetDateTime.now());

        // Everything signed in as this user is signed out. The common reason to
        // reset a password is suspecting someone else has it, and leaving their
        // sessions alive would make the reset ceremonial.
        sessionRepository.revokeAllForUser(user.getId());

        log.info("Password reset completed for {}", user.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 rather than the password encoder. A reset token is 256 bits of
     * randomness with a thirty-minute life, so it is not guessable and does not
     * need the deliberate slowness bcrypt exists for — and it has to be looked
     * up by hash, which bcrypt's per-row salt makes impossible.
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String buildEmail(String link) {
        return """
                <div style="font-family:sans-serif;line-height:1.7;color:#1a1a1a">
                  <h2 style="font-size:18px">비밀번호 재설정</h2>
                  <p>DAK 계정의 비밀번호 재설정이 요청되었습니다.<br>
                     아래 버튼을 눌러 새 비밀번호를 설정해 주세요.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#1668c1;color:#fff;padding:12px 20px;
                       border-radius:8px;text-decoration:none;display:inline-block">
                      새 비밀번호 설정
                    </a>
                  </p>
                  <p style="font-size:13px;color:#666">
                     이 링크는 30분 후 만료되며 한 번만 사용할 수 있습니다.<br>
                     본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                     비밀번호는 변경되지 않습니다.</p>
                  <p style="font-size:13px;color:#666">
                     문의: admin@discoveradelaidekorea.au</p>
                </div>
                """.formatted(link);
    }
}