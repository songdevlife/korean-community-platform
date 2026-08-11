package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@ConditionalOnProperty(
        name = "app.image.openai.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OpenAiHeroImageGenerationService
        implements HeroImageGenerationService {

    private static final Logger log =
            LoggerFactory.getLogger(OpenAiHeroImageGenerationService.class);

    private static final String API_URL =
            "https://api.openai.com/v1/images/generations";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    @Value("${app.image.openai.model:gpt-image-1.5}")
    private String model;

    @Value("${app.image.openai.enabled:true}")
    private boolean enabled;

    @Override
    public HeroImageResult generate(
            CardSpec.VisualSpec visual,
            CardSpec.LayoutType layoutType
    ) {

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI image generation is disabled or API key is missing."
            );
        }

        try {
                String prompt = usesPhotographicStyle(visual, layoutType)
                        ? buildPhotographicPrompt(visual)
                        : buildPrompt(visual);
            String base64 = callApi(prompt);

            String dataUrl = "data:image/png;base64," + base64;

            return new HeroImageResult(
                    dataUrl,
                    prompt
            );

        } catch (Exception e) {
            log.warn(
                    "Hero image generation failed: {}",
                    e.getMessage()
            );

            throw new IllegalStateException(
                    "Hero image generation failed.",
                    e
            );
        }
    }
/**
     * Grave stories carry more weight with the restrained photographic
     * treatment. Everyday updates stay in the illustrated house style.
     *
     * The style now follows the tone decided by the card editor rather than
     * the layout. Layout answers whether a figure is the point of the card,
     * which is a different question: a report of wage theft with no new
     * figure in it is STANDARD and grave at the same time, and tying the two
     * together left it in the house illustration style — which produced a
     * cheerful farm worker beside a story about seven dollars an hour.
     *
     * URGENT stays here as a floor. It is only chosen for deaths and
     * disasters, so it is grave whatever the tone field says.
     */
private boolean usesPhotographicStyle(
        CardSpec.VisualSpec visual,
        CardSpec.LayoutType layoutType
) {

    if (layoutType == CardSpec.LayoutType.URGENT) {
        return true;
    }

    return visual != null
            && "DAK_CONCEPTUAL_PHOTO_V1".equals(visual.style());
}

/**
 * Photographic treatment, deliberately restricted to abstract objects.
 *
 * A realistic image of a place, building or person would read as
 * documentary evidence of the actual event, which the illustration style
 * never risks. Conceptual objects stay legible as metaphor.
 */
