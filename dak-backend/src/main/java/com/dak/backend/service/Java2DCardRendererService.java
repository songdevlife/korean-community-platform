package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.AlphaComposite;
import java.awt.GradientPaint;
import java.awt.Paint;

@Service
public class Java2DCardRendererService implements CardRendererService {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1350;

    // Top badge
    private static final int BADGE_X = 72;
    private static final int BADGE_Y = 42;
    // Width is measured from the label rather than fixed: the update badge
    // and the guide badge are different lengths, and a fixed box leaves the
    // shorter one with a stretch of empty red beside it.
    private static final int BADGE_PADDING_X = 32;
    private static final int BADGE_HEIGHT = 72;

    // The badge says what kind of thing the card is. Width is fixed, so a
    // longer label than these would need the box measured rather than set.
    private static final String BADGE_TEXT_AU_UPDATE = "AUSTRALIA UPDATE";
    private static final String BADGE_TEXT_GUIDE = "DAK GUIDE";

    // DAK mascot
    // Drawn at an exact width with the original aspect ratio preserved,
    // anchored at the top-left corner of (MASCOT_X, MASCOT_Y).
    private static final int MASCOT_X = 665;
    private static final int MASCOT_Y = 6;
    private static final int MASCOT_WIDTH = 375;

    // Vertical position of the black ledge line drawn inside the mascot asset,
    // expressed as a ratio of the asset height. The blue divider is aligned to it.
    // Measure once: (y pixel of the black line) / (total image height).
    private static final double MASCOT_BASELINE_RATIO = 0.86;

    // Header title
    private static final int HEADER_TITLE_X = 72;
    // Narrowed because the mascot now starts at x=665.
    private static final int HEADER_TITLE_WIDTH = 565;

    // Sized so that eight or nine characters still fit on one line. The model
    // is asked for six or seven and repeatedly returns more, and a second line
    // here pushes the divider, the hero and everything below them down the
    // card — so the renderer absorbs it rather than the layout doing so.
    private static final int HEADER_TITLE_FONT_SIZE = 64;
    private static final int HEADER_TITLE_MIN_FONT_SIZE = 44;
    private static final int HEADER_TITLE_MAX_LINES = 2;

    // Line height as a multiple of the fitted font size.
    private static final double HEADER_TITLE_LINE_HEIGHT_RATIO = 1.18;

    // Distance from the last header title baseline down to the divider.
    private static final int HEADER_TITLE_GAP_ABOVE_DIVIDER = 34;

    // Blue divider
    private static final int DIVIDER_X = 72;
    private static final int DIVIDER_HEIGHT = 4;
    private static final int DIVIDER_DOT_SIZE = 12;
    private static final int DIVIDER_GAP = 52;
    private static final int DIVIDER_FADE_PADDING = 10;
    private static final int DIVIDER_MIN_RIGHT_LENGTH = 90;
    private static final int DIVIDER_MAX_RIGHT_X = 1008;

    // AI hero image
    private static final int HERO_X = 72;
    private static final int HERO_WIDTH = 936;

    // The hero artwork is trimmed of transparent margins before drawing,
    // so this region is sized for the artwork itself, not the raw file.
    private static final int HERO_BOTTOM_Y = 745;
    private static final int HERO_GAP_BELOW_DIVIDER = 30;

    // Main title
    private static final int TITLE_X = 72;
    private static final int TITLE_Y = 815;
    private static final int TITLE_WIDTH = 936;

    private static final int TITLE_FONT_SIZE = 58;
    private static final int TITLE_LINE_HEIGHT = 70;
    private static final int TITLE_MAX_LINES = 2;

    // Headline
    private static final int HEADLINE_X = 72;
    private static final int HEADLINE_Y = 925;
    private static final int HEADLINE_WIDTH = 936;

    private static final int HEADLINE_FONT_SIZE = 36;
    private static final int HEADLINE_LINE_HEIGHT = 48;
    private static final int HEADLINE_MAX_LINES = 2;

    // Key fact
    private static final int KEY_FACT_X = 72;
    private static final int KEY_FACT_Y = 1020;
    private static final int KEY_FACT_WIDTH = 936;
    private static final int KEY_FACT_HEIGHT = 130;

    private static final int KEY_FACT_LABEL_FONT_SIZE = 28;
    private static final int KEY_FACT_VALUE_FONT_SIZE = 46;
// FACT_HOOK layout
    // A verified figure becomes the visual subject. Elements are stripped back:
    // no header title, no divider, no mascot.
    private static final int HOOK_HERO_X = 72;
    private static final int HOOK_HERO_TOP_Y = 175;
    private static final int HOOK_HERO_BOTTOM_Y = 585;
    private static final int HOOK_HERO_WIDTH = 936;

    private static final int HOOK_CONTENT_X = 72;
    private static final int HOOK_CONTENT_WIDTH = 936;

    private static final int HOOK_LABEL_Y = 685;
    private static final int HOOK_LABEL_FONT_SIZE = 38;

    private static final int HOOK_VALUE_Y = 835;
    private static final int HOOK_VALUE_FONT_SIZE = 140;
    private static final int HOOK_VALUE_MIN_FONT_SIZE = 80;

    private static final int HOOK_TITLE_Y = 955;
    private static final int HOOK_TITLE_FONT_SIZE = 76;
    private static final int HOOK_TITLE_MIN_FONT_SIZE = 48;
    private static final int HOOK_TITLE_MAX_LINES = 2;
    private static final double HOOK_TITLE_LINE_HEIGHT_RATIO = 1.18;

    private static final int HOOK_HEADLINE_Y = 1060;
    private static final int HOOK_HEADLINE_FONT_SIZE = 34;
    private static final int HOOK_HEADLINE_LINE_HEIGHT = 46;
    private static final int HOOK_HEADLINE_MAX_LINES = 2;

    // URGENT layout
    // Shares the figure-led structure, with the emphasis taken out. A death
    // toll is not a headline number: it is set smaller than a FACT_HOOK figure,
    // in the body colour rather than an accent, under a restrained badge.
    // Nothing here should make human harm look like a headline.
    private static final int URGENT_VALUE_FONT_SIZE = 96;
    private static final int URGENT_VALUE_MIN_FONT_SIZE = 60;

    private static final Color URGENT_BADGE =
            new Color(13, 38, 82);

    // INFOGRAPHIC layout
    // No AI hero: the blocks need the height, and a decorative image on a card
    // whose purpose is instruction competes with the instruction.
    private static final int INFO_CONTENT_X = 72;
    private static final int INFO_CONTENT_WIDTH = 936;

    private static final int INFO_HEADLINE_Y = 400;
    private static final int INFO_HEADLINE_FONT_SIZE = 46;
    private static final int INFO_HEADLINE_LINE_HEIGHT = 60;
    private static final int INFO_HEADLINE_MAX_LINES = 2;

    private static final int INFO_BLOCKS_TOP_Y = 500;
    private static final int INFO_BLOCK_GAP = 24;
    private static final int INFO_BLOCK_CORNER = 44;
    private static final int INFO_BLOCK_PADDING = 32;

    // Carousel body cards
    // No AI hero: these are read rather than glanced at, and an illustration
    // per card would multiply the cost of a story by the number of cards.
    private static final int CAROUSEL_CONTENT_X = 72;
    private static final int CAROUSEL_CONTENT_WIDTH = 936;

    // Where the content block may sit. The heading is placed within this band
    // according to how much follows it, rather than at a fixed line: a card
    // with two lines of body and one with five would otherwise leave very
    // different amounts of empty space beneath them.
    private static final int CAROUSEL_BAND_TOP_Y = 360;
    private static final int CAROUSEL_BAND_BOTTOM_Y = 1120;
    private static final int CAROUSEL_HEADING_FONT_SIZE = 76;
    private static final int CAROUSEL_HEADING_MIN_FONT_SIZE = 48;
    private static final int CAROUSEL_HEADING_MAX_LINES = 3;
    private static final double CAROUSEL_HEADING_LINE_HEIGHT_RATIO = 1.24;

