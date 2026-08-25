package com.dak.backend.controller;

import com.dak.backend.dto.KakaoSkillResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Entry point for KakaoTalk chatbot skill requests.
 *
 * v1.1 scope: accept the Kakao POST, pull out the fields later stages need, and
 * return a fixed response. No DAK content lookup, no Claude call, no persistence.
 * The point is to prove the round trip KakaoTalk -> Chatbot Manager -> Render.
 *
 * Public by necessity: Kakao's servers send no credentials. SecurityConfig opens
 * POST on this path only. Abuse control belongs inside this controller (rate
 * limiting, payload validation) rather than in the filter chain.
 *
 * Returns the raw Kakao payload, NOT ApiResponse - see KakaoSkillResponse.
 */
@RestController
@RequestMapping("/api/v1/kakao")
public class KakaoSkillController {

    private static final Logger log = LoggerFactory.getLogger(KakaoSkillController.class);

    @PostMapping(
            value = "/skill",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> handleSkill(@RequestBody Map<String, Object> payload) {

        String utterance = "";
        String botUserKey = "";
        String callbackUrl = "";

        try {
            Map<String, Object> userRequest = asMap(payload.get("userRequest"));
            if (userRequest != null) {
                utterance = asString(userRequest.get("utterance"));
                callbackUrl = asString(userRequest.get("callbackUrl"));

                Map<String, Object> user = asMap(userRequest.get("user"));
                if (user != null) {
                    // Pseudonymous per-channel identifier. Hash this before any
                    // long-term storage, per the planned privacy controls.
                    botUserKey = asString(user.get("id"));
                }
            }
        } catch (Exception e) {
            // Nothing may escape this method. GlobalExceptionHandler would turn
            // it into a 500 wrapped in the standard envelope, and Kakao reads
            // that as a skill failure - the user then sees the fallback text and
            // the real cause is invisible from the chat.
            log.warn("Failed to parse Kakao skill payload", e);
        }

        // Utterance content is not logged. Logging it would start collecting user
        // messages, which requires the privacy-policy.md changes first.
        log.info("Kakao skill request. utteranceLength={}, hasUser={}, callbackAvailable={}",
                utterance.length(),
                !botUserKey.isEmpty(),
                !callbackUrl.isEmpty());

        String reply = "DAK 챗봇 연결에 성공했습니다.\n\n"
                + "받은 메시지: " + (utterance.isEmpty() ? "(없음)" : utterance) + "\n\n"
                + "현재는 연결 테스트 단계이며, 콘텐츠 검색 기능은 준비 중입니다.";

        return ResponseEntity.ok(KakaoSkillResponse.simpleText(reply));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }

    private static String asString(Object value) {
        return (value == null) ? "" : String.valueOf(value);
    }
}