package com.dak.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Real summarisation, via Anthropic's messages API.
 *
 * Judges relevance and drafts the Korean briefing in one call: the article text
 * has to be sent either way, so asking twice would double the cost for nothing.
 *
 * Marked @Primary so it wins over StubAiSummarizationService whenever it is
 * registered. Registration itself is conditional on a key being configured —
 * see the guard in summarize(), which falls back rather than failing.
 */
@Service
@Primary
public class ClaudeSummarizationService implements AiSummarizationService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeSummarizationService.class);

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    // Long enough for a briefing of a few paragraphs. The prompt asks for
    // brevity; this is the ceiling, not the target.
    private static final int MAX_TOKENS = 2000;

    // Articles are truncated before sending. Fetching already caps body text,
    // but a second bound here keeps one unusually long page from dominating a
    // day's spend.
    private static final int MAX_INPUT_CHARS = 12_000;

    /**
     * The whole design rests on this instruction. Two things it must get right:
     * the relevance test, which decides what an administrator ever sees, and the
     * drafting rule, which is what keeps DAK reporting on articles rather than
     * republishing them.
     */
    private static final String SYSTEM_PROMPT = """
            You are an editorial assistant for DAK, a local-information platform \
            for Korean speakers living in Adelaide, South Australia. Its readers \
            are permanent residents, students, working-holiday visa holders and \
            recent arrivals who mostly do not follow Australian media, and who \
            therefore miss things announced only in English on TV, radio or \
            government websites.

            You will be given one news article. Do two things.

            FIRST, decide whether it is worth a reader's attention.

            Relevant: anything affecting daily life, money, safety, health, \
            housing, transport, employment, education, visas, or dealings with \
            government or utilities. Emergency alerts and alert tests. Price, \
            fee, tax and rebate changes. Product recalls and scam or fraud \
            warnings. Data breaches at companies with many customers. Public \
            transport and road changes. Weather warnings. Rule changes people \
            must act on. Anything a resident could be penalised, charged or \
            endangered for not knowing.

            Not relevant: sport results and player news, celebrity and \
            entertainment, individual crimes or court cases with no wider \
            effect, opinion and analysis pieces, overseas politics, human \
            interest and novelty stories, scientific research with no immediate \
            practical consequence, and internal party politics.

            When genuinely unsure, mark it relevant. A human reviews everything \
            before publication, so a wrong inclusion costs one click, while a \
            wrong exclusion means a reader never learns something that mattered.

            SECOND, if relevant, write a Korean-language briefing.

            Do NOT translate the article. Read it, take the facts, and write a \
            fresh piece of Korean prose that tells a reader what happened and \
            what, if anything, they should do. Never reproduce the article's \
            sentences, structure or phrasing, in English or in Korean. Aim for \
            three to five short paragraphs.

            Lead with what affects the reader most directly, even if the article \
            buries it — a scam warning matters more to them than a company's \
            share price. Keep dates, amounts, deadlines, phone numbers and \
            organisation names exactly as the article states them; never \
            estimate or round. Omit detail that does not help a reader act. \
            Write plainly, in the polite -습니다 register, without headlines or \
            markdown. If action is needed, say so explicitly. Close by noting \
            that details are in the linked original.

            Reply with JSON only, no other text, in exactly this shape:
            {"relevant": true or false, "reason": "one short line in English", \
            "koreanDraft": "the Korean briefing, or null if not relevant"}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${app.ai.enabled:true}")
    private boolean enabled;

    @Override
    public SummarisationResult summarize(String title, String bodyText) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return SummarisationResult.relevant(
                    null, "AI summarisation disabled or no key configured.");
        }

        try {
            String responseText = callApi(title, truncate(bodyText));
            return parseResult(responseText);
        } catch (Exception e) {
            // A failed call must not discard the article. Passing it through
            // unjudged puts it in front of an administrator, which is where it
            // would have gone anyway without AI.
            log.warn("Summarisation failed for '{}': {}", title, e.getMessage());
            return SummarisationResult.relevant(
                    null, "Summarisation unavailable: " + e.getMessage());
        }
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() <= MAX_INPUT_CHARS ? text : text.substring(0, MAX_INPUT_CHARS);
    }

    private String callApi(String title, String bodyText) throws Exception {
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", "Article title: " + title + "\n\nArticle text:\n" + bodyText);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("max_tokens", MAX_TOKENS);
        payload.put("system", SYSTEM_PROMPT);
        payload.set("messages", objectMapper.createArrayNode().add(userMessage));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                // Generous: a long article can take a while, and a retry costs
                // another call.
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "API returned " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("content");

        if (!content.isArray() || content.isEmpty()) {
            throw new IllegalStateException("API response contained no content blocks");
        }

        return content.get(0).path("text").asText();
    }

    private SummarisationResult parseResult(String raw) throws Exception {
        // Models sometimes wrap JSON in a markdown fence despite instructions
        // to the contrary. Strip it rather than failing the whole import.
        String cleaned = raw.trim()
                .replaceAll("^```(?:json)?\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

        JsonNode node = objectMapper.readTree(cleaned);

        boolean relevant = node.path("relevant").asBoolean(true);
        String reason = node.path("reason").asText("");

        if (!relevant) {
            return SummarisationResult.irrelevant(reason);
        }

        JsonNode draftNode = node.path("koreanDraft");
        String draft = draftNode.isNull() ? null : draftNode.asText(null);

        // Relevant but no draft is a malformed reply, not a usable result. Pass
        // it through so an administrator writes the summary themselves.
        if (draft == null || draft.isBlank()) {
            return SummarisationResult.relevant(
                    null, "Model returned no draft; needs writing by hand.");
        }

        return SummarisationResult.relevant(draft, reason);
    }
}