package com.dak.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * AI-generated content specification for a DAK card.
 *
 * This record contains the editorial decisions required to render a card.
 * It does not contain generated image URLs or storage information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardSpec(
    String contentType,
    String format,

    /**
     * Legacy layout used by the current renderer.
     *
     * Kept during the migration so STANDARD / INFOGRAPHIC /
     * FACT_HOOK / URGENT cards continue to render while the
     * new hierarchy-based card engine is introduced.
     */
    String layoutType,

    /**
     * Overall editorial tone of the card.
     *
     * LIGHT      - friendly everyday information
     * STANDARD   - ordinary news, policy, economy and migration
     * SERIOUS    - accidents, crime, major disruption or serious harm
     * SENSITIVE  - death, victims, grief or content requiring restraint
     */
    String tone,

    /**
     * Short Korean topic label shown in the header.
     * It must remain understandable without the source article.
     */
    String headerTitle,

    String title,

    /**
     * Optional contextual sentence.
     *
     * Concrete facts should normally live in the structured fact fields,
     * not be repeated here.
     */
    String headline,

    /*
     * -----------------------------------------------------------------
     * NEW INFORMATION HIERARCHY
     * -----------------------------------------------------------------
     */

    /**
     * The one fact that carries the story when one clearly exists.
     * Null when all facts have similar importance.
     */
    CardFact primaryFact,

/**
 * Core supporting facts needed to understand the story.
 *
 * These are the main structured facts shown prominently by the renderer.
 */
List<CardFact> supportingFacts,

/**
 * Optional additional facts that are useful but less important than
 * the main supporting facts.
 *
 * LIGHT cards may use up to two of these as a secondary information row.
 *
 * Examples:
 * - live music or special program details
 * - venue information
 * - booking information
 * - eligibility or practical notes
 *
 * Must come from the source. Do not create secondary facts merely
 * to fill visual space.
 */
List<CardFact> secondaryFacts,

/**
 * One visually separate piece of context.
 */
Callout callout,

    /**
     * What the reader should do, only where the source explicitly
     * supports an action.
     */
    ActionBlock action,

    /*
     * -----------------------------------------------------------------
     * LEGACY FIELDS
     * -----------------------------------------------------------------
     *
     * These remain temporarily so the existing renderer continues to work
     * while cards are migrated to the hierarchy above.
     */

    KeyFact keyFact,

    List<InfoBlock> infoBlocks,

    List<CarouselCard> carouselCards,

    VisualSpec visual
) {

    /**
     * Backward-compatible constructor used by the current card pipeline.
     *
     * Existing services still create CardSpec using the original fields.
     * Keeping this constructor allows the hierarchy-based card model to be
     * introduced without breaking every renderer and service at once.
     */
    public CardSpec(
            String contentType,
            String format,
            String layoutType,
            String headerTitle,
            String title,
            String headline,
            KeyFact keyFact,
            List<InfoBlock> infoBlocks,
            List<CarouselCard> carouselCards,
            VisualSpec visual
    ) {
        this(
                contentType,
                format,
                layoutType,

                // New tone is not supplied by old callers yet.
                null,

                headerTitle,
                title,
                headline,

                // New hierarchy fields are populated in the next migration step.
null,   // primaryFact
null,   // supportingFacts
null,   // secondaryFacts
null,   // callout
null,   // action

keyFact,
infoBlocks,
carouselCards,
visual
        );
    }

/**
 * Supported card layouts.
 *
 * DAK principle: show strongly, do not exaggerate. A layout may make a
 * verified fact visually dominant, but must never add emotion, shock or
 * spectacle beyond what the source states.
 */
public enum LayoutType {

    /** Current editorial layout used by the legacy renderer. */
    STANDARD,

    /** Current multi-fact layout used by the legacy renderer. */
    INFOGRAPHIC,

    /** Current figure-led layout used by the legacy renderer. */
    FACT_HOOK,

    /** Current restrained emergency layout used by the legacy renderer. */
    URGENT
}

/**
 * Editorial tone controls the visual world of the card rather than
 * the arrangement of its facts.
 *
 * The article decides WHAT the background depicts.
 * Tone decides HOW that background is treated.
 */
public enum CardTone {

    /** Friendly everyday information and community content. */
    LIGHT,

    /** Normal news, migration, policy, economy and public information. */
    STANDARD,

    /** Accidents, crime, major disruption and serious public-interest news. */
    SERIOUS,

    /** Death, victims, grief and other content requiring maximum restraint. */
    SENSITIVE
}

/**
 * The header label, falling back to the full title when the AI
 * did not supply a short one.
 */
public String effectiveHeaderTitle() {

    if (headerTitle == null || headerTitle.isBlank()) {
        return title;
    }

    return headerTitle;
}

/**
 * The resolved layout.
 *
 * Falls back to STANDARD when the value is missing, unrecognised, or
 * when a figure-led layout was requested without a figure to show.
 */
public LayoutType effectiveLayoutType() {

    if (layoutType == null || layoutType.isBlank()) {
        return LayoutType.STANDARD;
    }

    LayoutType resolved;

    try {
        resolved = LayoutType.valueOf(
                layoutType.trim().toUpperCase()
        );
    } catch (IllegalArgumentException e) {
        return LayoutType.STANDARD;
    }

    boolean needsFigure =
                resolved == LayoutType.FACT_HOOK
                        || resolved == LayoutType.URGENT;

        if (needsFigure && !hasUsableKeyFact()) {
            return LayoutType.STANDARD;
        }

        // A card of blocks with no blocks is an empty card.
        if (resolved == LayoutType.INFOGRAPHIC && !hasUsableInfoBlocks()) {
            return LayoutType.STANDARD;
        }

        return resolved;
    }

    /**
     * Blocks that are safe to render: at least two, since one block is a
     * key fact rather than an infographic, and no more than four, which is
     * what the card height allows.
     */
    public List<InfoBlock> usableInfoBlocks() {

        if (infoBlocks == null) {
            return List.of();
        }

        return infoBlocks.stream()
                .filter(block -> block != null)
                .filter(block -> block.value() != null && !block.value().isBlank())
                .limit(4)
                .toList();
    }

    private boolean hasUsableInfoBlocks() {
        return usableInfoBlocks().size() >= 2;
    }

private boolean hasUsableKeyFact() {

    return keyFact != null
            && keyFact.value() != null
            && !keyFact.value().isBlank();
}

/**
 * A structured fact extracted from the article.
 *
 * role describes what the fact means editorially rather than where the
 * renderer must physically place it.
 */
public record CardFact(
    String role,

    /**
     * Controls how strongly this fact may be presented visually.
     *
     * CRITICAL is reserved for facts that genuinely define the severity
     * or immediate consequence of the event, such as deaths, severe harm,
     * evacuation orders or another exceptional immediate impact.
     *
     * NORMAL remains important editorially, but must not automatically
     * become a large red figure.
     */
    String emphasis,

    String label,
    String value,
    String note,
    String icon
) {

    public FactEmphasis effectiveEmphasis() {

        if (emphasis == null || emphasis.isBlank()) {
            return FactEmphasis.NORMAL;
        }

        try {
            return FactEmphasis.valueOf(
                    emphasis.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return FactEmphasis.NORMAL;
        }
    }
}

public enum FactEmphasis {

    /** Important information without exceptional visual emphasis. */
    NORMAL,

    /** Exceptional immediate impact that may receive strong visual emphasis. */
    CRITICAL
}

/**
* A separate supporting callout.
*
* Used for information that deserves its own visual treatment but should
* not compete with the primary fact.
*/
public record Callout(
    String label,
    String value,
    String note
) {}

/**
* Reader action explicitly supported by the source article.
*
* Null for articles where there is nothing the reader needs to do.
*/
public record ActionBlock(
    String title,
    String body,
    String icon
) {}

/**
* Legacy single key fact.
*
* Retained while the existing renderer is migrated.
*/
public record KeyFact(
    String label,
    String value
) {}

/**
* One fact in an INFOGRAPHIC card.
*
* label is what the fact is, value is the fact itself, note is the one
* line of context a reader needs to act on it. note may be absent where
* the value speaks for itself.
*/
public record InfoBlock(
    String label,
    String value,
    String note,

    /**
     * One of {@link BlockIcon}, or null. An unrecognised name renders
     * as no icon rather than failing the card.
     */
    String icon
) {}

/**
     * One card after the first in a carousel.
     *
     * The roles are deliberately not named for news: a guide runs overview,
     * steps, then what to watch for, and a news item runs what happened then
     * what to do about it, but both are a detail card followed by an action
     * card. Naming them WHAT_HAPPENED would have meant a second set for
     * guides.
     */
public record CarouselCard(
    String role,
    String heading,
    String body,
    List<InfoBlock> blocks
) {}

public enum CardRole {

/** Explains, expands, gives the substance. */
DETAIL,

/** What the reader should do, check, or watch for. */
ACTION,

/** Where this came from and where to read more. */
SOURCE
}

/**
* The cards that follow the first, filtered to those that can be drawn.
* Capped at three, which with the cover makes four — beyond that a reader
* stops swiping.
*/
public List<CarouselCard> usableCarouselCards() {

if (carouselCards == null) {
    return List.of();
}

return carouselCards.stream()
        .filter(card -> card != null)
        .filter(card -> card.heading() != null && !card.heading().isBlank())
        .limit(3)
        .toList();
}

@com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCarousel() {
        return !usableCarouselCards().isEmpty();
    }

/**
* The icons available to an infographic block.
*
* A fixed set rather than a free description: the renderer draws from
* stored assets, and a name with no asset behind it would leave a gap
* where an icon was promised.
*/
public enum BlockIcon {
CALENDAR,
CLOCK,
PEOPLE,
LOCATION,
MONEY,
DOCUMENT,
LIGHTBULB,
CHECK,
WARNING,
INFO
}

public record VisualSpec(
    /**
     * Main visual subject of the article.
     */
    String subject,

    /**
     * Article-specific background scene.
     *
     * The article decides what this depicts;
     * tone decides how it is visually treated.
     */
    String background,

    /**
     * Emotional treatment of the scene.
     */
    String mood,

    /**
     * Optional DAK mascot action.
     */
    String mascot,

    /**
     * Rendering style identifier.
     */
    String style
) {

/**
 * Backward-compatible constructor for existing callers
 * that still provide the original four fields.
 */
public VisualSpec(
        String subject,
        String mood,
        String mascot,
        String style
) {
    this(
            subject,
            null,
            mood,
            mascot,
            style
    );
}
}
}