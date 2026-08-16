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

    // Raised from 1200. A carousel spec with three cards of blocks, each with
    // a label, value, note and icon, runs past that limit and the response is
    // cut off mid-JSON — which surfaces as a parse failure and falls back to a
    // spec carrying the title three times and no visual concept at all. The
    // failure is silent from the outside: a card renders, it just says nothing.
    private static final int MAX_TOKENS = 4000;

    private static final int MAX_INPUT_CHARS = 12_000;

    // The house style for ordinary updates.
    private static final String CARD_STYLE_ILLUSTRATION =
            "DAK_HAND_PAINTED_EDITORIAL_V1";

    // Used where the subject matter is grave. Chosen separately from the
    // layout: a layout answers whether a figure is the point of the card,
    // and a story can be serious without carrying a figure at all.
    private static final String CARD_STYLE_PHOTOGRAPHIC =
            "DAK_CONCEPTUAL_PHOTO_V1";

    private static final String SYSTEM_PROMPT = """
            You will receive source article content used by DAK to prepare an
            Australia Update.

            Your job is NOT to summarize or rewrite the full article.
            Your job is to identify the most useful verified facts from the supplied
            source article and create a specification for ONE social-media card.

            The card must be understandable quickly on a mobile screen.

            STRICT FACTUAL RULES:

            - Treat the supplied source article as the primary source of truth.
            - Use only facts explicitly contained in the supplied source article.
            - Never add outside knowledge.
            - Never guess a date, number, amount, deadline, organisation or location.
            - Never make a claim stronger than the source article supports.
            - If a useful key fact is not explicitly present, return keyFact as null.
            - Write all reader-facing card text in natural Korean.

            SINGLE CARD OR CAROUSEL:

            For AU_UPDATE content, default to ONE self-contained card.

            The reader should be able to understand the most useful parts of
            the update without swiping to another card.

            For AU_UPDATE:
            - Prefer SINGLE even when the source article contains many facts.
            - Select the three to five facts that are most useful to a DAK reader
              and compress them into the first card.
            - Prefer concrete facts such as dates, numbers, amounts, deadlines,
              eligibility conditions, visa subclasses and minimum scores.
            - When the supplied content contains information specifically about
              South Australia, strongly prefer including that information when
              it is relevant to DAK readers.
            - Do not create extra cards merely because the source contains
              additional explanation.
            - Use carouselCards as null.

            Preserve the source's unit and meaning exactly.
            In particular, do not convert a count of applications, EOIs, cases,
            households, businesses or other records into a count of people.

            Example:
            "10,000 EOIs invited" must be expressed as "EOI 10,000건 초청",
            not "10,000명 초청".

            For GUIDE content, a carousel may still be used where the content
            genuinely forms a useful sequence.

            When you choose a carousel for GUIDE content, supply carouselCards:
            one to three of them, following the first card rather than repeating it.

            Each carousel card has a role:

            DETAIL   - what happened, or what the thing is.
            ACTION   - what the reader should do or check, only where supported.
            SOURCE   - where to consult for the full picture.

            Do not use the same role twice.

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
            - Use when the source contains multiple DISTINCT practical facts
              that are useful together on one card.
            - For AU_UPDATE, prefer INFOGRAPHIC when three or more different
              useful facts are available.
            - Useful facts include dates, deadlines, amounts, counts, eligibility
              conditions, minimum scores, visa subclasses, program types,
              locations and South Australia-specific figures.
            - Requires infoBlocks: three to five of them, each carrying a
              different fact.
            - Never choose INFOGRAPHIC if the blocks would merely repeat the
              same number or event in different wording.
            - Use keyFact as null when INFOGRAPHIC is chosen; the blocks carry
              the concrete facts instead.
            - When relevant South Australia-specific information exists in the
              supplied source, strongly prefer including at least one such fact.

            FACT_HOOK
            - Use only when ONE verified figure overwhelmingly carries the story.
            - Do not choose FACT_HOOK merely because one number looks visually
              impressive.
            - Requires a keyFact with a concrete value.
            - If the source contains three or more DISTINCT useful facts that
              materially help the reader understand the update, prefer
              INFOGRAPHIC instead.

            DISTINCT means each fact adds new information.

            These are NOT distinct:
            - headline: "EOI 10,000건 초청"
            - keyFact: "10,000 EOIs"
            - infoBlock: "초청 규모 10,000건"

            They are the same fact repeated three times.

            A better selection would be:
            - EOI 10,000건 초청
            - 직업별 최소 점수 65~100점
            - 동점 기준일 2026년 4월 24일
            - SA 190 추천 1,350건
            - SA 491 추천 900건

            A figure being present does not automatically make FACT_HOOK the
            correct layout.

            Before choosing FACT_HOOK, test the figure:

            1. Is it genuinely the main new fact of the story?
            2. Would the reader lose the central meaning of the update without it?
            3. Are there fewer than three other distinct practical facts that
               deserve similar prominence?

            If the answer to any of these is no, prefer STANDARD or INFOGRAPHIC.

            URGENT
            - Use ONLY for a specific event in which people were killed or
              seriously injured, a disaster, or an emergency warning that a
              reader must act on now.
            - It is not the layout for a serious subject. Wage theft, unsafe
              working conditions, exploitation, discrimination and reports of
              systemic harm are serious, and they are STANDARD or INFOGRAPHIC.
              A report about a condition that has persisted for years is not
              an emergency, however grave it is.
            - Ask whether something happened on a particular day that a
              reader needs to know about today. If not, this is not URGENT.
            - Requires a keyFact with a concrete value, and that value must
              be the figure the event newly established — a toll, a count of
              those affected, an area evacuated. A background statistic such
              as an hourly wage or a working week does not qualify, and if
              that is the only figure available the layout is not URGENT.
            - Present the fact plainly. Never treat human harm as spectacle.

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
            - headline must add context that the title does not already say.
            - Keep the headline concise and natural in Korean.
            - Do not use the headline as another fact box.
            - Do not repeat a number, date, amount, score, deadline or other
              concrete value that is already shown in keyFact or infoBlocks.
            - When INFOGRAPHIC is used, the headline should explain what the
              collection of facts means or what event they describe.
            - Prefer a short contextual sentence over repeating the most
              impressive number.
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
            - A card has either a body or blocks, never both. Decide which
              before writing either.
            - Use blocks whenever the card is a list of things rather than
              an explanation of one thing. Phone numbers, addresses, dates,
              amounts, websites, opening hours, eligibility conditions,
              steps. If you would separate the items with commas or full
              stops and the reader would need to find one of them again,
              they are blocks.
            - A card of contact details is always blocks. Written as prose it
              becomes a paragraph a reader has to search through for the
              number they want, which is the opposite of what a card is for.
            - Use body only where the card explains one thing that does not
              come apart into items — why a rule exists, what a term means,
              what a reader should expect.
            - body is two to four short Korean sentences. This is the only
              place on a card where continuous prose belongs, and it still has
              to be readable on a phone at arm's length.
            - blocks follows the infoBlocks rules below.
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
            - On an INFOGRAPHIC, choose and write infoBlocks FIRST.

            - After the blocks are complete, treat every concrete value used
              in them as RESERVED. A reserved value must not appear again in
              the headline.

            - Reserved values include dates, years, counts, amounts,
              percentages, scores, periods, deadlines and visa nomination
              figures.

            - The headline must provide context, not duplicate data.

            - For a news update, the headline should briefly explain what
              happened or what the group of facts represents.

            - For a procedure or rule change, the headline should explain who
              is affected or what changes for them.

            - If removing all numbers from the proposed headline makes the
              sentence meaningless, rewrite the headline rather than copying
              values from the blocks.

            Example:

            Blocks:
            - EOI 초청 | 10,000건
            - 최소 점수 | 65~100점
            - 동점 기준일 | 2026년 4월 24일
            - SA 190 추천 | 1,350건

            Good headline:
            "이번 초청 라운드의 주요 결과입니다."

            If a useful contextual headline cannot be written without repeating
            concrete data already shown in the blocks, return headline as null.

            Bad headline:
            "189 비자 EOI 10,000건 초청, 최소 점수 65~100점 적용"

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

            TONE:

            Tone and information layout are separate decisions.

            Tone controls the visual world of the card:
            background atmosphere, palette, illustration treatment, mascot use
            and overall emotional restraint.

            The ARTICLE decides what the scene depicts.
            Tone decides how that scene is treated.

            Choose exactly one:

            LIGHT
            - Friendly everyday information, community notices, events,
              lifestyle information and approachable public-service content.
            - Bright, warm and welcoming.
            - The DAK mascot may be used where it naturally fits.
            - Do not make a serious article LIGHT merely because the information
              is useful or easy to understand.

            STANDARD
            - Normal news, migration, policy, economy, government notices,
              education, transport updates and ordinary public information.
            - Calm, clear and editorial.
            - Use a warm contextual background related to the actual article.
            - This is the default when no stronger tone is justified.

            SERIOUS
            - Accidents, violent crime, major public disruption, major safety
              incidents, exploitation, severe misconduct or other stories where
              the subject requires visible weight.
            - Darker and more restrained than STANDARD, but still factual.
            - Use an article-specific scene such as the actual type of road,
              workplace, emergency setting or affected environment.
            - Do not add danger, emergency vehicles, damage or victims unless
              supported by the supplied source.

            SENSITIVE
            - Deaths, victims, grief, memorials, serious injury involving
              identifiable groups, or stories requiring maximum restraint.
            - Quiet, respectful and low-saturation.
            - Never use the DAK mascot.
            - Never turn deaths or victims into spectacle.
            - Prefer symbolic or contextual imagery where a literal depiction
              would be insensitive.

            Examples of the distinction:

            A motorway collision with road closures and emergency response
            may be SERIOUS.

            A school bus crash in which students died is SENSITIVE.

            A skilled-migration invitation round is STANDARD.

            A Census participation reminder may be LIGHT.

            When unsure between LIGHT and STANDARD, choose STANDARD.
            When unsure between SERIOUS and SENSITIVE, choose SENSITIVE only
            when death, victims, grief or comparable human harm is central.

            HERO VISUAL RULES:

            The visual must be based on the supplied article, not on a generic
            Australian-news background.

            visual.subject describes the main focal concept.

            visual.background describes the article-specific environment or scene
            surrounding that subject.

            The background must change with the story.

            Examples:
            - motorway collision:
              a northern Adelaide motorway at night with a restrained emergency
              response scene, only where the source supports it
            - school bus deaths:
              a quiet symbolic roadside memorial atmosphere appropriate to the
              reported loss
            - migration invitation round:
              a skilled-migration application setting with EOI documents and
              administrative process imagery
            - Census:
              an approachable household participation scene

            Do NOT select a background only because of the tone.
            Two SERIOUS stories about different events should normally have
            different backgrounds.

            The visual fields must:
            - be written in English
            - contain one clear focal concept
            - communicate the article before the reader studies the text
            - contain no generated text, letters or numbers
            - contain no logos or trademarks
            - contain no watermark
            - contain no generated DAK wordmark
            - contain no unnecessary Australian landmarks
            - never add Sydney landmarks merely because the story is Australian
            - avoid generic corporate stock-art composition

            LIGHT and STANDARD should normally retain DAK's hand-painted
            editorial character with visible paper/canvas texture and slightly
            imperfect brush strokes.

            SERIOUS should use a darker and more restrained editorial treatment.

            SENSITIVE should use a quiet, low-saturation and respectful treatment.
            Literal depictions of death, injury or suffering are not required.

            mascot:
            - may be used for LIGHT
            - may occasionally be used for STANDARD if it genuinely fits
            - normally null for SERIOUS
            - always null for SENSITIVE

            Reply with JSON only.

            Use exactly this shape:

                        {
              "layoutType": "STANDARD | INFOGRAPHIC | FACT_HOOK | URGENT",
              "tone": "LIGHT | STANDARD | SERIOUS | SENSITIVE",

              "headerTitle": "short Korean topic label that is understandable by itself",
              "title": "short Korean card title",
              "headline": "short contextual Korean sentence, or null",

              "primaryFact": {
  "role": "PRIMARY",
  "emphasis": "NORMAL | CRITICAL",
  "label": "short Korean label",
  "value": "exact value or fact from the source",
  "note": "short Korean context, or null",
  "icon": "CALENDAR | CLOCK | PEOPLE | LOCATION | MONEY | DOCUMENT | LIGHTBULB | CHECK | WARNING | INFO, or null"
},

              "supportingFacts": [
                {
                  "role": "DATE | CONDITION | LOCATION | RANGE | COUNT | AMOUNT | CONTEXT | OTHER",
                  "emphasis": "NORMAL",
                  "label": "short Korean label",
                  "value": "exact fact from the source",
                  "note": "short Korean context, or null",
                  "icon": "CALENDAR | CLOCK | PEOPLE | LOCATION | MONEY | DOCUMENT | LIGHTBULB | CHECK | WARNING | INFO, or null"
                }
              ],

              "callout": {
                "label": "short Korean label",
                "value": "exact fact from the source",
                "note": "why this deserves separate emphasis, or null"
              },

              "action": {
                "title": "short Korean action heading",
                "body": "what the reader should do, explicitly supported by the source",
                "icon": "CHECK | WARNING | DOCUMENT | INFO, or null"
              },

              "keyFact": {
                "label": "legacy key fact label",
                "value": "legacy key fact value"
              },

              "infoBlocks": [
                {
                  "label": "legacy block label",
                  "value": "legacy block value",
                  "note": "legacy block note, or null",
                  "icon": "CALENDAR | CLOCK | PEOPLE | LOCATION | MONEY | DOCUMENT | LIGHTBULB | CHECK | WARNING | INFO, or null"
                }
              ],

              "carouselCards": null,

                            "visual": {
                "subject": "English description of the main focal visual",
                "background": "English description of the article-specific background scene",
                "mood": "English description of the intended treatment",
                "mascot": "English description of a small DAK chicken action, or null"
              }
            }

            The hierarchy fields are editorial roles, not pixel positions.

INFORMATION PRIORITY:

Choose facts according to what the reader most needs to understand,
not according to which number looks largest or most dramatic.

For every fact, ask in this order:

1. Does this tell the reader about immediate human impact, safety or risk?
2. Does this tell the reader what they need to do?
3. Does this establish the central new development of the article?
4. Does this directly concern Adelaide or South Australia?
5. Does this provide useful time, location, eligibility or scope?
6. Is it merely background scale or descriptive context?

A large number does not automatically deserve visual prominence.

Examples:
- In a chemical fire, the amount of material stored at the site is normally
  background scale if the article's practical significance is toxic smoke,
  people taken to hospital, affected suburbs or health precautions.
- In a collision, the number of vehicles involved is normally less important
  than deaths, serious injuries, road closure or an active public warning.
- In a migration invitation round, the number of invitations may genuinely
  be primary because the invitation result itself is the subject of the update.
- In a fee increase, the new fee or amount may genuinely be primary because
  the amount is the change being reported.

Never promote a fact merely because it contains the largest number.

primaryFact:
- Use only when ONE fact genuinely carries the central meaning of the story.
- It must be the fact a reader should understand first after reading the title.
- Prefer the article's consequence or central new development over background
  scale.
- primaryFact must contain ONE fact only.
- Never combine two independent facts into one primaryFact merely because
  both are important.
- If two facts deserve similar prominence, choose the more immediately useful
  one as primaryFact and move the other to supportingFacts.
- For an active public-safety incident, an ongoing hazard or warning normally
  outranks an already-completed consequence when the warning still affects
  the reader.
- For SERIOUS and SENSITIVE stories, human impact or an active safety
  consequence outranks property size, material quantity, financial value or
  other scene-setting statistics.
- Do not use a quantity describing what existed before the event merely
  because the number is large.
- If no single fact deserves substantially more emphasis than the others,
  use null.
- primaryFact must not duplicate the title.

primaryFact emphasis:

- Use CRITICAL only when the fact itself represents exceptional immediate
  human impact or an urgent consequence that defines the severity of the event.

- CRITICAL is rare.

- Use CRITICAL only when the source explicitly establishes one of these:
  death or deaths;
  life-threatening or critical injury;
  a major evacuation order;
  an immediate threat to life affecting the public;
  a catastrophic consequence that clearly defines the event.

- Hospitalisation alone is NOT CRITICAL.

- "Taken to hospital", "hospitalised", "treated in hospital" or similar wording
  must be NORMAL unless the source explicitly says the injury or condition is
  critical, life-threatening, severe, or otherwise equivalent.

- Do not infer severity from hospital transport.

- Use NORMAL for:
  people taken to hospital without an explicit severe or life-threatening
  condition;
  minor or unspecified injuries;
  number of vehicles involved;
  material quantity;
  property damage;
  financial value;
  ordinary closures;
  incident duration;
  number of firefighters or responders.

- When uncertain between NORMAL and CRITICAL, always choose NORMAL.

- Never use CRITICAL merely because a value is numeric or visually striking.

- Never use CRITICAL to make a SERIOUS story look more dramatic.

- When uncertain, use NORMAL.

supportingFacts:
- Use two to five distinct facts that help the reader understand:
  what happened, when, where, who was affected, the current situation,
  or why it matters.
- Rank facts by reader usefulness, not numerical size.
- For SERIOUS public-safety stories, choose supporting facts by practical
  importance to the reader.

- Prefer, in this order when the source provides them:
  1. HUMAN IMPACT — deaths, injuries, hospitalisation or people affected.
  2. CURRENT HAZARD — smoke, toxic material, flooding, fire, closure or
     another hazard that may still affect people.
  3. AFFECTED AREA — where the public may be affected.
  4. CURRENT STATUS — contained, extinguished, closed, reopened, ongoing.
  5. TIME — only when knowing the exact time materially helps the reader
     understand or respond to the incident.

- Do not use DATE or TIME merely because the source contains an exact time.
- For an ongoing public-safety incident, human impact, current hazard and
  affected area are normally more useful than the incident start time.
- This priority controls selection only. Never invent a fact to fill a
  preferred category.
- Do not repeat primaryFact.
- Do not repeat the same underlying fact in different wording.

CARD TEXT DENSITY:
- These facts will appear inside small visual panels, not in an article.
- Write for a mobile card that must be understood at a glance.

- label:
  Prefer 2-6 Korean characters.
  Use a category such as "현황", "영향 지역", "피해", "발생 시간",
  "건강 위험" or "현재 상황".

- value:
  Prefer one compact fact of roughly 3-12 Korean characters.
  It should normally fit on one or two short lines.
  Do not write a complete explanatory sentence as value.

- note:
  Optional.
  Omit it when the value is already understandable by itself.
  When needed, use only one short Korean phrase, preferably 2-10 characters.
  Never use a complete sentence.
  Never add secondary background information merely to fill the note field.

  value and note must have different jobs:
  - value = the main fact.
  - note = only essential context that changes or clarifies the meaning.
  - Never repeat the same noun, action or outcome from value in note.
  - If removing note does not reduce understanding, use null.

  Prefer:
  value: "2명 병원 이송"
  note: "연기 흡입"

  Not:
  value: "2명 병원 이송"
  note: "연기 흡입으로 이송"

  Prefer:
  value: "포트 리버 일대"
  note: "주민 경고 지역"

  Not:
  value: "포트 애들레이드 포트 리버 일대"
  note: "경고 구역 내 주민 대상"

- Do not list every location, symptom, date or condition available in the
  source merely because it is accurate.
- Select the minimum information necessary to represent that fact.
- Where several locations are affected, prefer the most useful geographic
  summary when the source supports one.
- Where several symptoms are listed, summarise the hazard rather than copying
  the entire symptom list unless individual symptoms are essential.

callout:
- Use only when one separate fact deserves additional emphasis but does not
  carry the entire story.
- Good uses include a South Australia-specific fact, an important consequence,
  a warning, an unusual condition or a meaningful comparison.
- Do not use callout merely to rescue a large background number that was
  rejected as primaryFact.
- Otherwise use null.

action:
- Use when the supplied article explicitly tells the reader to do, check,
  apply, avoid, contact, prepare or monitor something.
- Never invent advice.

- For an active safety or public-health story, choose the action the reader
  should take BEFORE symptoms, injury or further harm occurs.

- Preventive action outranks symptom monitoring.
- Symptom monitoring outranks emergency escalation instructions only when
  no preventive action is supplied by the source.

- Example:
  If residents are told to close doors and windows when smoke or odour is
  present, that is the primary action.
  Instructions about seeking medical help for severe symptoms are secondary
  and should not replace that preventive action.

- In an active safety or public-health story, supported protective action is
  more useful than background statistics and must not be omitted merely to
  make room for another number.

- action should normally contain ONE clear instruction.
- Do not combine several different instructions into a paragraph.

- title:
  Prefer 2-8 Korean characters.
  Describe the purpose, such as "주민 보호 조치", "지금 확인", or
  "신청 방법".

- body:
  Write exactly ONE direct instruction containing ONE action only.
  Prefer roughly 15-30 Korean characters.
  Select the single most useful immediate protective action.

  ONE action means one thing the reader should do.
  Do not join two actions using connectors such as
  "하고", "하며", "그리고", "~고", "또는", or commas.

  For general public-safety incidents, prefer an immediate preventive action
  over secondary medical advice when both are available.

  Do not append:
  - a second action,
  - medical-treatment advice,
  - symptom lists,
  - background explanation,
  - affected locations,
  - incident details.

  Prefer:
  "연기나 냄새가 감지되면 문과 창문을 닫으세요."

  Not:
  "연기나 냄새가 감지되면 문과 창문을 닫고,
  심한 증상 시 의료 지원을 받으세요."
- For example, prefer:
  "연기나 냄새가 감지되면 문과 창문을 닫으세요."

  rather than:
  "지역 주민은 비상 업데이트를 계속 확인하고, 냄새나 연기가
  감지되면 창문과 문을 닫으세요."

- Otherwise use null.

SERIOUS / SENSITIVE HIERARCHY CHECK:

Before returning the JSON for a SERIOUS or SENSITIVE story, inspect
primaryFact one final time.

Ask:

"If I removed this fact, would a DAK reader lose the main human or practical
meaning of what happened?"

If no, it is not primaryFact.

Then ask:

"Am I choosing this mainly because the number is visually impressive?"

If yes, it is not primaryFact.

Background quantities such as tonnes of material, property value, building
size, historical totals or other scene-setting scale normally belong in a
supporting fact, callout, or nowhere on the card unless that quantity itself
is the reported development.

            Legacy keyFact and infoBlocks must still be supplied where the
            current layout rules require them. They temporarily coexist with
            the hierarchy fields while the renderer is being migrated.

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
            String sourceContent
    ) {
        return generate(
                "AU_UPDATE",
                title,
                sourceContent
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
                // Logged at error rather than warn. The fallback renders a card
                // that looks finished — title repeated three times over artwork
                // about nothing — so nothing in the admin screen says the
                // generation failed. The log is the only place it shows.
                log.error(
                        "Card generation failed for '{}', falling back to a spec "
                                + "with no visual concept: {}",
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

String tone = normaliseTone(
  nullableText(node.path("tone"))
);

String headline = nullableText(
  node.path("headline")
);

CardSpec.CardFact primaryFact = parseCardFact(
  node.path("primaryFact")
);

List<CardSpec.CardFact> supportingFacts = parseCardFacts(
  node.path("supportingFacts")
);

CardSpec.Callout callout = parseCallout(
  node.path("callout")
);

CardSpec.ActionBlock action = parseActionBlock(
  node.path("action")
);

// Legacy fields remain populated until the renderer is migrated.
CardSpec.KeyFact keyFact = parseKeyFact(
  node.path("keyFact")
);

List<CardSpec.InfoBlock> infoBlocks = parseInfoBlocks(
  node.path("infoBlocks")
);

// INFOGRAPHIC concrete data belongs in its structured facts.
// A numeric headline would duplicate information already displayed.
if ("AU_UPDATE".equals(contentType)
  && "INFOGRAPHIC".equalsIgnoreCase(layoutType)
  && containsConcreteNumber(headline)) {

headline = null;
}

List<CardSpec.CarouselCard> carouselCards =
        "AU_UPDATE".equals(contentType)
                ? null
                : parseCarouselCards(node.path("carouselCards"));

                JsonNode visualNode = node.path("visual");

                String subject = textOrFallback(
                        visualNode.path("subject"),
                        "editorial visual representing " + originalTitle
                );
                
                String background = nullableText(
                        visualNode.path("background")
                );
                
                String mood = nullableText(
                        visualNode.path("mood")
                );
                
                String mascot = nullableText(
                        visualNode.path("mascot")
                );
                
                // Tone controls treatment, while subject/background describe the article.
                if ("SENSITIVE".equals(tone)) {
                    mascot = null;
                }
                
                String style =
                        ("SERIOUS".equals(tone) || "SENSITIVE".equals(tone))
                                ? CARD_STYLE_PHOTOGRAPHIC
                                : CARD_STYLE_ILLUSTRATION;
                
                CardSpec.VisualSpec visual = new CardSpec.VisualSpec(
                        subject,
                        background,
                        mood,
                        mascot,
                        style
                );
                
                return new CardSpec(
                        contentType,
                        carouselCards == null ? "SINGLE" : "CAROUSEL",
                        layoutType,
                        tone,
                        headerTitle,
                        title,
                        headline,
                        primaryFact,
                        supportingFacts,
                        callout,
                        action,
                        keyFact,
                        infoBlocks,
                        carouselCards,
                        visual
                );
    }

    private String normaliseTone(String tone) {

      if (tone == null) {
          return CardSpec.CardTone.STANDARD.name();
      }
  
      try {
          return CardSpec.CardTone.valueOf(
                  tone.trim().toUpperCase()
          ).name();
      } catch (IllegalArgumentException e) {
          return CardSpec.CardTone.STANDARD.name();
      }
  }
  
  private CardSpec.CardFact parseCardFact(JsonNode node) {
  
      if (node == null || node.isNull() || node.isMissingNode()) {
          return null;
      }
  
      String value = nullableText(node.path("value"));
  
      if (value == null) {
          return null;
      }
  
      return new CardSpec.CardFact(
        nullableText(node.path("role")),
        nullableText(node.path("emphasis")),
        nullableText(node.path("label")),
        value,
        nullableText(node.path("note")),
        nullableText(node.path("icon"))
);
  }
  
  private List<CardSpec.CardFact> parseCardFacts(JsonNode node) {
  
      if (node == null || !node.isArray() || node.isEmpty()) {
          return null;
      }
  
      List<CardSpec.CardFact> facts = new ArrayList<>();
  
      for (JsonNode element : node) {
  
          CardSpec.CardFact fact = parseCardFact(element);
  
          if (fact != null) {
              facts.add(fact);
          }
      }
  
      return facts.isEmpty()
              ? null
              : facts.stream().limit(5).toList();
  }
  
  private CardSpec.Callout parseCallout(JsonNode node) {
  
      if (node == null || node.isNull() || node.isMissingNode()) {
          return null;
      }
  
      String value = nullableText(node.path("value"));
  
      if (value == null) {
          return null;
      }
  
      return new CardSpec.Callout(
              nullableText(node.path("label")),
              value,
              nullableText(node.path("note"))
      );
  }
  
  private CardSpec.ActionBlock parseActionBlock(JsonNode node) {
  
      if (node == null || node.isNull() || node.isMissingNode()) {
          return null;
      }
  
      String body = nullableText(node.path("body"));
  
      if (body == null) {
          return null;
      }
  
      return new CardSpec.ActionBlock(
              nullableText(node.path("title")),
              body,
              nullableText(node.path("icon"))
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

    private boolean containsConcreteNumber(String text) {

      if (text == null || text.isBlank()) {
          return false;
      }
  
      return text.matches(".*\\d.*");
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
                                "simple hand-painted editorial illustration representing "
                                        + safeTitle,
                                "informative and neutral",
                                null,
                                CARD_STYLE_ILLUSTRATION
                        )
        );
    }
}