    private static final int CAROUSEL_RULE_GAP = 44;
    private static final int CAROUSEL_RULE_WIDTH = 140;
    private static final int CAROUSEL_RULE_HEIGHT = 6;

    private static final int CAROUSEL_BODY_GAP = 48;
    private static final int CAROUSEL_BODY_FONT_SIZE = 40;
    private static final int CAROUSEL_BODY_LINE_HEIGHT = 62;

    // Blocks on a carousel card sit where the body would, at a fixed height
    // each rather than sharing the space as they do on an infographic.
    private static final int CAROUSEL_BLOCK_HEIGHT = 150;
    private static final int CAROUSEL_BLOCK_GAP = 20;


    // The headline on an infographic is the line that says who this concerns,
    // which is the one thing a reader scrolling past needs to catch.
    private static final Color INFO_HEADLINE_COLOR =
            new Color(214, 40, 40);
    private static final int INFO_BLOCK_LABEL_FONT_SIZE = 28;
    private static final int INFO_BLOCK_VALUE_FONT_SIZE = 42;
    private static final int INFO_BLOCK_NOTE_FONT_SIZE = 26;
    private static final int INFO_BLOCK_NOTE_LINE_HEIGHT = 36;

    private static final int LABEL_TO_VALUE_GAP = 16;
    private static final int VALUE_TO_NOTE_GAP = 14;

    // Icon badge sits at the left of a block, with the text beside it.
    private static final int INFO_ICON_DIAMETER = 92;
    private static final int INFO_ICON_GLYPH_SIZE = 54;
    private static final int INFO_ICON_TEXT_GAP = 20;

    // Badge fills, matched to the block tints by index.
    private static final Color[] INFO_ICON_FILLS = {
        new Color(37, 110, 235),
        new Color(240, 145, 12),
        new Color(22, 145, 78),
        new Color(124, 72, 240),
};

    // Blocks are tinted in rotation so adjacent ones separate without any of
    // them reading as a warning or a highlight.
    private static final Color[] INFO_BLOCK_FILLS = {
            new Color(232, 243, 255),
            new Color(255, 244, 230),
            new Color(235, 248, 238),
            new Color(243, 238, 252),
    };

    private static final Color[] INFO_BLOCK_LABELS = {
            new Color(28, 90, 168),
            new Color(176, 98, 20),
            new Color(30, 120, 66),
            new Color(102, 62, 168),
    };


    private static final Color HOOK_ACCENT =
            new Color(211, 47, 47);

    private static final Color TEXT_MUTED =
            new Color(110, 110, 106);
    // AI disclosure
    // Rendered into the image itself so it survives screenshots and re-shares.
    private static final String AI_NOTICE = "AI 생성 이미지";
    private static final int AI_NOTICE_FONT_SIZE = 20;
    private static final int AI_NOTICE_GAP = 14;

    private static final Color TEXT_FAINT =
            new Color(160, 160, 156);

    // Footer
    private static final int FOOTER_TOP_Y = 1180;
    private static final int FOOTER_LOGO_X = 72;
    private static final int FOOTER_LOGO_WIDTH = 340;

    private static final int FOOTER_SEPARATOR_X = 448;
    private static final int FOOTER_SEPARATOR_TOP_INSET = 8;
    private static final int FOOTER_SEPARATOR_BOTTOM_INSET = 8;

    private static final int FOOTER_CHIP_RIGHT_X = 1008;
    private static final int FOOTER_CHIP_HEIGHT = 58;
    private static final int FOOTER_CHIP_GAP = 14;
    private static final int FOOTER_CHIP_PADDING_X = 12;
    private static final int FOOTER_CHIP_TEXT_GAP = 16;
    private static final int FOOTER_CHIP_FONT_SIZE = 28;

    private static final int FOOTER_BADGE_DIAMETER = 42;
    private static final int FOOTER_BADGE_ICON_SIZE = 26;

    private static final String FOOTER_WEBSITE = "discoveradelaidekorea.au";
    private static final String FOOTER_INSTAGRAM = "@discoveradelaidekorea";

    // Outer cream canvas
    private static final Color BACKGROUND =
            new Color(248, 244, 234);

    // Inner white panel that all content sits on
    private static final Color PANEL_BACKGROUND =
            new Color(255, 255, 255);

            private static final int PANEL_MARGIN = 16;
            private static final int PANEL_CORNER_RADIUS = 56;

    private static final Color KEY_FACT_BACKGROUND =
            new Color(232, 243, 255);

            private static final Color TEXT_PRIMARY =
            new Color(28, 28, 26);

    private static final Color CHIP_BLUE =
            new Color(45, 127, 249);

    private static final Color CHIP_INSTAGRAM_START =
            new Color(245, 133, 41);

    private static final Color CHIP_INSTAGRAM_END =
            new Color(221, 42, 123);

    private static final Color FOOTER_SEPARATOR_COLOR =
            new Color(224, 224, 224);

            private final Font regularFont;
            private final Font semiBoldFont;
            private final Font boldFont;
            private final Font extraBoldFont;
            private final BufferedImage dakMascot;
    private final BufferedImage footerLogo;
    private final BufferedImage globeIcon;
    private final BufferedImage instagramIcon;

    // Keyed by CardSpec.BlockIcon name. Missing assets are absent from the
    // map rather than null entries, so a block simply renders without one.
    private final Map<String, BufferedImage> blockIcons;
        
            // Derived from the mascot asset at startup so the divider,
            // header title and hero all follow the mascot's ledge line.
            private final int dividerY;
            private final int headerTitleY;
            private final int heroY;
            private final int heroHeight;

    public Java2DCardRendererService() {
        this.regularFont =
                loadFont("/fonts/Pretendard-Regular.otf");

        this.semiBoldFont =
                loadFont("/fonts/Pretendard-SemiBold.otf");

        this.boldFont =
                loadFont("/fonts/Pretendard-Bold.otf");
        
        this.extraBoldFont =
                loadFont("/fonts/Pretendard-ExtraBold.otf");

                this.dakMascot =
                loadImage("/branding/dak-mascot.png");

        this.footerLogo =
                loadImage("/branding/footer-logo.png");

        this.globeIcon =
                loadImage("/branding/globe.png");

                this.instagramIcon =
                loadImage("/branding/instagram.png");

        this.blockIcons = loadBlockIcons();

        int mascotDrawnHeight = (int) Math.round(
                dakMascot.getHeight()
                        * ((double) MASCOT_WIDTH / dakMascot.getWidth())
        );

        this.dividerY = MASCOT_Y
                + (int) Math.round(mascotDrawnHeight * MASCOT_BASELINE_RATIO);

        this.headerTitleY = dividerY - HEADER_TITLE_GAP_ABOVE_DIVIDER;

        this.heroY = dividerY + HERO_GAP_BELOW_DIVIDER;

        this.heroHeight = HERO_BOTTOM_Y - heroY;
    }
    @Override
    public RenderedCard renderSingle(
            CardSpec cardSpec,
            byte[] heroImage
    ) {

        try {
            BufferedImage canvas = new BufferedImage(
                    WIDTH,
                    HEIGHT,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = canvas.createGraphics();

            try {
                configureGraphics(g);

                // Outer cream background
                g.setColor(BACKGROUND);
                g.fillRect(0, 0, WIDTH, HEIGHT);

                // Inner white panel
                g.setColor(PANEL_BACKGROUND);
                g.fillRoundRect(
                        PANEL_MARGIN,
                        PANEL_MARGIN,
                        WIDTH - (PANEL_MARGIN * 2),
                        HEIGHT - (PANEL_MARGIN * 2),
                        PANEL_CORNER_RADIUS,
                        PANEL_CORNER_RADIUS
                );

                CardSpec.LayoutType layout =
                        cardSpec.effectiveLayoutType();

                BufferedImage trimmedHero = null;

                // INFOGRAPHIC is drawn from blocks and is given no artwork.
                if (layout != CardSpec.LayoutType.INFOGRAPHIC) {

                    BufferedImage hero = ImageIO.read(
                            new ByteArrayInputStream(heroImage)
                    );

                    if (hero == null) {
                        throw new IllegalArgumentException(
                                "Hero image could not be decoded."
                        );
                    }

                    trimmedHero = trimTransparentBorder(hero);
                }

                        if (layout == CardSpec.LayoutType.FACT_HOOK
                            || layout == CardSpec.LayoutType.URGENT) {
    
                        drawFigureLedLayout(g, cardSpec, trimmedHero, layout);
    
                    } else if (layout == CardSpec.LayoutType.INFOGRAPHIC) {
    
                        drawInfographicLayout(g, cardSpec);
    
                    } else {
    
                        drawStandardLayout(g, cardSpec, trimmedHero);
                    }

                drawFooter(g);

            } finally {
                g.dispose();
            }

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    canvas,
                    "png",
                    output
            );

            return new RenderedCard(
                    output.toByteArray(),
                    "image/png",
                    WIDTH,
                    HEIGHT
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to render single card.",
                    e
            );
        }
    }

