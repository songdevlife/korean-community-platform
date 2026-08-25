package com.dak.backend.controller;

import com.dak.backend.dto.DakSearchResult;
import com.dak.backend.dto.KakaoSkillResponse;
import com.dak.backend.service.DakSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    // Two guides and one update. The cap is the phone screen: a KakaoTalk bubble
    // showing more than three items is scrolled past rather than read. Guides get
    // the larger share because they are written to stay correct, while an update
    // is news and dates quickly.
    private static final int MAX_GUIDES = 2;
    private static final int MAX_UPDATES = 1;

    private static final int SUMMARY_LENGTH = 70;

    private final DakSearchService dakSearchService;

    public KakaoSkillController(DakSearchService dakSearchService) {
        this.dakSearchService = dakSearchService;
    }

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

                List<DakSearchResult> results;
                try {
                    results = dakSearchService.search(utterance, MAX_GUIDES, MAX_UPDATES);
                } catch (Exception e) {
                    // A search failure must not become a skill failure. Kakao reads a
                    // non-200 as the skill being broken and shows the fallback text, so
                    // a database hiccup would look identical to a misconfigured bot.
                    log.error("DAK search failed for a Kakao request", e);
                    results = List.of();
                }
        
                return ResponseEntity.ok(KakaoSkillResponse.simpleText(formatReply(results)));
            }
        
            /**
             * Turns search results into one KakaoTalk message.
             *
             * Plain text with bare URLs rather than cards: KakaoTalk linkifies a URL on
             * its own line, and a card layout is a second thing to get wrong before the
             * first one is known to work.
             */
            private String formatReply(List<DakSearchResult> results) {
        
                if (results.isEmpty()) {
                    return """
                            찾으시는 내용을 DAK에서 아직 찾지 못했습니다.
        
                            아래에서 직접 둘러보실 수 있습니다.
        
                            생활 가이드
                            https://discoveradelaidekorea.au/guides
        
                            호주 소식
                            https://discoveradelaidekorea.au/australia-updates
        
                            다른 단어로 다시 물어보셔도 됩니다.""";
                }
        
                StringBuilder sb = new StringBuilder("DAK에서 찾은 내용입니다.\n");
        
                boolean updateHeaderWritten = false;
                boolean guideHeaderWritten = false;
        
                for (DakSearchResult result : results) {
                    if (result.type() == DakSearchResult.Type.GUIDE && !guideHeaderWritten) {
                        sb.append("\n[생활 가이드]\n");
                        guideHeaderWritten = true;
                    } else if (result.type() == DakSearchResult.Type.UPDATE && !updateHeaderWritten) {
                        sb.append("\n[관련 소식]\n");
                        updateHeaderWritten = true;
                    }
        
                    sb.append("\n").append(result.title()).append("\n");
        
                    String summary = truncate(result.summary(), SUMMARY_LENGTH);
                    if (!summary.isEmpty()) {
                        sb.append(summary).append("\n");
                    }
        
                    sb.append(result.url()).append("\n");
                }
        
                // Guides state the date their facts were checked, and rules change after
                // publication. Sending someone to the article rather than answering from
                // it is the point of this stage.
                sb.append("\n자세한 내용은 링크에서 확인해 주세요.");
        
                return sb.toString();
            }
        
            private static String truncate(String text, int max) {
                if (text == null || text.isBlank()) {
                    return "";
                }
                String cleaned = text.strip().replaceAll("\\s+", " ");
                return cleaned.length() <= max ? cleaned : cleaned.substring(0, max) + "...";
            }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }

    private static String asString(Object value) {
        return (value == null) ? "" : String.valueOf(value);
    }
}