private String buildPhotographicPrompt(CardSpec.VisualSpec visual) {

    String subject =
            visual.subject() == null
                    ? "A single conceptual object representing the topic"
                    : visual.subject();

                    return """
                        Create a conceptual photograph for a news information card.
            
                        SUBJECT:
                        %s
            
            Reduce the subject to ONE simple physical object or a small
            arrangement of objects. Do not depict the event itself.

            Draw what the SUBJECT above describes, not the difficulty that
            surrounds it. Where the subject is help being offered — a place
            to go, a service that exists, something a reader can use — show
            the thing that is open and available: a full bowl, a door with
            the light on, a stocked shelf, a hand offering rather than a
            hand empty. A worn wallet, a padlock and a crumpled receipt
            describe having nothing, and a guide about where to eat is not
            about having nothing.

            Where the subject is harm or loss itself, the reverse applies:
            do not make it pleasant. A straw hat beside a wooden hut and a
            pile of coins is a rural idyll, and against a report of wage
            theft it contradicts the words.

            MOOD:
            Serious and factual. Restrained. Not cheerful, but not bleak
            either — the register is that of a public notice rather than a
            campaign poster or a photograph of hardship.
            
                        STYLE:
            Studio product photography.
            Single clear subject on a plain seamless background.
            Soft directional light with gentle shadows.
            Shallow depth of field.
            Muted, desaturated palette.
            Restrained and serious. No drama, no spectacle.

            BACKGROUND:
            Fully transparent background.
            The subject must be cut out with nothing behind it.
            No backdrop, no background colour, no gradient, no vignette.
            No frame, no border, no rectangle.
            No cast shadow on the ground.

            COMPOSITION:
            Wide landscape orientation.
            Keep the subject large, centred and tightly cropped.
            The image sits on a white card, so keep tonal contrast strong
            enough to read against white.

            STRICTLY DO NOT INCLUDE:
            - people or any part of a human body
            - faces
            - real buildings, offices, streets or interiors
            - identifiable locations
            - anything that could be mistaken for documentary evidence
              of the actual event
            - news photography or photojournalism framing
            - crowds, crime scenes, emergency vehicles, damage or injury
            - text, letters, numbers, captions
            - logos, trademarks, watermarks
            - brand names or product packaging
            - Australian landmarks
            - dramatic lighting, lens flare, motion blur
            - graphic or distressing imagery

            Do not render any typography.
            """.formatted(
            subject
    );
}
    private String buildPrompt(CardSpec.VisualSpec visual) {

        String subject =
                visual.subject() == null
                        ? "A simple editorial illustration about the topic"
                        : visual.subject();

        String mood =
                visual.mood() == null
                        ? "Informative and calm"
                        : visual.mood();

        String mascotInstruction =
                visual.mascot() == null
                        ? "Do not include the DAK chicken mascot."
                        : """
                          Include a small supporting DAK chicken mascot:
                          %s
                          The mascot must not dominate the composition.
                          """.formatted(visual.mascot());

        return """
                Create a hero illustration for Discover Adelaide Korea.

                SUBJECT:
                %s

                MOOD:
                %s

                STYLE:
                DAK Hand-Painted Editorial v1.

                Hand-painted editorial illustration.
                Slightly imperfect brush strokes.
                Bold, confident dark outlines on every shape.
                Rounded and simplified forms.
                Warm, saturated colours with soft pastel accents.
                Friendly editorial composition, but not childish.
                One clear visual focal point.

                BACKGROUND:
                Fully transparent background.
                The subject must be cut out and isolated with nothing behind it.
                No backdrop, no background colour, no background wash.
                No paper texture, no canvas texture, no grain covering the canvas.
                No frame, no border, no rectangle, no panel, no card edge.
                No drop shadow on the ground and no cast shadow behind the subject.
                Any texture belongs inside the painted shapes themselves,
                never in the empty space around them.

                BRAND COLOUR DIRECTION:
                Use red, blue, cream and near-black as primary accents.
                Yellow, green, soft pink and sky blue may be used sparingly.

                COMPOSITION:
                Create a wide landscape editorial illustration.
                Use the full horizontal canvas.
                Keep the main subject large and visually dominant.
                Place the focal subject near the centre.
                Use supporting visual elements to extend naturally toward the left and right.
                Avoid excessive empty space above or below the subject.
                Keep the artwork tight to the subject; the surrounding area
                will be transparent, so trailing empty space is wasted.
                The illustration is placed on a white card, so keep colours
                strong enough to read clearly against white.

                MASCOT:
                %s

                STRICTLY DO NOT INCLUDE:
                - text
                - letters
                - numbers
                - captions
                - logos
                - trademarks
                - DAK wordmark
                - watermark
                - photorealism
                - photography
                - 3D rendering
                - glossy corporate vector art
                - anime
                - unnecessary Australian landmarks
                - Sydney Opera House
                - Sydney Harbour Bridge
                - background rectangles or coloured panels
                - visible canvas or paper backdrop
                - checkerboard patterns
                - borders or frames of any kind

                Do not render any typography.
                """.formatted(
                subject,
                mood,
                mascotInstruction
        );
    }

    private String callApi(String prompt) throws Exception {

        ObjectNode payload = objectMapper.createObjectNode();

        payload.put("model", model);
        payload.put("prompt", prompt);

        // Landscape hero artwork, not the final 1080x1350 card.
        payload.put("size", "1536x1024");

        // Medium is sufficient while testing visual consistency.
        payload.put("quality", "medium");

        // The card renderer draws the hero directly onto its own panel,
        // so the artwork must arrive with an alpha channel and no backdrop.
        payload.put("background", "transparent");

        // Required for alpha to survive; the API may otherwise return JPEG.
        payload.put("output_format", "png");

        // The generated image is returned as base64 image data.

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .timeout(Duration.ofSeconds(120))
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
                    "OpenAI API returned "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        JsonNode root = objectMapper.readTree(
                response.body()
        );

        JsonNode data = root.path("data");

        if (!data.isArray() || data.isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI image response contained no image data."
            );
        }

        String base64 = data.get(0)
                .path("b64_json")
                .asText(null);

        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI image response contained no base64 image."
            );
        }

        return base64;
    }
}