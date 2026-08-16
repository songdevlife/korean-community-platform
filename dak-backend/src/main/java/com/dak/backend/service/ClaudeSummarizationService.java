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

            THIRD, if relevant, write a Korean headline for that briefing.

            Write it from the briefing you have just written, not from the \
            article's own headline — do not translate that headline, and do not \
            reuse its wording. The headline must stand on its own: someone \
            reading only this line should know what happened and whether it \
            concerns them.

            Put the concrete words a reader would search for in it — the suburb \
            or city, the organisation, the amount, the deadline, the thing \
            recalled. A headline that says an outage occurred is worth less \
            than one that names where. Keep it under 45 Korean characters. End \
            with a noun rather than a verb, as Korean news headlines do, and do \
            not use the -습니다 register the body uses. No quotation marks, no \
            markdown, no trailing full stop, no source name.

            Lead with what affects the reader most directly, even if the article \
            buries it — a scam warning matters more to them than a company's \
            share price. Keep dates, amounts, deadlines, phone numbers and \
            organisation names exactly as the article states them; never \
            estimate or round. Omit detail that does not help a reader act. \
            Write plainly, in the polite -습니다 register, without headlines or \
            markdown. If action is needed, say so explicitly. Close by noting \
            that details are in the linked original.

            FOURTH, if relevant, write an English URL slug for the briefing.

            This is the web address the article will live at, so it is read by \
            search engines and by anyone glancing at a shared link. Lowercase \
            English words separated by hyphens, no other characters. Three to \
            six words.

            Take the same concrete nouns you put in the Korean headline — the \
            suburb, the organisation, the thing recalled — and give their \
            English equivalents. Do not transliterate Korean. Do not include \
            the date, the source name, or filler words like "news" or \
            "update".

            Reply with JSON only, no other text, in exactly this shape:
            {"relevant": true or false, "reason": "one short line in English", \
            "koreanTitle": "the Korean headline, or null if not relevant", \
            "slug": "the English slug, or null if not relevant", \
            "koreanDraft": "the Korean briefing, or null if not relevant"}
            """;

            private static final String MANUAL_SYSTEM_PROMPT = """
                You are an editorial assistant for DAK, a local-information platform
                for Korean speakers living in Adelaide, South Australia.
        
                The administrator has already selected this article for review.
        
                Do NOT judge relevance.
                Do NOT reject the article.
                Always produce a Korean draft, Korean headline and English URL slug.
        
                Do NOT translate the article. Read the facts and write a fresh Korean
                briefing in your own structure and wording. Do not reproduce sentences
                or phrasing from the source.
        
                Write three to five short paragraphs in clear Korean using the polite
                -습니다 register.
        
                If the article contains dates, amounts, locations, organisation names,
                warnings or actions readers should take, preserve those facts exactly.
        
                Write a Korean headline under 45 Korean characters. The headline should
                state the concrete event and location or organisation where useful.
                Do not use quotation marks, markdown or a trailing full stop.
        
                Write an English URL slug using three to six lowercase English words
                separated by hyphens.
        
                Reply with JSON only in exactly this shape:
        
                {
                  "relevant": true,
                  "reason": "Manually selected by administrator",
                  "koreanTitle": "the Korean headline",
                  "slug": "the English slug",
                  "koreanDraft": "the Korean briefing"
                }
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

    log.info(
            "Claude summarisation started for '{}'. API key present: {}, enabled: {}",
            title,
            apiKey != null && !apiKey.isBlank(),
            enabled
    );

    if (!enabled || apiKey == null || apiKey.isBlank()) {
            return SummarisationResult.relevant(
                    null, "AI summarisation disabled or no key configured.");
        }

        try {
            String responseText =
        callApi(
                title,
                truncate(bodyText),
                SYSTEM_PROMPT
        );
        
            SummarisationResult result = parseResult(responseText);
        
            log.info(
                "Claude summarisation completed for '{}'. Relevant: {}, reason: '{}', Korean draft present: {}",
                title,
                result.relevant(),
                result.reason(),
                result.koreanDraft() != null && !result.koreanDraft().isBlank()
        );
        
            return result;
        
        } catch (Exception e) {
            // A failed call must not discard the article. Passing it through
            // unjudged puts it in front of an administrator, which is where it
            // would have gone anyway without AI.
            log.warn("Summarisation failed for '{}': {}", title, e.getMessage());
            return SummarisationResult.relevant(
                    null, "Summarisation unavailable: " + e.getMessage());
        }
    }

    @Override
public SummarisationResult summarizeManual(
        String title,
        String bodyText
) {

    log.info(
            "Claude manual summarisation started for '{}'. API key present: {}, enabled: {}",
            title,
            apiKey != null && !apiKey.isBlank(),
            enabled
    );

    if (!enabled || apiKey == null || apiKey.isBlank()) {
        return SummarisationResult.relevant(
                null,
                "AI summarisation disabled or no key configured."
        );
    }

    try {
        String responseText =
                callApi(
                        title,
                        truncate(bodyText),
                        MANUAL_SYSTEM_PROMPT
                );

        SummarisationResult result =
                parseResult(responseText);

        log.info(
                "Claude manual summarisation completed for '{}'. Korean draft present: {}",
                title,
                result.koreanDraft() != null
                        && !result.koreanDraft().isBlank()
        );

        return result;

    } catch (Exception e) {

        log.warn(
                "Manual summarisation failed for '{}': {}",
                title,
                e.getMessage()
        );

        return SummarisationResult.relevant(
                null,
                "Manual summarisation unavailable: "
                        + e.getMessage()
        );
    }
}

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() <= MAX_INPUT_CHARS ? text : text.substring(0, MAX_INPUT_CHARS);
    }

    private String callApi(
        String title,
        String bodyText,
        String systemPrompt
) throws Exception {
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", "Article title: " + title + "\n\nArticle text:\n" + bodyText);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("max_tokens", MAX_TOKENS);
        payload.put("system", systemPrompt);
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

        JsonNode titleNode = node.path("koreanTitle");
        String koreanTitle = titleNode.isNull() ? null : titleNode.asText(null);

        // A missing headline is not worth discarding a good draft over. The
        // caller falls back to the source's headline and says so in the log,
        // which is visible rather than silent — the failure this project keeps
        // producing is the one that reports success.
        if (koreanTitle != null) {
            koreanTitle = koreanTitle.trim();
            if (koreanTitle.isEmpty()) {
                koreanTitle = null;
            }
        }

        // Same treatment as the headline, and for the same reason. The caller
        // generates a dated fallback when this is null, so a malformed slug
        // costs a readable URL rather than the whole import.
        JsonNode slugNode = node.path("slug");
        String slug = slugNode.isNull() ? null : slugNode.asText(null);
        if (slug != null) {
            slug = slug.trim();
            if (slug.isEmpty()) {
                slug = null;
            }
        }

        return SummarisationResult.relevant(koreanTitle, slug, draft, reason);
    }
}