    @Override
    public RenderedCard renderCarouselCard(
            CardSpec cardSpec,
            byte[] heroImage,
            int index
    ) {

        // The cover is a single card in every respect; only what follows it
        // is drawn differently.
        if (index == 0) {
            return renderSingle(cardSpec, heroImage);
        }

        List<CardSpec.CarouselCard> cards = cardSpec.usableCarouselCards();

        if (index < 1 || index > cards.size()) {
            throw new IllegalArgumentException(
                    "Card " + index + " does not exist in this carousel."
            );
        }

        try {
            BufferedImage canvas = new BufferedImage(
                    WIDTH,
                    HEIGHT,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = canvas.createGraphics();

            try {
                configureGraphics(g);

                g.setColor(BACKGROUND);
                g.fillRect(0, 0, WIDTH, HEIGHT);

                g.setColor(PANEL_BACKGROUND);
                g.fillRoundRect(
                        PANEL_MARGIN,
                        PANEL_MARGIN,
                        WIDTH - (PANEL_MARGIN * 2),
                        HEIGHT - (PANEL_MARGIN * 2),
                        PANEL_CORNER_RADIUS,
                        PANEL_CORNER_RADIUS
                );

                drawCarouselCard(
                        g,
                        cardSpec,
                        cards.get(index - 1)
                );

                drawFooter(g);

            } finally {
                g.dispose();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            ImageIO.write(canvas, "png", output);

            return new RenderedCard(
                    output.toByteArray(),
                    "image/png",
                    WIDTH,
                    HEIGHT
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to render carousel card " + index + ".",
                    e
            );
        }
    }


    private void drawStandardLayout(
        Graphics2D g,
        CardSpec cardSpec,
        BufferedImage hero
) {

    drawHeader(g, cardSpec);

    drawContainedImage(
                g,
                hero,
                HERO_X,
                heroY,
                HERO_WIDTH,
                heroHeight
        );

        drawAiNotice(
                g,
                HERO_X + HERO_WIDTH,
                HERO_BOTTOM_Y + AI_NOTICE_GAP
        );

        drawTitle(g, cardSpec.title());

    drawHeadline(g, cardSpec.headline());

    drawKeyFact(g, cardSpec.keyFact());
}

/**
 * Figure-led layout. The verified value from keyFact becomes the visual
 * subject of the card. Header title, divider and mascot are omitted so
 * nothing competes with it.
 *
 * The value is shown at the size the number needs, not larger. No emphasis
 * is added beyond what the supplied content states.
 */
/**
     * Figure-led layout, used by FACT_HOOK and URGENT.
     *
     * A verified value from keyFact becomes the visual subject; the header
     * title, divider and mascot are dropped so nothing competes with it.
     *
     * URGENT keeps the structure and removes the emphasis. Where FACT_HOOK
     * describes exposure or scale, URGENT describes people who were hurt, and
     * the same treatment applied to both would turn a death toll into the
     * headline number the layout was designed to produce.
     */
private void drawFigureLedLayout(
    Graphics2D g,
    CardSpec cardSpec,
    BufferedImage hero,
    CardSpec.LayoutType layout
) {

boolean urgent = layout == CardSpec.LayoutType.URGENT;

drawBadge(
        g,
        cardSpec,
        urgent ? URGENT_BADGE : new Color(255, 63, 54)
);

drawContainedImage(
        g,
        hero,
        HOOK_HERO_X,
        HOOK_HERO_TOP_Y,
        HOOK_HERO_WIDTH,
        HOOK_HERO_BOTTOM_Y - HOOK_HERO_TOP_Y
);

drawAiNotice(
        g,
        HOOK_HERO_X + HOOK_HERO_WIDTH,
        HOOK_HERO_BOTTOM_Y + AI_NOTICE_GAP
);

CardSpec.KeyFact keyFact = cardSpec.keyFact();

    // Small label above the figure
    if (keyFact.label() != null && !keyFact.label().isBlank()) {

        g.setFont(
                semiBoldFont.deriveFont(
                        Font.PLAIN,
                        (float) HOOK_LABEL_FONT_SIZE
                )
        );

        g.setColor(TEXT_MUTED);

        g.drawString(
                keyFact.label(),
                HOOK_CONTENT_X,
                HOOK_LABEL_Y
        );
    }

    // The figure itself
    Font valueFont = fitWrappedFont(
        g,
        keyFact.value(),
        extraBoldFont,
        urgent ? URGENT_VALUE_FONT_SIZE : HOOK_VALUE_FONT_SIZE,
        urgent ? URGENT_VALUE_MIN_FONT_SIZE : HOOK_VALUE_MIN_FONT_SIZE,
        HOOK_CONTENT_WIDTH,
        1
);

g.setFont(valueFont);
g.setColor(urgent ? TEXT_PRIMARY : HOOK_ACCENT);

    g.drawString(
            keyFact.value(),
            HOOK_CONTENT_X,
            HOOK_VALUE_Y
    );

    // Short Korean title beneath the figure
    String hookTitle = cardSpec.effectiveHeaderTitle();

    if (hookTitle != null && !hookTitle.isBlank()) {

        Font titleFont = fitWrappedFont(
                g,
                hookTitle,
                extraBoldFont,
                HOOK_TITLE_FONT_SIZE,
                HOOK_TITLE_MIN_FONT_SIZE,
                HOOK_CONTENT_WIDTH,
                HOOK_TITLE_MAX_LINES
        );

        g.setFont(titleFont);
        g.setColor(TEXT_PRIMARY);

        int lineHeight = (int) Math.round(
                titleFont.getSize()
                        * HOOK_TITLE_LINE_HEIGHT_RATIO
        );

        int y = HOOK_TITLE_Y;

        for (String line : wrapToFit(
                hookTitle,
                g.getFontMetrics(),
                HOOK_CONTENT_WIDTH
        )) {

            g.drawString(
                    line,
                    HOOK_CONTENT_X,
                    y
            );

            y += lineHeight;
        }
    }

    // Supporting sentence
    String headline = cardSpec.headline();

    if (headline != null && !headline.isBlank()) {

        g.setFont(
                regularFont.deriveFont(
                        Font.PLAIN,
                        (float) HOOK_HEADLINE_FONT_SIZE
                )
        );

        int y = HOOK_HEADLINE_Y;

        for (List<TextRun> line : wrapRuns(
                headline,
                g.getFontMetrics(),
                HOOK_CONTENT_WIDTH
        )) {

            // The figure above already carries the accent here, so a
            // marked phrase in the supporting line uses the body colour
            // rather than competing with it.
            drawRuns(
                    g,
                    line,
                    HOOK_CONTENT_X,
                    y,
                    TEXT_MUTED,
                    TEXT_PRIMARY
            );

            y += HOOK_HEADLINE_LINE_HEIGHT;
        }
    }
}

/**
     * Block layout for content a reader has to act on.
     *
     * Keeps the header — badge, short title, divider, mascot — and drops the
     * AI hero. The blocks need the height, and a decorative illustration on a
     * card whose purpose is instruction competes with the instruction.
     *
     * Blocks are laid out in two columns where an even number allows it, and
     * full width otherwise, so a trailing block never sits beside a gap.
     */
private void drawInfographicLayout(
    Graphics2D g,
    CardSpec cardSpec
) {

drawHeader(g, cardSpec);

// Headline sits above the blocks as the one thing to take away.
String headline = cardSpec.headline();

if (headline != null && !headline.isBlank()) {

    g.setFont(
        extraBoldFont.deriveFont(
                Font.PLAIN,
                (float) INFO_HEADLINE_FONT_SIZE
        )
);

int y = INFO_HEADLINE_Y;

            // The marked phrase takes the accent; the rest stays readable.
            for (List<TextRun> line : wrapRuns(
                    headline,
                    g.getFontMetrics(),
                    INFO_CONTENT_WIDTH
            )) {

                if (y > INFO_HEADLINE_Y
                        + (INFO_HEADLINE_LINE_HEIGHT * (INFO_HEADLINE_MAX_LINES - 1))) {
                    break;
                }

                drawRuns(
                        g,
                        line,
                        INFO_CONTENT_X,
                        y,
                        TEXT_PRIMARY,
                        INFO_HEADLINE_COLOR
                );

                y += INFO_HEADLINE_LINE_HEIGHT;
            }
}

List<CardSpec.InfoBlock> blocks = cardSpec.usableInfoBlocks();

if (blocks.isEmpty()) {
    return;
}

// Two columns only where the count divides evenly. Three blocks in two
// columns leaves one beside empty space, which reads as a missing
// block rather than a deliberate one.
boolean twoColumns = blocks.size() % 2 == 0;

int availableHeight = FOOTER_TOP_Y - INFO_BLOCKS_TOP_Y - INFO_BLOCK_GAP;

int rows = twoColumns
        ? blocks.size() / 2
        : blocks.size();

int blockHeight =
        (availableHeight - (INFO_BLOCK_GAP * (rows - 1))) / rows;

int blockWidth = twoColumns
        ? (INFO_CONTENT_WIDTH - INFO_BLOCK_GAP) / 2
        : INFO_CONTENT_WIDTH;

for (int i = 0; i < blocks.size(); i++) {

    int row = twoColumns ? i / 2 : i;
    int column = twoColumns ? i % 2 : 0;

    int x = INFO_CONTENT_X
            + (column * (blockWidth + INFO_BLOCK_GAP));

    int y = INFO_BLOCKS_TOP_Y
            + (row * (blockHeight + INFO_BLOCK_GAP));

    drawInfoBlock(
            g,
            blocks.get(i),
            i,
            x,
            y,
            blockWidth,
            blockHeight
    );
}
}

/**
     * A card a reader swipes to.
     *
     * The badge and mascot stay so a card lifted out of the set still reads
     * as DAK's, but there is no header title or divider: the heading below
     * does that work, and repeating the cover's title would waste the space
     * this card exists to provide.
     */
private void drawCarouselCard(
        Graphics2D g,
        CardSpec cardSpec,
        CardSpec.CarouselCard card
) {
    
            drawBadge(g, cardSpec);

    drawImageAtWidth(
            g,
            dakMascot,
            MASCOT_X,
            MASCOT_Y,
            MASCOT_WIDTH
    );

    String heading = stripMarkers(card.heading());

    Font headingFont = fitWrappedFont(
            g,
            heading,
        extraBoldFont,
        CAROUSEL_HEADING_FONT_SIZE,
        CAROUSEL_HEADING_MIN_FONT_SIZE,
        CAROUSEL_CONTENT_WIDTH,
        CAROUSEL_HEADING_MAX_LINES
);

List<String> headingLines = wrapToFit(
        card.heading(),
        g.getFontMetrics(headingFont),
        CAROUSEL_CONTENT_WIDTH
);

int lineHeight = (int) Math.round(
        headingFont.getSize() * CAROUSEL_HEADING_LINE_HEIGHT_RATIO
);

List<CardSpec.InfoBlock> blocks =
        card.blocks() == null ? List.of() : card.blocks();

Font bodyFont = regularFont.deriveFont(
        Font.PLAIN,
        (float) CAROUSEL_BODY_FONT_SIZE
);

List<List<TextRun>> bodyLines =
                (blocks.isEmpty() && card.body() != null && !card.body().isBlank())
                        ? wrapRuns(
                                card.body(),
                                g.getFontMetrics(bodyFont),
                                CAROUSEL_CONTENT_WIDTH
                        )
                        : List.of();

// Measure the whole block before drawing any of it, so a short card
// and a long one are both centred in the band rather than both
// starting at the same line.
int contentHeight =
        (lineHeight * headingLines.size())
                + CAROUSEL_RULE_GAP
                + CAROUSEL_RULE_HEIGHT
                + CAROUSEL_BODY_GAP;

if (!blocks.isEmpty()) {
    int count = Math.min(blocks.size(), 4);
    contentHeight += (CAROUSEL_BLOCK_HEIGHT * count)
            + (CAROUSEL_BLOCK_GAP * (count - 1));
} else {
    contentHeight += CAROUSEL_BODY_LINE_HEIGHT * bodyLines.size();
}

int band = CAROUSEL_BAND_BOTTOM_Y - CAROUSEL_BAND_TOP_Y;

int y = CAROUSEL_BAND_TOP_Y
        + Math.max(0, (band - contentHeight) / 2)
        + headingFont.getSize();

g.setFont(headingFont);
g.setColor(TEXT_PRIMARY);

for (String line : headingLines) {
        g.drawString(line, CAROUSEL_CONTENT_X, y);
        y += lineHeight;
    }
    
    // Short rule under the heading, in place of the header divider.
    y += CAROUSEL_RULE_GAP - lineHeight;
    
        g.setColor(new Color(117, 190, 245));

    g.fillRoundRect(
            CAROUSEL_CONTENT_X,
            y,
            CAROUSEL_RULE_WIDTH,
            CAROUSEL_RULE_HEIGHT,
            CAROUSEL_RULE_HEIGHT,
            CAROUSEL_RULE_HEIGHT
    );

    y += CAROUSEL_BODY_GAP;

        if (!blocks.isEmpty()) {

        for (int i = 0; i < Math.min(blocks.size(), 4); i++) {

            drawInfoBlock(
                    g,
                    blocks.get(i),
                    i,
                    CAROUSEL_CONTENT_X,
                    y,
                    CAROUSEL_CONTENT_WIDTH,
                    CAROUSEL_BLOCK_HEIGHT
            );

            y += CAROUSEL_BLOCK_HEIGHT + CAROUSEL_BLOCK_GAP;
        }

        return;
    }

    if (bodyLines.isEmpty()) {
        return;
    }

    g.setFont(bodyFont);

    int bottomLimit = FOOTER_TOP_Y - CAROUSEL_BODY_LINE_HEIGHT;

    for (List<TextRun> line : bodyLines) {

        if (y > bottomLimit) {
            break;
        }

        drawRuns(
                g,
                line,
                CAROUSEL_CONTENT_X,
                y,
                TEXT_PRIMARY,
                HOOK_ACCENT
        );

        y += CAROUSEL_BODY_LINE_HEIGHT;
    }
}


private void drawInfoBlock(
    Graphics2D g,
    CardSpec.InfoBlock block,
    int index,
    int x,
    int y,
    int width,
    int height
) {

Color fill = INFO_BLOCK_FILLS[index % INFO_BLOCK_FILLS.length];
Color labelColour = INFO_BLOCK_LABELS[index % INFO_BLOCK_LABELS.length];

g.setColor(fill);

g.fillRoundRect(
        x,
        y,
        width,
        height,
        INFO_BLOCK_CORNER,
        INFO_BLOCK_CORNER
);

BufferedImage icon =
                block.icon() == null
                        ? null
                        : blockIcons.get(block.icon().trim().toUpperCase());

        int textX = x + INFO_BLOCK_PADDING;
        int textWidth = width - (INFO_BLOCK_PADDING * 2);

        if (icon != null) {
            textX += INFO_ICON_DIAMETER + INFO_ICON_TEXT_GAP;
            textWidth -= INFO_ICON_DIAMETER + INFO_ICON_TEXT_GAP;
        }

        boolean hasLabel =
                block.label() != null && !block.label().isBlank();

        boolean hasNote =
                block.note() != null && !block.note().isBlank();

        Font labelFont = semiBoldFont.deriveFont(
                Font.PLAIN,
                (float) INFO_BLOCK_LABEL_FONT_SIZE
        );

// One line where the text allows it: a two-line value in one block and
        // a one-line value in the next reads as two different kinds of block.
        Font valueFont = fitWrappedFont(
                g,
                block.value(),
                extraBoldFont,
                INFO_BLOCK_VALUE_FONT_SIZE,
                24,
                textWidth,
                1
        );

        // A value with no spaces cannot be wrapped, so fitting it to a line
        // count achieves nothing and it runs past the block edge. Shrink
        // until it actually measures short enough.
        while (valueFont.getSize() > 16
                && g.getFontMetrics(valueFont).stringWidth(block.value()) > textWidth) {

            valueFont = extraBoldFont.deriveFont(
                    Font.PLAIN,
                    (float) (valueFont.getSize() - 1)
            );
        }

        Font noteFont = regularFont.deriveFont(
                Font.PLAIN,
                (float) INFO_BLOCK_NOTE_FONT_SIZE
        );

        List<String> valueLines = wrapToFit(
                block.value(),
                g.getFontMetrics(valueFont),
                textWidth
        );

        List<String> noteLines = hasNote
                ? wrapToFit(block.note(), g.getFontMetrics(noteFont), textWidth)
                : List.of();

        // Measure before drawing. Blocks share a height, so a block with no
        // note would otherwise sit against its top edge while the one beside
        // it filled the space — which reads as a block missing its last line.
        int valueLineHeight =
                (int) Math.round(valueFont.getSize() * 1.22);

        int contentHeight = 0;

        if (hasLabel) {
            contentHeight += INFO_BLOCK_LABEL_FONT_SIZE + LABEL_TO_VALUE_GAP;
        }

        contentHeight += valueLineHeight * valueLines.size();

        if (!noteLines.isEmpty()) {
            contentHeight += VALUE_TO_NOTE_GAP
                    + (INFO_BLOCK_NOTE_LINE_HEIGHT * noteLines.size());
        }

        int available = height - (INFO_BLOCK_PADDING * 2);

        int cursor = y + INFO_BLOCK_PADDING
                + Math.max(0, (available - contentHeight) / 2);

        if (icon != null) {

            // Centred against the block rather than against the text, so a
            // one-line block and a three-line one look alike in a row.
            drawIconBadge(
                    g,
                    icon,
                    INFO_ICON_FILLS[index % INFO_ICON_FILLS.length],
                    x + INFO_BLOCK_PADDING,
                    y + (height - INFO_ICON_DIAMETER) / 2
            );
        }

        if (hasLabel) {

            g.setFont(labelFont);
            g.setColor(labelColour);

            cursor += g.getFontMetrics().getAscent();

            g.drawString(block.label(), textX, cursor);

            cursor += (INFO_BLOCK_LABEL_FONT_SIZE
                    - g.getFontMetrics().getAscent())
                    + LABEL_TO_VALUE_GAP;
        }

        g.setFont(valueFont);
        g.setColor(TEXT_PRIMARY);

        for (String line : valueLines) {

            cursor += g.getFontMetrics().getAscent();

            g.drawString(line, textX, cursor);

            cursor += valueLineHeight - g.getFontMetrics().getAscent();
        }

        if (noteLines.isEmpty()) {
            return;
        }

        g.setFont(noteFont);
        g.setColor(TEXT_MUTED);

        cursor += VALUE_TO_NOTE_GAP;

        int bottomLimit = y + height - INFO_BLOCK_PADDING;

        for (String line : noteLines) {

            cursor += g.getFontMetrics().getAscent();

            // Silently drops what will not fit rather than overflowing the
            // block. A clipped note is recoverable; a note printed across the
            // block below it is not.
            if (cursor > bottomLimit) {
                break;
            }

            g.drawString(line, textX, cursor);

            cursor += INFO_BLOCK_NOTE_LINE_HEIGHT
                    - g.getFontMetrics().getAscent();
        }
    }


    private void configureGraphics(Graphics2D g) {

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
    }

    private void drawContainedImage(
            Graphics2D g,
            BufferedImage image,
            int x,
            int y,
            int width,
            int height
    ) {

        double scale = Math.min(
                (double) width / image.getWidth(),
                (double) height / image.getHeight()
        );

        int drawWidth =
                (int) Math.round(image.getWidth() * scale);

        int drawHeight =
                (int) Math.round(image.getHeight() * scale);

        int drawX =
                x + (width - drawWidth) / 2;

        int drawY =
                y + (height - drawHeight) / 2;

        g.drawImage(
                image,
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                null
        );
    }

    /**
     * Crops fully transparent rows and columns from the edges of an image.
     * Returns the original image when it has no alpha channel or is empty.
     */
    private BufferedImage trimTransparentBorder(BufferedImage image) {

        if (!image.getColorModel().hasAlpha()) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        // Ignore near-invisible pixels so faint compression noise
        // does not defeat the crop.
        final int alphaThreshold = 8;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int alpha = (image.getRGB(x, y) >>> 24);

                if (alpha <= alphaThreshold) {
                    continue;
                }

                if (x < minX) {
                    minX = x;
                }

                if (x > maxX) {
                    maxX = x;
                }

                if (y < minY) {
                    minY = y;
                }

                if (y > maxY) {
                    maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        return image.getSubimage(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );
    }

    private void drawImageAtWidth(
        Graphics2D g,
        BufferedImage image,
        int x,
        int y,
        int width
) {

    int height = (int) Math.round(
            image.getHeight()
                    * ((double) width / image.getWidth())
    );

    g.drawImage(
            image,
            x,
            y,
            width,
            height,
            null
    );
}

    private void drawTitle(
            Graphics2D g,
            String title
    ) {

        if (title == null || title.isBlank()) {
            return;
        }

        // Shrinks to fit rather than failing. A title one word too long used
        // to throw, which meant the whole render returned 500 and the caller
        // received JSON where it expected a PNG.
        title = stripMarkers(title);

        Font font = fitWrappedFont(
                g,
                title,
            extraBoldFont,
            TITLE_FONT_SIZE,
            40,
            TITLE_WIDTH,
            TITLE_MAX_LINES
    );

    g.setFont(font);
    g.setColor(TEXT_PRIMARY);

    List<String> lines =
            wrapToFit(
                    title,
                    g.getFontMetrics(),
                    TITLE_WIDTH
            );

    int y = TITLE_Y;

        for (String line : lines) {
            g.drawString(
                    line,
                    TITLE_X,
                    y
            );

            y += TITLE_LINE_HEIGHT;
        }
    }
    private void drawHeadline(
        Graphics2D g,
        String headline
) {

    if (headline == null || headline.isBlank()) {
        return;
    }

// Markers are not drawn, so the fitted size has to be measured without
    // them or a marked headline would shrink further than it needs to.
    Font font = fitWrappedFont(
        g,
        stripMarkers(headline),
        semiBoldFont,
        HEADLINE_FONT_SIZE,
        26,
        HEADLINE_WIDTH,
        HEADLINE_MAX_LINES
);

g.setFont(font);

int y = HEADLINE_Y;

for (List<TextRun> line : wrapRuns(
        headline,
        g.getFontMetrics(),
        HEADLINE_WIDTH
)) {

    drawRuns(
            g,
            line,
            HEADLINE_X,
            y,
            TEXT_PRIMARY,
            HOOK_ACCENT
    );

    y += HEADLINE_LINE_HEIGHT;
}
}

        private void drawKeyFact(
            Graphics2D g,
            CardSpec.KeyFact keyFact
        ) {

        if (keyFact == null) {
            return;
        }

        g.setColor(KEY_FACT_BACKGROUND);
        g.fillRoundRect(
                KEY_FACT_X,
                KEY_FACT_Y,
                KEY_FACT_WIDTH,
                KEY_FACT_HEIGHT,
                32,
                32
        );

        g.setColor(TEXT_PRIMARY);

        Font labelFont = semiBoldFont.deriveFont(
                Font.PLAIN,
                (float) KEY_FACT_LABEL_FONT_SIZE
        );

        g.setFont(labelFont);

        if (keyFact.label() != null && !keyFact.label().isBlank()) {
            g.drawString(
                    keyFact.label(),
                    KEY_FACT_X + 36,
                    KEY_FACT_Y + 46
            );
        }

        Font valueFont = extraBoldFont.deriveFont(
            Font.PLAIN,
            (float) KEY_FACT_VALUE_FONT_SIZE
            );

        g.setFont(valueFont);

        if (keyFact.value() != null && !keyFact.value().isBlank()) {
            g.drawString(
                    keyFact.value(),
                    KEY_FACT_X + 36,
                    KEY_FACT_Y + 100
            );
        }
        }
/**
     * Right-aligned disclosure that the illustration was AI-generated.
     *
     * This is drawn into the card rather than left to the post caption,
     * because captions are lost when a card is screenshotted or re-shared.
     */
private void drawAiNotice(
    Graphics2D g,
    int rightX,
    int baselineY
) {

Font font = regularFont.deriveFont(
        Font.PLAIN,
        (float) AI_NOTICE_FONT_SIZE
);

g.setFont(font);
g.setColor(TEXT_FAINT);

int textWidth = g.getFontMetrics().stringWidth(AI_NOTICE);

g.drawString(
        AI_NOTICE,
        rightX - textWidth,
        baselineY
);
}
        private void drawFooter(Graphics2D g) {

            int chipTopY = FOOTER_TOP_Y;
    
            int chipBottomY =
                    chipTopY + FOOTER_CHIP_HEIGHT + FOOTER_CHIP_GAP;
    
            int footerBottomY = chipBottomY + FOOTER_CHIP_HEIGHT;
    
            // Brand lockup, vertically centred against the chip stack
            int logoHeight = (int) Math.round(
                    footerLogo.getHeight()
                            * ((double) FOOTER_LOGO_WIDTH / footerLogo.getWidth())
            );
    
            int logoY =
                    chipTopY + ((footerBottomY - chipTopY) - logoHeight) / 2;
    
            g.drawImage(
                    footerLogo,
                    FOOTER_LOGO_X,
                    logoY,
                    FOOTER_LOGO_WIDTH,
                    logoHeight,
                    null
            );
    
            // Vertical separator
            g.setColor(FOOTER_SEPARATOR_COLOR);
    
            g.fillRect(
                    FOOTER_SEPARATOR_X,
                    chipTopY + FOOTER_SEPARATOR_TOP_INSET,
                    2,
                    (footerBottomY - chipTopY)
                            - FOOTER_SEPARATOR_TOP_INSET
                            - FOOTER_SEPARATOR_BOTTOM_INSET
            );
    
            Font chipFont = semiBoldFont.deriveFont(
                Font.PLAIN,
                (float) FOOTER_CHIP_FONT_SIZE
        );

        // Both chips share the width of the wider label so they align.
        int chipWidth = Math.max(
                measureChipWidth(g, FOOTER_WEBSITE, chipFont),
                measureChipWidth(g, FOOTER_INSTAGRAM, chipFont)
        );

        drawFooterChip(
                g,
                chipTopY,
                chipWidth,
                FOOTER_WEBSITE,
                globeIcon,
                chipFont,
                CHIP_BLUE,
                null
        );

        drawFooterChip(
                g,
                chipBottomY,
                chipWidth,
                FOOTER_INSTAGRAM,
                instagramIcon,
                chipFont,
                CHIP_INSTAGRAM_START,
                CHIP_INSTAGRAM_END
        );
    }

    private int measureChipWidth(
            Graphics2D g,
            String text,
            Font font
    ) {

        return FOOTER_CHIP_PADDING_X
                + FOOTER_BADGE_DIAMETER
                + FOOTER_CHIP_TEXT_GAP
                + g.getFontMetrics(font).stringWidth(text)
                + FOOTER_CHIP_PADDING_X
                + 12;
    }
    
        /**
         * Draws one right-aligned footer chip. When endColour is null the chip is
         * filled with a solid startColour, otherwise a horizontal gradient is used.
         */
        private void drawFooterChip(
            Graphics2D g,
            int y,
            int chipWidth,
            String text,
            BufferedImage icon,
            Font font,
            Color startColour,
            Color endColour
    ) {

        FontMetrics metrics = g.getFontMetrics(font);

        int x = FOOTER_CHIP_RIGHT_X - chipWidth;
    
            Paint oldPaint = g.getPaint();
    
            if (endColour == null) {
                g.setPaint(startColour);
            } else {
                g.setPaint(
                        new GradientPaint(
                                x,
                                y,
                                startColour,
                                x + chipWidth,
                                y,
                                endColour
                        )
                );
            }
    
            g.fillRoundRect(
                    x,
                    y,
                    chipWidth,
                    FOOTER_CHIP_HEIGHT,
                    FOOTER_CHIP_HEIGHT,
                    FOOTER_CHIP_HEIGHT
            );
    
            g.setPaint(oldPaint);
    
            // White badge with the icon knocked out of it
            int badgeX = x + FOOTER_CHIP_PADDING_X;
    
            int badgeY =
                    y + (FOOTER_CHIP_HEIGHT - FOOTER_BADGE_DIAMETER) / 2;
    
            drawKnockoutBadge(
                    g,
                    icon,
                    badgeX,
                    badgeY
            );
    
            // Chip label
            g.setFont(font);
            g.setColor(Color.WHITE);
    
            int textX =
                    badgeX
                            + FOOTER_BADGE_DIAMETER
                            + FOOTER_CHIP_TEXT_GAP;
    
            int textY =
                    y
                            + (FOOTER_CHIP_HEIGHT
                            - metrics.getHeight()) / 2
                            + metrics.getAscent();
    
            g.drawString(
                    text,
                    textX,
                    textY
            );
        }
    
        /**
         * Renders a solid white circle and then removes the icon shape from it,
         * so the chip colour shows through the icon. This lets a single white
         * icon asset work on any chip background.
         */
        private void drawKnockoutBadge(
                Graphics2D g,
                BufferedImage icon,
                int x,
                int y
        ) {
    
            BufferedImage badge = new BufferedImage(
                    FOOTER_BADGE_DIAMETER,
                    FOOTER_BADGE_DIAMETER,
                    BufferedImage.TYPE_INT_ARGB
            );
    
            Graphics2D badgeGraphics = badge.createGraphics();
    
            try {
                configureGraphics(badgeGraphics);
    
                badgeGraphics.setColor(Color.WHITE);
    
                badgeGraphics.fillOval(
                        0,
                        0,
                        FOOTER_BADGE_DIAMETER,
                        FOOTER_BADGE_DIAMETER
                );
    
                badgeGraphics.setComposite(
                        AlphaComposite.DstOut
                );
    
                int offset =
                        (FOOTER_BADGE_DIAMETER - FOOTER_BADGE_ICON_SIZE) / 2;
    
                badgeGraphics.drawImage(
                        icon,
                        offset,
                        offset,
                        FOOTER_BADGE_ICON_SIZE,
                        FOOTER_BADGE_ICON_SIZE,
                        null
                );
    
            } finally {
                badgeGraphics.dispose();
            }
    
            g.drawImage(
                    badge,
                    x,
                    y,
                    null
            );
        }
/**
     * Coloured circle with the icon knocked out of it.
     *
     * The same approach as the footer chips: one white asset works on any
     * background, because what is drawn is the circle minus the glyph.
     */
private void drawIconBadge(
    Graphics2D g,
    BufferedImage icon,
    Color fill,
    int x,
    int y
) {

BufferedImage badge = new BufferedImage(
        INFO_ICON_DIAMETER,
        INFO_ICON_DIAMETER,
        BufferedImage.TYPE_INT_ARGB
);

Graphics2D badgeGraphics = badge.createGraphics();

try {
    configureGraphics(badgeGraphics);

    badgeGraphics.setColor(fill);

    badgeGraphics.fillOval(
            0,
            0,
            INFO_ICON_DIAMETER,
            INFO_ICON_DIAMETER
    );

    badgeGraphics.setComposite(AlphaComposite.DstOut);

    int offset =
            (INFO_ICON_DIAMETER - INFO_ICON_GLYPH_SIZE) / 2;

    badgeGraphics.drawImage(
            icon,
            offset,
            offset,
            INFO_ICON_GLYPH_SIZE,
            INFO_ICON_GLYPH_SIZE,
            null
    );

} finally {
    badgeGraphics.dispose();
}

g.drawImage(badge, x, y, null);
}
        private List<String> wrapText(
            String text,
            FontMetrics metrics,
            int maxWidth,
            int maxLines
    ) {
    
        List<String> lines = new ArrayList<>();
    
        String[] words = text.trim().split("\\s+");
    
        StringBuilder currentLine = new StringBuilder();
    
        for (String word : words) {
    
            String candidate =
                    currentLine.isEmpty()
                            ? word
                            : currentLine + " " + word;
    
            if (metrics.stringWidth(candidate) <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }
    
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
    
            if (lines.size() >= maxLines) {
                throw new IllegalArgumentException(
                        "Text exceeds "
                                + maxLines
                                + " lines."
                );
            }
    
            if (metrics.stringWidth(word) <= maxWidth) {
                currentLine.append(word);
            } else {
                // Fallback for an unusually long word with no spaces.
                StringBuilder partial = new StringBuilder();
    
                for (int i = 0; i < word.length(); i++) {
    
                    char character = word.charAt(i);
    
                    String partialCandidate =
                            partial.toString() + character;
    
                    if (metrics.stringWidth(partialCandidate) <= maxWidth) {
                        partial.append(character);
                        continue;
                    }
    
                    if (!partial.isEmpty()) {
                        lines.add(partial.toString());
                        partial.setLength(0);
                    }
    
                    if (lines.size() >= maxLines) {
                        throw new IllegalArgumentException(
                                "Text exceeds "
                                        + maxLines
                                        + " lines."
                        );
                    }
    
                    partial.append(character);
                }
    
                currentLine.append(partial);
            }
        }
    
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
    
        if (lines.size() > maxLines) {
            throw new IllegalArgumentException(
                    "Text exceeds "
                            + maxLines
                            + " lines."
            );
        }
    
        return lines;
    }

    private void drawBadge(Graphics2D g, CardSpec cardSpec) {

        drawBadge(g, cardSpec, new Color(255, 63, 54));
    }

    private void drawBadge(
        Graphics2D g,
        CardSpec cardSpec,
        Color background
) {

    String label = badgeText(cardSpec);

    Font badgeFont = extraBoldFont.deriveFont(
            Font.PLAIN,
            34f
    );

    int labelWidth = g.getFontMetrics(badgeFont).stringWidth(label);

    g.setColor(background);

    g.fillRoundRect(
            BADGE_X,
            BADGE_Y,
            labelWidth + (BADGE_PADDING_X * 2),
            BADGE_HEIGHT,
            36,
            36
    );

    g.setFont(badgeFont);
    g.setColor(Color.WHITE);

    g.drawString(
            label,
            BADGE_X + BADGE_PADDING_X,
            BADGE_Y + 49
    );
}

    /**
     * Falls back to the update label rather than leaving the badge empty:
     * an unlabelled card is worse than one labelled as the commoner kind.
     */
    private String badgeText(CardSpec cardSpec) {

        return "GUIDE".equals(cardSpec.contentType())
                ? BADGE_TEXT_GUIDE
                : BADGE_TEXT_AU_UPDATE;
    }

    private void drawHeader(
        Graphics2D g,
        CardSpec cardSpec
) {

    drawBadge(g, cardSpec);

        // Mascot
    drawImageAtWidth(
            g,
            dakMascot,
            MASCOT_X,
            MASCOT_Y,
            MASCOT_WIDTH
    );

    // Header title
    String headerTitle = cardSpec.effectiveHeaderTitle();

    int headerTitleEndX = HEADER_TITLE_X;

    if (headerTitle != null && !headerTitle.isBlank()) {

        Font fittedHeaderFont = fitWrappedFont(
                g,
                headerTitle,
                extraBoldFont,
                HEADER_TITLE_FONT_SIZE,
                HEADER_TITLE_MIN_FONT_SIZE,
                HEADER_TITLE_WIDTH,
                HEADER_TITLE_MAX_LINES
        );

        g.setFont(fittedHeaderFont);
        g.setColor(new Color(13, 38, 82));

        List<String> lines = wrapToFit(
                headerTitle,
                g.getFontMetrics(),
                HEADER_TITLE_WIDTH
        );

        int lineHeight = (int) Math.round(
                fittedHeaderFont.getSize()
                        * HEADER_TITLE_LINE_HEIGHT_RATIO
        );

        // The block grows upward so the last baseline stays
        // a fixed distance above the divider.
        int y = headerTitleY - (lineHeight * (lines.size() - 1));

        for (String line : lines) {

            g.drawString(
                    line,
                    HEADER_TITLE_X,
                    y
            );

            int lineEndX =
                    HEADER_TITLE_X
                            + g.getFontMetrics().stringWidth(line);

            headerTitleEndX = Math.max(headerTitleEndX, lineEndX);

            y += lineHeight;
        }
    }

    drawDivider(g, headerTitleEndX);
}

private void drawDivider(
        Graphics2D g,
        int headerTitleEndX
) {

    Color dividerBlue = new Color(117, 190, 245);

    int dotX = WIDTH / 2 - DIVIDER_DOT_SIZE / 2;

    // Left solid line, stopping short of the centre dot
    int leftEndX = dotX - DIVIDER_GAP;

    g.setColor(dividerBlue);

    g.fillRoundRect(
        DIVIDER_X,
        dividerY,
        leftEndX - DIVIDER_X,
        DIVIDER_HEIGHT,
        DIVIDER_HEIGHT,
        DIVIDER_HEIGHT
);

// Centre dot
g.fillOval(
        dotX,
        dividerY - (DIVIDER_DOT_SIZE - DIVIDER_HEIGHT) / 2,
        DIVIDER_DOT_SIZE,
        DIVIDER_DOT_SIZE
);

    // Right line, fading out just past the end of the header title
    int rightStartX = dotX + DIVIDER_DOT_SIZE + DIVIDER_GAP;

    int rightEndX = Math.min(
            headerTitleEndX + DIVIDER_FADE_PADDING,
            DIVIDER_MAX_RIGHT_X
    );

    rightEndX = Math.max(
            rightEndX,
            rightStartX + DIVIDER_MIN_RIGHT_LENGTH
    );

    GradientPaint fade = new GradientPaint(
        rightStartX,
        dividerY,
        dividerBlue,
        rightEndX,
        dividerY,
        new Color(
                dividerBlue.getRed(),
                dividerBlue.getGreen(),
                dividerBlue.getBlue(),
                0
        )
);

Paint oldPaint = g.getPaint();

g.setPaint(fade);

g.fillRect(
        rightStartX,
        dividerY,
        rightEndX - rightStartX,
        DIVIDER_HEIGHT
);

    g.setPaint(oldPaint);
}

    private Font loadFont(String resourcePath) {

        try (
                InputStream input =
                        getClass().getResourceAsStream(resourcePath)
        ) {

            if (input == null) {
                throw new IllegalStateException(
                        "Font resource not found: "
                                + resourcePath
                );
            }

            return Font.createFont(
                    Font.TRUETYPE_FONT,
                    input
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load font: "
                            + resourcePath,
                    e
            );
        }
    }

    /**
     * Finds the largest font size at which the text wraps into
     * no more than maxLines lines within maxWidth.
     */
    private Font fitWrappedFont(
        Graphics2D g,
        String text,
        Font baseFont,
        int preferredSize,
        int minimumSize,
        int maxWidth,
        int maxLines
) {

    for (int size = preferredSize; size >= minimumSize; size--) {

        Font candidate = baseFont.deriveFont(
                Font.PLAIN,
                (float) size
        );

        List<String> lines = wrapToFit(
                text,
                g.getFontMetrics(candidate),
                maxWidth
        );

        if (lines.size() <= maxLines) {
            return candidate;
        }
    }

    // Falls back to the minimum size rather than failing the whole render.
    return baseFont.deriveFont(
            Font.PLAIN,
            (float) minimumSize
    );
}

/**
 * Greedy word wrap with no line limit. Unlike wrapText this never throws,
 * so it can be used to measure how many lines a given font size needs.
 */
private List<String> wrapToFit(
        String text,
        FontMetrics metrics,
        int maxWidth
) {

    List<String> lines = new ArrayList<>();

    StringBuilder currentLine = new StringBuilder();

    for (String word : text.trim().split("\\s+")) {

        String candidate =
                currentLine.isEmpty()
                        ? word
                        : currentLine + " " + word;

        if (metrics.stringWidth(candidate) <= maxWidth) {
            currentLine.setLength(0);
            currentLine.append(candidate);
            continue;
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
            currentLine.setLength(0);
        }

        currentLine.append(word);
    }

    if (!currentLine.isEmpty()) {
        lines.add(currentLine.toString());
    }

    if (lines.isEmpty()) {
        lines.add(text.trim());
    }

    return rebalanceOrphan(lines, metrics, maxWidth);
}

/**
 * Pulls one word down when the last line holds a single word.
 *
 * Greedy wrapping fills each line to the limit, which regularly leaves a
 * Korean title ending in a one-syllable 어절 alone on its own line. The
 * line count is unchanged, so this is safe to run inside the measurement
 * path used by fitWrappedFont.
 */
private List<String> rebalanceOrphan(
        List<String> lines,
        FontMetrics metrics,
        int maxWidth
) {

    if (lines.size() < 2) {
        return lines;
    }

    String last = lines.get(lines.size() - 1);

    // Already more than one word: nothing to rescue.
    if (last.contains(" ")) {
        return lines;
    }

    String previous = lines.get(lines.size() - 2);

    int split = previous.lastIndexOf(' ');

    // Moving the only word off the previous line would empty it.
    if (split < 0) {
        return lines;
    }

    String moved = previous.substring(split + 1);

    String adjustedLast = moved + " " + last;

    // The word being moved has to actually fit alongside the orphan.
    if (metrics.stringWidth(adjustedLast) > maxWidth) {
        return lines;
    }

    List<String> adjusted = new ArrayList<>(lines);

    adjusted.set(adjusted.size() - 2, previous.substring(0, split));
    adjusted.set(adjusted.size() - 1, adjustedLast);

    return adjusted;
}


/**
     * One run of headline text, and whether it carries the accent colour.
     *
     * The model marks a phrase with double asterisks. Splitting on those
     * rather than colouring the whole headline is what makes the emphasis
     * mean anything — a line entirely in the accent colour emphasises
     * nothing.
     */
private record TextRun(String text, boolean accent) {}

/**
 * Splits **marked** phrases out of a line into runs.
 *
 * Unmatched asterisks are left as literal text rather than swallowed:
 * a stray marker is visible and can be corrected, where silently
 * dropping it hides the mistake.
 */
private List<TextRun> parseRuns(String text) {

    List<TextRun> runs = new ArrayList<>();

    int cursor = 0;

    while (cursor < text.length()) {

        int open = text.indexOf("**", cursor);

        if (open < 0) {
            runs.add(new TextRun(text.substring(cursor), false));
            break;
        }

        int close = text.indexOf("**", open + 2);

        if (close < 0) {
            runs.add(new TextRun(text.substring(cursor), false));
            break;
        }

        if (open > cursor) {
            runs.add(new TextRun(text.substring(cursor, open), false));
        }

        runs.add(new TextRun(text.substring(open + 2, close), true));

        cursor = close + 2;
    }

    return runs;
}

/** The same text with the markers removed, for measuring and wrapping. */
private String stripMarkers(String text) {

    if (text == null) {
        return null;
    }

    return text.replace("**", "");
}

/**
 * Draws a line that may contain accented runs, and returns where the
 * next line should start on the x axis is irrelevant — the caller
 * advances y itself.
 *
 * Runs are drawn one after another rather than as one string, because
 * only the marked ones take the accent colour.
 */
private void drawRuns(
        Graphics2D g,
        List<TextRun> runs,
        int x,
        int baseline,
        Color plain,
        Color accent
) {

    int cursor = x;

    for (TextRun run : runs) {

        g.setColor(run.accent() ? accent : plain);

        g.drawString(run.text(), cursor, baseline);

        cursor += g.getFontMetrics().stringWidth(run.text());
    }
}

/**
 * Wraps text that carries markers, keeping each line's runs intact.
 *
 * Wrapping has to happen on the stripped text — the markers are not
 * drawn and must not count toward the width — but the runs then have to
 * be rebuilt per line, which is what this returns.
 */
private List<List<TextRun>> wrapRuns(
        String marked,
        FontMetrics metrics,
        int maxWidth
) {

    List<TextRun> all = parseRuns(marked);

    List<List<TextRun>> lines = new ArrayList<>();

    List<TextRun> current = new ArrayList<>();

    int width = 0;

    for (TextRun run : all) {

        for (String word : run.text().split("(?<= )")) {

            if (word.isEmpty()) {
                continue;
            }

            int wordWidth = metrics.stringWidth(word);

            if (width + wordWidth > maxWidth && !current.isEmpty()) {
                lines.add(current);
                current = new ArrayList<>();
                width = 0;

                // A line never starts with the space that ended the last.
                word = word.stripLeading();
                wordWidth = metrics.stringWidth(word);
            }

            current.add(new TextRun(word, run.accent()));
            width += wordWidth;
        }
    }

    if (!current.isEmpty()) {
        lines.add(current);
    }

    return lines;
}

private Font fitSingleLineFont(
        String text,
        Font baseFont,
        int preferredSize,
        int minimumSize,
        int maxWidth
) {

    for (int size = preferredSize; size >= minimumSize; size--) {

        Font candidate = baseFont.deriveFont(
                Font.PLAIN,
                (float) size
        );

        FontMetrics metrics = new BufferedImage(
                1,
                1,
                BufferedImage.TYPE_INT_ARGB
        ).createGraphics().getFontMetrics(candidate);

        if (metrics.stringWidth(text) <= maxWidth) {
            return candidate;
        }
    }

    throw new IllegalArgumentException(
            "Header title is too long to fit on one line."
    );
}

    private BufferedImage loadImage(String resourcePath) {
        try (InputStream inputStream =
                     getClass().getResourceAsStream(resourcePath)) {
    
            if (inputStream == null) {
                throw new IllegalStateException(
                        "Image resource not found: " + resourcePath
                );
            }
    
            BufferedImage image = ImageIO.read(inputStream);
    
            if (image == null) {
                throw new IllegalStateException(
                        "Unable to decode image: " + resourcePath
                );
            }
    
            return image;
    
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load image: " + resourcePath,
                    e
            );
        }
    }

    /**
     * Reads the block icon set once at startup.
     *
     * A missing file is logged rather than thrown: an icon is decoration on a
     * block that already carries its label and value, and failing to start the
     * application over one absent PNG would be out of proportion.
     */
    private Map<String, BufferedImage> loadBlockIcons() {

        Map<String, BufferedImage> icons = new LinkedHashMap<>();

        for (CardSpec.BlockIcon icon : CardSpec.BlockIcon.values()) {

            String resourcePath =
                    "/branding/icons/"
                            + icon.name().toLowerCase()
                            + ".png";

            try (InputStream input =
                         getClass().getResourceAsStream(resourcePath)) {

                if (input == null) {
                    continue;
                }

                BufferedImage image = ImageIO.read(input);

                if (image != null) {
                    icons.put(icon.name(), image);
                }

            } catch (IOException e) {
                // Left out of the map; the block renders without it.
            }
        }

        return icons;
    }
}