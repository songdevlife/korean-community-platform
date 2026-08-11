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
     * Visual arrangement chosen for this content.
     * See {@link LayoutType}. Falls back to STANDARD when absent.
     */
    String layoutType,

    /**
     * Short Korean label shown in the card header, above the divider.
     * Roughly 6-12 characters, noun phrase only, no particles or verbs.
     * Falls back to title when absent.
     */
    String headerTitle,

    String title,
        String headline,
        KeyFact keyFact,

        /**
         * Practical facts a reader needs in order to act, one per block.
         * Only used by INFOGRAPHIC; null everywhere else.
         */
        List<InfoBlock> infoBlocks,

        /**
         * Cards after the first, where the content warrants a carousel.
         * Null for a single card. The first card is described by the fields
         * above; these are the ones that follow it.
         */
        List<CarouselCard> carouselCards,

        VisualSpec visual
) {

/**
 * Supported card layouts.
 *
 * DAK principle: show strongly, do not exaggerate. A layout may make a
 * verified fact visually dominant, but must never add emotion, shock or
 * spectacle beyond what the source states.
 */
public enum LayoutType {

    /** Category, title, illustration, supporting key fact, branding. */
    STANDARD,

    /** Multiple explanatory blocks in a single card. Not yet rendered. */
    INFOGRAPHIC,

    /** A verified figure becomes the visual subject of the card. */
    FACT_HOOK,

    /** Restrained treatment for death, injury, disaster and emergencies. */
    URGENT
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
            String subject,
            String mood,
            String mascot,
            String style
    ) {}
}