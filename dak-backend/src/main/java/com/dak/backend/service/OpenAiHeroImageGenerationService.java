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
    return generate(
            visual,
            layoutType,
            CardSpec.CardTone.STANDARD
    );
}

@Override
public HeroImageResult generate(
        CardSpec.VisualSpec visual,
        CardSpec.LayoutType layoutType,
        CardSpec.CardTone tone
) {

    if (!enabled || apiKey == null || apiKey.isBlank()) {
        throw new IllegalStateException(
                "OpenAI image generation is disabled or API key is missing."
        );
    }

    CardSpec.CardTone effectiveTone =
    tone == null
            ? CardSpec.CardTone.STANDARD
            : tone;

try {

String prompt =
        usesPhotographicStyle(
                visual,
                layoutType,
                effectiveTone
        )
                ? buildPhotographicPrompt(
                        visual,
                        effectiveTone
                )
                : effectiveTone == CardSpec.CardTone.STANDARD
                        ? buildStandardPrompt(
                                visual
                        )
                        : buildPrompt(
                                visual,
                                effectiveTone
                        );

String base64 = callApi(
        prompt,
        effectiveTone
);

String dataUrl =
        "data:image/png;base64," + base64;

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
        CardSpec.LayoutType layoutType,
        CardSpec.CardTone tone
) {

    if (tone == CardSpec.CardTone.SERIOUS
            || tone == CardSpec.CardTone.SENSITIVE) {
        return true;
    }

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
private String buildPhotographicPrompt(
        CardSpec.VisualSpec visual,
        CardSpec.CardTone tone
) {

        String subject =
        visual.subject() == null
                ? "A single conceptual object representing the topic"
                : visual.subject();

String background =
        visual.background() == null
                || visual.background().isBlank()
                ? "No additional article-specific environmental elements."
                : visual.background();

String toneInstruction =
        tone == CardSpec.CardTone.SENSITIVE
                ? """
                  Quiet, respectful and highly restrained.
                  Avoid spectacle, shock, visible suffering or dramatic
                  reconstruction. Prefer symbolic contextual objects where
                  literal depiction would be insensitive.
                  """
                : """
                  Serious, factual and restrained.
                  Give the subject appropriate visual weight without
                  sensationalising the event.
                  """;

                  return """
                        Create a conceptual photographic composition for a news information card.
                
                        SUBJECT:
                        %s
                
                        ARTICLE-SPECIFIC SCENE:
                        %s
                
                        TONE:
                        %s
                
                        Use the ARTICLE-SPECIFIC SCENE to choose supporting objects and
                        environmental cues that make the image specific to this report.
                
                        Create a complete environmental scene rather than an isolated object.

                        The artwork will become the full background of a vertical DAK news card.
                        The environment must therefore extend naturally to every edge of the image.
                        Do not isolate the subject and do not create a transparent cut-out.

                        This is an editorial reconstruction, not documentary photography.
                        Do not imitate a specific real photograph or imply that this is an image
                        captured at the actual event.

                        Use the ARTICLE-SPECIFIC SCENE to construct a believable but clearly
                        generic environment appropriate to the report.

                        COMPOSITION FOR CARD OVERLAY:
                        - vertical editorial scene
                        - environment fills the entire frame
                        - no isolated object floating in empty space
                        - keep important visual activity mainly around the middle of the frame
                        - preserve darker, lower-detail space near the top for headline typography
                        - preserve lower-detail space toward the bottom for information panels
                        - allow contextual elements to continue naturally behind those areas
                        - compose as one coherent scene, not a collection of cut-out objects

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
Editorial conceptual photography.
Realistic environmental lighting and believable physical space.
Muted, restrained colour palette.
Natural depth throughout the scene.
Atmospheric but factual.
The image must feel suitable as the background of a serious news card.
No spectacle, sensationalism or cinematic disaster-poster treatment.

BACKGROUND:
Create a complete opaque environmental scene.
The environment must continue to every edge of the image.
No transparency.
No isolated cut-out subject.
No plain studio backdrop.
No empty white or transparent canvas.

COMPOSITION:
Vertical portrait composition designed as a full-card background.

Use three loose visual zones:

TOP:
Keep this area darker and visually quieter.
Do not place the main subject here.
This area will carry the news headline.

MIDDLE:
Place the principal article-specific scene here.
Show enough surrounding environment that the reader understands
the context rather than seeing one isolated object.

BOTTOM:
Continue the same environment naturally, but reduce visual detail
and contrast.
This area will carry information panels and an action notice.

Do not add artificial boxes, panels or text areas to the image itself.
The card renderer adds those later.

            AUSTRALIAN CONTEXT:

            This artwork is for an Australian news card.

            - Keep government, legal, regulatory, financial and public-service
              imagery Australian or visually neutral.
            - Never depict United States flags, U.S. government seals,
              American government emblems, the White House, the U.S. Capitol,
              recognisably American courthouses or other U.S. civic symbolism.
            - Never substitute another country's government, legal or civic
              imagery for an Australian context.
            - Do not invent Australian government seals, agency logos,
              official emblems or institutional branding.
            - Do not add an Australian flag merely to communicate that the
              story is Australian.
            - If the specific Australian institution or setting cannot be
              represented reliably, use neutral contextual imagery instead:
              legal scales, documents, an office, a generic courtroom,
              financial records, public infrastructure or other
              non-country-specific objects appropriate to the article.
            - Accuracy is more important than adding recognisable national
              symbolism.

            STRICTLY DO NOT INCLUDE:
        - identifiable real people
        - recognisable faces
        - graphic injury
        - bodies
        - blood or gore
        - visible suffering
        - identifiable private individuals
        - exact reconstruction of a real news photograph
        - anything presented as documentary evidence of the actual event
        - readable text or typography of any kind
        - numbers or monetary values
        - logos, trademarks or branding

                TEXT-FREE VISUAL DIRECTION:

        Build the scene entirely from physical, non-textual visual symbolism.

        Choose objects whose meaning is clear without written language,
        such as legal scales, a gavel, coins, unmarked payment cards,
        locks, shields, neutral architecture, furniture and other
        relevant physical objects.

        All surfaces must be plain and unmarked.
        Any papers must be blank.
        Any screens must be blank.
        Any cards must be unmarked.

        Written language is not part of this artwork.

        Communicate the article through objects, composition, lighting
        and environment only.

        The SUBJECT and ARTICLE-SPECIFIC SCENE are semantic context for
        choosing the visual concept; they are not content to reproduce
        visually as writing.

        All reader-facing information is added separately by the
        DAK card renderer.

        - United States flags
        - U.S. government seals or emblems
        - the White House
        - the U.S. Capitol
        - recognisably American government buildings
        - recognisably American courtroom symbolism
        - foreign national symbols that are not explicitly relevant to the article
        - invented Australian government logos, seals or emblems
        - unnecessary Australian landmarks
        - dramatic lens flare
        - exaggerated explosions
        - disaster-movie spectacle
        - sensational or distressing imagery

        Generic contextual elements such as roads, warehouses, emergency vehicles,
        smoke, warning barriers, buildings, vegetation and public infrastructure
        are allowed when they are relevant to the ARTICLE-SPECIFIC SCENE.

            Do not render any typography.
            """.formatted(
        subject,
        background,
        toneInstruction
);
}
private String buildStandardPrompt(
        CardSpec.VisualSpec visual
) {

    String subject =
            visual.subject() == null
                    ? "A calm editorial illustration representing the news topic"
                    : visual.subject();

    String background =
            visual.background() == null
                    || visual.background().isBlank()
                    ? "A subtle warm editorial environment related to the subject."
                    : visual.background();

    String mood =
            visual.mood() == null
                    ? "Calm, trustworthy and informative"
                    : visual.mood();

    return """
            Create a wide editorial hero background for a DAK STANDARD news card.

            SUBJECT:
            %s

            ARTICLE-SPECIFIC SCENE:
            %s

            MOOD:
            %s

            PURPOSE:
            This illustration will fill the complete upper section of a
            vertical news card.

            It is NOT a separate picture, sticker, framed illustration,
            icon or cut-out object.

            The title will be rendered by the card system over the LEFT
            side of this artwork.

            COMPOSITION:

            - Use a wide landscape editorial composition.
            - The environment must extend naturally across the full width.
            - Reserve approximately the LEFT 45%% of the image as calm
              negative space for large headline typography.
            - Keep the left side low-detail, low-contrast and visually quiet.
            - Place the principal article-specific subject mainly in the
              RIGHT 55%% of the composition.
            - The main subject may extend slightly toward the centre,
              but must not dominate or obstruct the left headline area.
            - Do not place the largest object in the exact centre.
            - Supporting environmental elements may continue faintly
              behind the left side.
            - The composition should feel like one complete editorial scene,
              not several unrelated objects arranged together.

            VISUAL HIERARCHY:

            LEFT:
            Quiet warm background with generous negative space.
            Only subtle contextual details.

            RIGHT:
            Main article-specific subject.
            This is where the strongest visual detail belongs.

            BACKGROUND:
            Use a warm cream / soft neutral editorial environment that can
            blend naturally into the STANDARD card background.

            Do not create a hard-edged image rectangle inside the scene.
            Do not create a white sticker or isolated transparent cut-out.
            Do not create a visible frame, border, panel or picture box.

            STYLE:
            DAK Hand-Painted Editorial v1.
            Hand-painted editorial illustration.
            Slightly imperfect brush strokes.
            Clear but restrained outlines.
            Warm cream, amber, muted blue and neutral tones.
            Professional and trustworthy rather than playful.
            Detailed enough to communicate the article quickly,
            but visually calmer than LIGHT.

            IMPORTANT:
            Show the subject described above rather than replacing it with
            a generic financial dashboard, generic office desk or giant
            symbolic icon unless that object is explicitly central to the
            supplied subject.

            For institutional or policy stories, prefer a recognisable
            environmental representation of the institution or process
            described by the SUBJECT and ARTICLE-SPECIFIC SCENE.

            Do not include the DAK chicken mascot.

            STRICTLY DO NOT INCLUDE:
            - text
            - letters
            - numbers
            - captions
            - logos
            - trademarks
            - DAK wordmark
            - watermark
            - typography
            - framed artwork
            - square picture compositions
            - isolated sticker-like illustrations
            - a giant object occupying the centre of the image
            - generic corporate stock-art composition
            - photorealism
            - photography
            - 3D rendering
            - unnecessary Australian landmarks
            - Sydney Opera House
            - Sydney Harbour Bridge

            Do not render any typography.
            """.formatted(
            subject,
            background,
            mood
    );
}


private String buildPrompt(
        CardSpec.VisualSpec visual,
        CardSpec.CardTone tone
) {

        String subject =
                visual.subject() == null
                        ? "A simple editorial illustration about the topic"
                        : visual.subject();

                        String mood =
                        visual.mood() == null
                                ? "Informative and calm"
                                : visual.mood();
                
                String background =
                        visual.background() == null
                                || visual.background().isBlank()
                                ? "No additional article-specific environmental elements."
                                : visual.background();
                
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
                        
                                ARTICLE-SPECIFIC SCENE:
                                %s
                        
                                MOOD:
                                %s
                        
                                The ARTICLE-SPECIFIC SCENE describes the environment and contextual
                                elements that make this artwork specific to this story.
                        
                                Incorporate useful elements from that scene around the main subject,
                                but keep the complete artwork as one isolated transparent composition.
                        
                                Do not create a rectangular background, backdrop or full-card scene.
                                The final card renderer supplies the card background separately.
                        
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
        background,
        mood,
        mascotInstruction
);
    }

    private String callApi(
        String prompt,
        CardSpec.CardTone tone
) throws Exception {

    ObjectNode payload = objectMapper.createObjectNode();

    payload.put("model", model);
    payload.put("prompt", prompt);

    boolean portraitFullScene =
    tone == CardSpec.CardTone.SERIOUS
            || tone == CardSpec.CardTone.SENSITIVE;

boolean standardLandscapeScene =
    tone == CardSpec.CardTone.STANDARD;

/*
* LIGHT remains a transparent cut-out hero.
*
* STANDARD is a wide opaque editorial environment used across
* the complete upper section of the card.
*
* SERIOUS / SENSITIVE remain portrait full-card environments.
*/
if (portraitFullScene) {

payload.put("size", "1024x1536");
payload.put("background", "opaque");

} else if (standardLandscapeScene) {

payload.put("size", "1536x1024");
payload.put("background", "opaque");

} else {

payload.put("size", "1536x1024");
payload.put("background", "transparent");
}

    // Medium is sufficient while testing visual consistency.
    payload.put("quality", "medium");

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