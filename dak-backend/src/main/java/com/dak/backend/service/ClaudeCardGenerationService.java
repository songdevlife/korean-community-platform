package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Uses Claude to turn finished DAK editorial content into a compact card spec.
 *
 * This service does NOT generate an image and does NOT render the final card.
 * Its only responsibility is deciding what short text and visual concept
 * should be passed to the later hero-image generator and card renderer.
 */
@Service
public class ClaudeCardGenerationService implements CardGenerationService {

    private static final Logger log =
            LoggerFactory.getLogger(ClaudeCardGenerationService.class);

    private static final String API_URL =
            "https://api.anthropic.com/v1/messages";

    private static final String API_VERSION = "2023-06-01";

    private static final int MAX_TOKENS = 1200;

    private static final int MAX_INPUT_CHARS = 12_000;

    private static final String CARD_STYLE =
            "DAK_HAND_PAINTED_EDITORIAL_V1";

    private static final String SYSTEM_PROMPT = """
            You are the card editor for DAK, Discover Adelaide Korea.

            You will receive finished DAK editorial content that has already been
            written or reviewed for publication.

            Your job is NOT to rewrite the full article.
            Your job is to create a specification for ONE single social-media card.

            The card must be understandable quickly on a mobile screen.

            STRICT FACTUAL RULES:

            - Use only facts contained in the supplied DAK content.
            - Never add outside knowledge.
            - Never guess a date, number, amount, deadline, organisation or location.
            - Never make a claim stronger than the supplied content supports.
            - If a useful key fact is not explicitly present, return keyFact as null.
            - Do not translate or reconstruct information from an external source.
              Work only from the DAK content supplied to you.

            SINGLE CARD OR CAROUSEL:

            A single card carries one thing a reader takes away at a glance.
            A carousel carries a sequence: the point, then what it means, then
            what to do about it.

            Choose a carousel only where the supplied content actually holds
            enough for each card to say something different. Two cards that
            restate one another are worse than one card that says it once.
            A short news item is a single card.

            When you choose a carousel, supply carouselCards: one to three of
            them, following the first card rather than repeating it. The first
            card is described by title, headline, keyFact and layoutType as
            usual; these are what a reader swipes to.

            Each carousel card has a role:

            DETAIL   - what happened, or what the thing is. Expands on the
                       first card without repeating its wording.
            ACTION   - what the reader should do, check, watch for or be
                       aware of. Only where the content supports it; never
                       invent advice.
            SOURCE   - where this came from and what to consult for the full
                       picture. Optional, and last when present.

            Order them as a reader needs them: DETAIL before ACTION, SOURCE
            last. Do not use the same role twice.

            Otherwise leave carouselCards as null.

            LAYOUT SELECTION:

            Choose exactly one layoutType for this card.

            DAK's principle is: show strongly, do not exaggerate.
            A layout may make a verified fact visually dominant, but must never
            add emotion, shock or spectacle beyond what the source states.

            STANDARD
            - The default. Use it whenever no other layout clearly fits.
            - Suits ordinary policy changes, notices, fee changes and updates.

            INFOGRAPHIC
            - Use when the reader needs several separate pieces of practical
              information to act, such as a date plus a period plus a reason.
            - Suits participation notices, procedures and step-by-step guidance.
            - Requires infoBlocks: two to four of them, each a different fact.
              Never choose INFOGRAPHIC without them.
            - Use keyFact as null when INFOGRAPHIC is chosen; the blocks carry
              the facts instead.

            FACT_HOOK
            - Use only when ONE verified figure is the single most important
              thing the reader must understand, and that figure conveys scale
              or exposure rather than human casualties.
            - Requires a keyFact with a concrete value. Never choose FACT_HOOK
              without one.
            - Example shape: a number of affected customers, an amount, a
              percentage, a deadline.

            A figure being present does not make it the point of the story.
            Before choosing FACT_HOOK, test the figure you would show:

            1. Is it what this report newly establishes, or is it a running
               total, a background statistic, or context carried over from
               earlier coverage? A cumulative national tally that was already
               true yesterday is not what the story says.
            2. Does it concern the reader? DAK readers are in Adelaide and
               South Australia. A South Australian figure outranks a national
               one, and a national one outranks a figure about another state.
               New South Wales is not closer to a DAK reader than Australia
               is — it is somewhere else. Prefer the South Australian figure
               whenever the content contains one.
            3. Would the story still stand without it? If removing the figure
               leaves the report intact, the figure is supporting detail and
               the layout is STANDARD.

            Where two figures compete, prefer the one that is newly reported
            and concerns the reader, not the largest.

            A figure too small to convey scale does not carry a card. "4마리"
            printed at the size of a headline makes a story look smaller than
            it is. If the honest figure is small, the story is STANDARD and
            the figure belongs in the keyFact box.

            If no figure passes all three, choose STANDARD even when the
            content contains numbers.

            URGENT
            - Use for deaths, serious injury, disaster, emergency warnings and
              similarly grave events.
            - Present the fact plainly. Never treat human harm as spectacle.
            - Requires a keyFact with a concrete value.

            Never choose a layout to make the story feel more dramatic than the
            supplied content supports.

            CARD WRITING RULES:

            - format is SINGLE for one card, CAROUSEL where carouselCards
              are supplied.
            - headerTitle is the short Korean label printed at the very top of
              the card, above a divider line and beside the DAK mascot.
            - headerTitle must be a noun phrase only, with no particles,
              no verbs and no sentence ending.
            - headerTitle must be 6 or 7 Korean characters, counting spaces.
              It is set large beside the mascot, and eight will wrap onto a
              second line that pushes everything below it down the card.
            - Drop the qualifier before the subject: "면허 규정 변경" over
              "남호주 면허 규정 변경", "정보 유출 확인" over "오리진 정보 유출 확인".
              The title below carries the specifics; this is a label.
            - headerTitle should be roughly 6 to 10 Korean characters and fit
              on one line. It sits beside the mascot with limited room, and a
              second line pushes everything below it down the card.
            - headerTitle compresses the topic. Examples of the intended shape:
              "오리진 정보 유출", "2026 호주 센서스", "학생비자 규정 변경".
            - headerTitle must never repeat title word for word.
            - title should identify the topic immediately, in roughly fifteen
              to twenty-five Korean characters. It is set large, and a title
              that wraps onto a second line with one word on it reads as a
              mistake.
            - headline must add what the title does not already say. The
              consequence, the scale, who is affected, what changes, or what
              the reader should do about it.
            - A headline that restates the title in different words is wasted
              space on a card that has very little of it. If the only thing
              you can write is the title again, the content is thin enough
              that the headline should carry the next most useful fact
              instead.
            - Keep the headline concise and natural in Korean.
            - Do not put several facts into the headline.
            - Mark the one phrase a reader must not miss by wrapping it in
              double asterisks: 영주권자는 **90일 안에** 시험을 봐야 합니다.
              It is drawn in the accent colour while the rest stays black.
            - Mark one phrase, never two, and never the whole headline. A
              headline entirely in the accent colour emphasises nothing.
            - The marked phrase should be short — a period, a figure, a
              condition. Not a clause.
            - Where nothing in the headline stands out, use no asterisks.
            - keyFact is optional and there may be only ONE.
            - A keyFact should normally be a date, amount, deadline, number,
              location, eligibility condition or similarly concrete fact.
            - For FACT_HOOK and URGENT the keyFact becomes the visual subject
              of the card, so its value must be short and read clearly at a
              glance. Prefer "480만 명 이상" over a long descriptive phrase.
            - The keyFact label explains what the value counts, and must say
              what it is a count of and where. "남호주 확인 사례" and
              "호주 전체 누적" are different facts and the label must not blur
              them.
            - A figure that will be out of date within days is a poor choice
              for a card that stays online.
            - Do not write explanatory paragraphs.
            - Do not include a URL, source citation, hashtags or social-media CTA.
              Those are added later by the renderer.

            CAROUSEL CARD RULES:

            - heading is a short Korean line naming what this card covers.
              Roughly six to sixteen characters. Not a sentence.
            - body is two to four short Korean sentences. This is the only
              place on a card where continuous prose belongs, and it still has
              to be readable on a phone at arm's length.
            - blocks is optional and follows the infoBlocks rules below. Use
              it where the card carries separate facts rather than an
              explanation — dates, amounts, contacts. A card has either a body
              or blocks, not both.
            - Never repeat a sentence from the first card.
            - Every statement must come from the supplied content, exactly as
              on the first card. A carousel is more room, not more licence.

            INFOGRAPHIC BLOCK RULES:

            - Each block answers one question a reader would actually ask:
              when, how long, who, how much, why it matters, what to do.
            - label names the fact in two to six Korean characters.
              Examples of the intended shape: "기준일", "참여 기간", "대상".
            - value is the fact itself, kept short enough to read at a glance.
            - note is one short line telling the reader what the value means
              for them. Supply one wherever the content supports it; use null
              only when nothing in the supplied content would fill it.
            - Never repeat the value in the note. A date's note says what
              happens on that date, not the date again.
            - Keep note to roughly twenty Korean characters. The blocks share
              a height, so one long note leaves the others looking empty.
            - Blocks should carry a similar amount of text as one another.
            - On an INFOGRAPHIC, write infoBlocks first. Then write the
              headline, and before you do, list to yourself every date,
              amount, period and figure you have just put in a block. None of
              them may appear in the headline. Not the year, not the month,
              not the number of days, not the price.

            - The headline is the sentence a reader would say if asked what
              this means for them. It names who is affected and what is now
              required of them, without stating when or how much — the blocks
              are directly beneath it and answer those.

              Acceptable: 한국 면허 소지자는 이제 시험을 다시 봐야 합니다
              Acceptable: 영주권을 받으면 남호주 면허로 바꿔야 합니다
              Not acceptable: 2025년 5월부터 영주권자는 90일 안에 시험을 봐야 합니다
              Not acceptable: 340~380불의 실기 시험을 통과해야 합니다

              The last two are rejected because the card already shows 2025년
              5월 1일, 90일 and 340~380불 in its blocks.
            - value must stay short enough to read at a glance: aim for under
              ten Korean characters, and never more than twelve. It is set
              large in a narrow box beside an icon.
            - Put no qualifier in the value. "340~380불" is the value;
              what it is for goes in the label, and the conditions go in the
              note.
            - icon names the picture shown beside the block. Choose the one
              that fits the fact, from exactly this list, or null:
              CALENDAR for a fixed date, CLOCK for a period or deadline,
              PEOPLE for who is affected, LOCATION for where, MONEY for an
              amount or fee, DOCUMENT for a form or a process, LIGHTBULB for
              why it matters, CHECK for what qualifies or what to do,
              WARNING for a risk or a consequence, INFO for anything else.
            - Never invent an icon name outside that list.
            - Every block must carry a different fact. Two blocks saying the
              same thing in different words is worse than one block.
            - Order them the way a reader would need them, not by importance.

            HERO VISUAL RULES:

            Describe one simple visual scene that communicates the topic before
            the reader starts reading the card.

            The visual description must:
            - be written in English
            - contain one clear focal concept
            - work as an editorial illustration
            - avoid excessive background detail
            - contain no text, letters or numbers
            - contain no logos or trademarks
            - contain no watermark
            - contain no generated DAK wordmark
            - contain no unnecessary Australian landmarks
            - never add Sydney landmarks merely because the story is Australian
            - avoid photorealism, photography, 3D render and glossy corporate art

            DAK uses a hand-painted editorial illustration style with visible
            canvas or paper texture, slightly imperfect brush strokes, bold
            hand-drawn outlines, rounded simplified forms, warm colours and a
            cream or off-white dominant background.

            The DAK chicken mascot may appear as a small supporting character only
            when appropriate.

            For deaths, serious accidents, violent crime, disasters, severe harm
            or similarly sensitive stories, mascot must be null.

            For ordinary informational stories, mascot may still be null if adding
            it would make the visual cluttered or childish.

            Reply with JSON only.

            Use exactly this shape:

            {
              "layoutType": "STANDARD | INFOGRAPHIC | FACT_HOOK | URGENT",
              "headerTitle": "very short Korean noun phrase for the card header",
              "title": "short Korean card title",
              "headline": "one concise Korean takeaway",
              "keyFact": {
                "label": "short Korean or established English label",
                "value": "exact value from the supplied content"
              },
              "infoBlocks": [
                {
                  "label": "short Korean name for this fact",
                  "value": "the fact itself",
                  "note": "one short Korean line of context, or null",
                  "icon": "CALENDAR | CLOCK | PEOPLE | LOCATION | MONEY | DOCUMENT | LIGHTBULB | CHECK | WARNING | INFO, or null"
                }
              ],
              "carouselCards": [
                {
                  "role": "DETAIL | ACTION | SOURCE",
                  "heading": "short Korean line naming what this card covers",
                  "body": "two to four short Korean sentences, or null",
                  "blocks": null
                }
              ],
              "visual": {
                "subject": "English description of the main illustration subject",
                "mood": "English description of the intended mood",
                "mascot": "English description of a small DAK chicken action, or null"
              }
            }

            If there is no suitable key fact, use:

            "keyFact": null

            Unless the layout is INFOGRAPHIC, use:

            "infoBlocks": null

            For a single card, use:

            "carouselCards": null
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
    public CardSpec generateForAustraliaUpdate(
            String title,
            String koreanSummary
    ) {
        return generate(
                "AU_UPDATE",
                title,
                koreanSummary
        );
    }

    @Override
    public CardSpec generateForGuide(
            String title,
            String summary,
            String body
    ) {
        StringBuilder content = new StringBuilder();

        if (summary != null && !summary.isBlank()) {
            content.append("Guide summary:\n")
                    .append(summary.trim())
                    .append("\n\n");
        }

        content.append("Guide body:\n")
                .append(body == null ? "" : body);

        return generate(
                "GUIDE",
                title,
                content.toString()
        );
    }

    private CardSpec generate(
            String contentType,
            String title,
            String content
    ) {
        String safeTitle = title == null ? "" : title.trim();
        String safeContent = content == null ? "" : content.trim();

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.warn(
                    "Card generation skipped for '{}': AI is disabled or no API key is configured.",
                    safeTitle
            );

            return fallbackSpec(contentType, safeTitle);
        }

        try {
            String responseText = callApi(
                    contentType,
                    safeTitle,
                    truncate(safeContent)
            );

            return parseResult(
                    contentType,
                    safeTitle,
                    responseText
            );

        } catch (Exception e) {
            log.warn(
                    "Card generation failed for '{}': {}",
                    safeTitle,
                    e.getMessage()
            );

            return fallbackSpec(contentType, safeTitle);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }

        return text.length() <= MAX_INPUT_CHARS
                ? text
                : text.substring(0, MAX_INPUT_CHARS);
    }

    private String callApi(
            String contentType,
            String title,
            String content
    ) throws Exception {

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");

        String userContent = """
                Content type: %s

