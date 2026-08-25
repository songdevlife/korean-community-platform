package com.dak.backend.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds KakaoTalk skill response payloads (Kakao skill response format v2.0).
 *
 * NOTE - this deliberately does NOT use ApiResponse.
 *
 * Every other endpoint in this project returns the { success, data } envelope
 * from 05_API_Specification_DAK.docx 11.2. Kakao's servers cannot read that.
 * They require { version, template } at the top level of the body, and treat
 * anything else as a skill failure - which surfaces to the user as the fallback
 * block message, indistinguishable from a routing problem. Do not "tidy" this
 * into the standard envelope later.
 *
 * Keeping the shape in one class means that constraint lives in a single place.
 */
public final class KakaoSkillResponse {

    private static final String VERSION = "2.0";

    private KakaoSkillResponse() {
        // Utility class - not instantiable
    }

    /**
     * Simple text response. The only output type used in v1.1.
     */
    public static Map<String, Object> simpleText(String text) {
        Map<String, Object> simpleText = new LinkedHashMap<>();
        simpleText.put("text", text);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("simpleText", simpleText);

        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(output);

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("outputs", outputs);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", VERSION);
        response.put("template", template);
        return response;
    }

    /**
     * Immediate acknowledgement, used when the real answer will be pushed later
     * through Kakao's callback URL.
     *
     * Not wired up in v1.1. It exists now because the Kakao skill timeout is
     * 5 seconds and any Claude call will exceed it, so the callback flow becomes
     * mandatory from the AI step onward. Having the shape here means adding it
     * is a branch in the controller rather than a rewrite of the response layer.
     */
    public static Map<String, Object> callbackAck(String waitingMessage) {
        Map<String, Object> response = simpleText(waitingMessage);
        response.put("useCallback", true);
        return response;
    }
}