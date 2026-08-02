package com.dak.backend.service;

import com.dak.backend.config.TokenHasher;
import com.dak.backend.repository.EmailVerificationTokenRepository;
import com.dak.backend.domain.EmailVerificationToken;
import com.dak.backend.domain.User;
import com.dak.backend.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Confirms that a registered address can actually be read.
 *
 * Without this, a typo at sign-up produces an account whose only route back in
 * - password reset - goes to an address nobody holds. Nothing is blocked by
 * being unverified: an account that cannot be used is a worse outcome than one
 * whose address is unconfirmed, and the reason to have this is to catch the
 * mistake early rather than to gate anything on it.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    // Days rather than minutes. A verification email may sit unread until
    // someone next opens their inbox, and an expired link costs a resend.
    private static final Duration TOKEN_LIFETIME = Duration.ofDays(3);

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final com.dak.backend.config.TokenHasher tokenHasher;

    @Value("${app.site.base-url}")
    private String baseUrl;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
        EmailService emailService,
        com.dak.backend.config.TokenHasher tokenHasher) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.tokenHasher = tokenHasher;
        }

    /**
     * Issues a token and sends the link.
     *
     * Never throws. Registration must not fail because mail did not go out -
     * the account is created either way, and the person can ask for another
     * link from their account page.
     */
    @Transactional
    public void sendVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (tokenRepository.existsRecentForUser(user.getId(), now.minus(RESEND_COOLDOWN))) {
            log.info("Verification email for {} suppressed by cooldown", user.getId());
            return;
        }

        tokenRepository.invalidateAllForUser(user.getId(), now);

        String token = tokenHasher.generateRawToken();
        tokenRepository.save(EmailVerificationToken.createNew(
                user, tokenHasher.hash(token), now.plus(TOKEN_LIFETIME)));
        String link = baseUrl + "/verify-email?token=" + token;
        boolean sent = emailService.send(user.getEmail(), "DAK 이메일 주소 확인", buildEmail(link));

        if (!sent) {
            log.info("Verification email not sent. Link for {}: {}", user.getEmail(), link);
        }
    }

    /**
     * Spends a token and marks the address confirmed.
     *
     * Reports failure plainly, as password reset does: whoever holds the link
     * already holds the secret, and needs to know whether to ask for another.
     */
    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository
                .findByTokenHash(tokenHasher.hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_VERIFICATION_TOKEN", "This verification link is not valid."));

        if (!token.isUsable()) {
            throw ApiException.badRequest("EXPIRED_VERIFICATION_TOKEN",
                    "This verification link has expired or has already been used.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(OffsetDateTime.now());

        log.info("Email verified for {}", user.getId());
    }

    private String buildEmail(String link) {
        return """
                <div style="font-family:sans-serif;line-height:1.7;color:#1a1a1a">
                  <h2 style="font-size:18px">이메일 주소 확인</h2>
                  <p>DAK 가입을 환영합니다.<br>
                     아래 버튼을 눌러 이메일 주소를 확인해 주세요.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#1668c1;color:#fff;padding:12px 20px;
                       border-radius:8px;text-decoration:none;display:inline-block">
                      이메일 주소 확인
                    </a>
                  </p>
                  <p style="font-size:13px;color:#666">
                     이 링크는 3일 후 만료됩니다.<br>
                     확인하지 않아도 DAK를 이용하실 수 있지만, 비밀번호를 잊으셨을 때
                     이 주소로만 재설정이 가능합니다.</p>
                  <p style="font-size:13px;color:#666">
                     본인이 가입하지 않으셨다면 이 메일을 무시하셔도 됩니다.<br>
                     문의: admin@discoveradelaidekorea.au</p>
                </div>
                """.formatted(link);
    }
}