                DAK title:
                %s

                DAK content:
                %s
                """.formatted(
                contentType,
                title,
                content
        );

        userMessage.put("content", userContent);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("max_tokens", MAX_TOKENS);
        payload.put("system", SYSTEM_PROMPT);
        payload.set(
                "messages",
                objectMapper.createArrayNode().add(userMessage)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .timeout(Duration.ofSeconds(90))
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(payload)
                        )
                )
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "API returned "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentBlocks = root.path("content");

        if (!contentBlocks.isArray() || contentBlocks.isEmpty()) {
            throw new IllegalStateException(
                    "API response contained no content blocks"
            );
        }

        return contentBlocks.get(0)
                .path("text")
                .asText();
    }

    private CardSpec parseResult(
            String contentType,
            String originalTitle,
            String raw
    ) throws Exception {

        String cleaned = raw.trim()
                .replaceAll("^```(?:json)?\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

        JsonNode node = objectMapper.readTree(cleaned);

        String title = textOrFallback(
            node.path("title"),
            originalTitle
    );

    // Null is acceptable here; CardSpec falls back to title when rendering.
    String headerTitle = nullableText(
        node.path("headerTitle")
);

// Validated and defaulted by CardSpec.effectiveLayoutType().
String layoutType = nullableText(
        node.path("layoutType")
);

    String headline = textOrFallback(
            node.path("headline"),
            title
    );

    CardSpec.KeyFact keyFact = parseKeyFact(
        node.path("keyFact")
);

List<CardSpec.InfoBlock> infoBlocks = parseInfoBlocks(
        node.path("infoBlocks")
);

List<CardSpec.CarouselCard> carouselCards = parseCarouselCards(
        node.path("carouselCards")
);

        JsonNode visualNode = node.path("visual");

        String subject = textOrFallback(
                visualNode.path("subject"),
                "simple hand-painted editorial illustration representing "
                        + originalTitle
        );

        String mood = nullableText(
                visualNode.path("mood")
        );

        String mascot = nullableText(
                visualNode.path("mascot")
        );

        CardSpec.VisualSpec visual = new CardSpec.VisualSpec(
                subject,
                mood,
                mascot,
                CARD_STYLE
        );

        return new CardSpec(
                contentType,
                carouselCards == null ? "SINGLE" : "CAROUSEL",
                layoutType,
            headerTitle,
            title,
            headline,
            keyFact,
            infoBlocks,
            carouselCards,
            visual
    );
    }

    private CardSpec.KeyFact parseKeyFact(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        String label = nullableText(node.path("label"));
        String value = nullableText(node.path("value"));

        if (label == null || value == null) {
            return null;
        }

        return new CardSpec.KeyFact(
                label,
                value
        );
    }

    private List<CardSpec.InfoBlock> parseInfoBlocks(JsonNode node) {

        if (node == null || !node.isArray() || node.isEmpty()) {
            return null;
        }

        List<CardSpec.InfoBlock> blocks = new ArrayList<>();

        for (JsonNode element : node) {

            String label = nullableText(element.path("label"));
            String value = nullableText(element.path("value"));

            // A block with no value has nothing to show. The label alone
            // would render as a heading over empty space.
            if (value == null) {
                continue;
            }

            blocks.add(
                new CardSpec.InfoBlock(
                        label,
                        value,
                        nullableText(element.path("note")),
                        nullableText(element.path("icon"))
                )
        );
        }

        return blocks.isEmpty() ? null : blocks;
    }

    private List<CardSpec.CarouselCard> parseCarouselCards(JsonNode node) {

        if (node == null || !node.isArray() || node.isEmpty()) {
            return null;
        }

        List<CardSpec.CarouselCard> cards = new ArrayList<>();

        for (JsonNode element : node) {

            String heading = nullableText(element.path("heading"));

            // A card with no heading has nothing to lead with, and the
            // renderer would draw an empty band where one should be.
            if (heading == null) {
                continue;
            }

            String role = nullableText(element.path("role"));

            cards.add(
                    new CardSpec.CarouselCard(
                            role == null ? CardSpec.CardRole.DETAIL.name() : role,
                            heading,
                            nullableText(element.path("body")),
                            parseInfoBlocks(element.path("blocks"))
                    )
            );
        }

        return cards.isEmpty() ? null : cards;
    }

    private String textOrFallback(
            JsonNode node,
            String fallback
    ) {
        String value = nullableText(node);

        return value == null
                ? fallback
                : value;
    }

    private String nullableText(JsonNode node) {
        if (node == null
                || node.isNull()
                || node.isMissingNode()) {
            return null;
        }

        String value = node.asText(null);

        if (value == null) {
            return null;
        }

        value = value.trim();

        return value.isEmpty()
                ? null
                : value;
    }

    /**
     * Conservative fallback used when Claude is unavailable.
     *
     * It deliberately does not invent a key fact or detailed visual concept.
     * The existing DAK title is reused instead.
     */
    private CardSpec fallbackSpec(
            String contentType,
            String title
    ) {
        String safeTitle =
                title == null || title.isBlank()
                        ? "DAK"
                        : title;

                        return new CardSpec(
                            contentType,
                            "SINGLE",
                            CardSpec.LayoutType.STANDARD.name(),
                            // No short header label when the AI is unavailable;
                            // the renderer falls back to the title.
                            null,
                            safeTitle,
                            safeTitle,
                            null,
                            null,
                            null,
                            new CardSpec.VisualSpec(
                        "simple hand-painted editorial illustration representing the topic",
                        "informative and neutral",
                        null,
                        CARD_STYLE
                )
        );
    }
}