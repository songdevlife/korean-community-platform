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

            KOREAN NUMBER AND CURRENCY FORMATTING:

            - Write numbers and monetary amounts in natural Korean notation
              when they appear in reader-facing Korean card text.
            - Never mix an English numeric scale word or symbol with a Korean
              scale word in an unnatural form.
            - In particular, never write expressions such as:
              "$248.3 백만", "$1.2 십억", "248.3 million 달러".

            - Convert English million and billion amounts into natural Korean
              number units when practical, while preserving the exact magnitude.

            Examples:
            - "$248.3 million" -> "2억 4,830만 호주달러"
            - "$1.2 billion" -> "12억 호주달러"
            - "$25 million" -> "2,500만 호주달러"
            - "$3.5 million" -> "350만 호주달러"

- When the source clearly establishes that a monetary amount is in
  Australian dollars, ALWAYS write "호주달러" for that amount in
  Korean reader-facing card text.

- Apply this rule consistently to EVERY reader-facing field,
  including:
  title, headline, primaryFact, supportingFacts, secondaryFacts,
  callout, action, keyFact and infoBlocks.

- Within the same card, never mix "달러", "$" and "호주달러" for
  amounts that the source establishes are Australian dollars.

- For example, if all three amounts are Australian dollars:
  "390만 호주달러"
  "3,200만 호주달러"
  "32만 5천 호주달러"

  Never return:
  "390만 호주달러"
  "3,200만 달러"
  "32만 5천 달러"

- This consistency rule also applies when the currency is obvious
  from an earlier sentence or from the article context. Once the
  supplied source establishes that the relevant amounts are AUD,
  preserve "호주달러" for subsequent AUD amounts on the card.

- Do not assume that an unspecified "$" means Australian dollars
  unless the supplied source establishes the currency.

- Do not perform exchange-rate conversion.
- Do not round or alter the source amount merely to make it shorter.
- Preserve qualifiers such as "more than", "at least", "up to",
  "approximately" and "over" when they are present in the source.

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
            - Do NOT use INFOGRAPHIC for AU_UPDATE content.
            - AU_UPDATE cards now use the hierarchy fields rendered by the
              current DAK card renderer instead of legacy infoBlocks.
            - For AU_UPDATE:
              - LIGHT tone must use layoutType STANDARD.
              - STANDARD tone must use layoutType STANDARD.
              - Use supportingFacts for the most useful distinct facts.
              - Use secondaryFacts for additional useful distinct facts.
              - Use primaryFact only when one fact genuinely deserves
                substantially greater prominence.
            - INFOGRAPHIC remains available only for non-AU_UPDATE content
              where a structured block layout is genuinely useful.
            - When INFOGRAPHIC is used for non-AU_UPDATE content:
              - Requires infoBlocks: three to five of them.
              - Every infoBlock must carry a different source-supported fact.
              - Never choose INFOGRAPHIC if the blocks merely repeat the same
                number or event in different wording.
              - Use keyFact as null because the blocks carry the concrete facts.

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

            If the answer to any of these is no, prefer STANDARD.
For AU_UPDATE, never fall back from FACT_HOOK to INFOGRAPHIC.

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
- Write the title in plain, natural Korean that a general Korean reader
  can understand immediately.
- Avoid compressed Sino-Korean newspaper shorthand when a clearer
  everyday expression is available.
- NEVER use "모자" to mean mother and child in a card title, headline,
  primaryFact, facts or action.
  "모자" is ambiguous in modern Korean because it is commonly understood
  as "hat".
  Always write the relationship explicitly, such as "어머니와 자녀",
  "어머니와 두 자녀", or another source-supported plain expression.

- Use age-specific terms such as "영아", "유아" or "아동" only when
  that age distinction is important to understanding the story.

- Before returning the JSON, perform a final Korean-language check:
  1. Replace ambiguous family shorthand such as "모자" with plain Korean.
  2. Remove unnatural translated or compressed news-headline expressions.
  3. Compare the headline against the title, primaryFact, supportingFacts
   and action.

   Treat a fact as duplicated even when it is expressed using different
   Korean wording.

   Do not repeat the same underlying information by adding modifiers
   or changing sentence structure.

   Examples of duplication:
   - title: "3개월 영아 수색 계속"
     headline: "광범위한 수색이 계속되고 있습니다."
   - supportingFact: "광범위한 수색 중"
     headline: "광범위한 수색이 계속되고 있습니다."
   - supportingFact: "경찰·소방 합동 수색"
     headline: "경찰과 소방이 수색을 진행하고 있습니다."

   Remove every duplicated idea from the headline.

   The final headline must contain at least one meaningful,
   source-supported development or context that is not already
   communicated anywhere else on the card.

   If only part of the headline is new, keep only the new information
   and rewrite it as natural Korean.

   If no genuinely new information remains, return headline as null.
  4. Check that facts do not unnecessarily repeat information already
     communicated by the title or headline.
  5. Remove awkward repetition within the same sentence.
   Do not repeat the same Korean noun or phrase unnecessarily.

6. For SERIOUS and SENSITIVE stories, verify that every person's status
   remains accurate.
   Do not merge confirmed deaths, missing persons, suspected deaths or
   people still being searched for into one collective description.
   If the title could imply that a missing person is already confirmed dead,
   rewrite it before returning the JSON.
     For example, avoid expressions such as
     "수색은 광범위한 수색 지역에서 계속되고 있습니다."
     Prefer concise natural Korean such as
     "광범위한 수색이 계속되고 있습니다."
- Otherwise prefer a natural description such as "어린 자녀",
  "아기", "자녀", or a verified age from the source.
- For SERIOUS or SENSITIVE stories, prefer respectful human wording
  over terse crime-headline shorthand.
- Do not compress people into an ambiguous collective term merely
  to make the title shorter.

- When confirmed deaths and an unresolved missing-person search are both
  present, keep those statuses clearly separate in the title.
- Never write a title that can imply a missing person has already been
  confirmed dead.
- Do not group confirmed victims and a still-missing person under one
  shared death phrase.

- Respectful wording must NEVER make a confirmed death ambiguous.
  If the source explicitly states that a person was found dead,
  died, or that a body was recovered, preserve that status explicitly
  in Korean.

- Never shorten a confirmed death to ambiguous wording such as:
  "발견", "찾아", "확인" or "발견됨".

- Prefer clear but restrained wording such as:
  "숨진 채 발견", "사망 확인", or "시신 발견",
  according to what the source explicitly supports.

- Prefer wording such as:
  "어머니와 자녀 숨진 채 발견… 실종 아동 수색 계속"
  rather than:
  "어머니와 자녀 발견… 실종 아동 수색 계속"

- The title must preserve the source's certainty:
  confirmed death, missing, presumed, suspected and under investigation
  are different statuses and must not be blended together.
  
CARD TEXT GENERATION ORDER:

For every card, decide reader-facing text in this order:

1. title
2. primaryFact, if needed
3. supportingFacts
4. secondaryFacts, if useful
5. action, if needed
6. headline LAST

Do not draft the headline before the structured facts are selected.

When writing the headline, treat all information already used in:
- title
- primaryFact
- supportingFacts
- secondaryFacts
- action

as RESERVED.

The headline must not repeat, paraphrase or recombine reserved information.

The headline must contribute genuinely new context that helps the reader
understand the story.

For SERIOUS and SENSITIVE stories, useful headline context may include:
- an ongoing search
- a distinct investigation development
- a public appeal
- a separate current status
- another source-supported development not already shown elsewhere

If no genuinely new useful context remains after the other card fields are
written, return headline as null.

- headline must add NEW context that the title does not already communicate.
- Never paraphrase or restate the title in the headline.
- If the title already says who died, what happened, or that an investigation
  is ongoing, do not repeat those facts in the headline.
- Do not repeat victim counts, ages, dates, locations or investigation status
  when those facts already appear in the title or supportingFacts.
- Prefer current useful context that advances the story, such as:
  an ongoing search, a public appeal, a supported next step, or another
  distinct development explicitly stated in the source.
- If no genuinely new contextual sentence is available, return headline as null.
- Keep the headline concise and natural in Korean.
- Do not use the headline as another fact box.
- Do not repeat a number, date, amount, score, deadline or other
  concrete value that is already shown elsewhere on the card.
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

LIGHT STRUCTURE:
- When tone is LIGHT, layoutType must be STANDARD.
- Never use INFOGRAPHIC, FACT_HOOK or URGENT with LIGHT.
- Use primaryFact as null.
- Select exactly three supportingFacts when the source contains
  at least three useful distinct facts.
- supportingFacts are the three most useful practical facts that
  should be understood first.
- Use zero to two secondaryFacts for additional useful information.

- After selecting the three supportingFacts, inspect the remaining
  source-supported information for additional distinct useful facts.

- If TWO OR MORE distinct useful facts remain, you MUST return
  exactly two secondaryFacts.

- If exactly ONE distinct useful fact remains, return exactly one
  secondaryFact.

- Return an empty secondaryFacts array only when no additional
  useful distinct fact remains.

- Do not combine two distinct useful facts into one secondaryFact
  merely to reduce the number of secondaryFacts.

- For example, programs, booking information, special dates,
  venue information, eligibility, practical tips and additional
  event activities should remain separate when they communicate
  different information.

- Never invent or duplicate information merely to reach two
  secondaryFacts.
- For LIGHT, infoBlocks must be null.
- For LIGHT, keyFact must be null.
- For LIGHT, callout should normally be null.
- For LIGHT, action should normally be null unless the source
  explicitly requires the reader to take an action.

            STANDARD
            - Normal news, migration, policy, economy, government notices,
              education, transport updates and ordinary public information.
            - Calm, clear and editorial.
            - Use a warm contextual background related to the actual article.
            - This is the default when no stronger tone is justified.
            - For AU_UPDATE, when tone is STANDARD, layoutType MUST be STANDARD.
            - Never return INFOGRAPHIC, FACT_HOOK or URGENT for a STANDARD
              AU_UPDATE card.

            STANDARD STRUCTURE:
- When tone is STANDARD and layoutType is STANDARD,
  supportingFacts are REQUIRED whenever the source contains
  useful distinct facts that can be represented accurately.

- Count the useful distinct source-supported facts BEFORE deciding
  whether supportingFacts may be empty.

- If the source contains THREE OR MORE useful distinct facts,
  you MUST return exactly three supportingFacts.

- If the source contains exactly TWO useful distinct facts,
  you MUST return exactly two supportingFacts.

- If the source contains exactly ONE useful distinct fact,
  you MUST return exactly one supportingFact.

- Return an empty supportingFacts array ONLY when the source contains
  no useful distinct fact suitable for a supportingFact.

- A fact is suitable when it provides useful concrete information such as:
  a date, amount, count, affected group, location, program, purpose,
  eligibility condition, current status, practical effect or other
  source-supported information that helps explain the update.

- Do not omit an otherwise useful fact merely because it is already
  broadly related to the title. Only exclude it when it would repeat
  the same underlying information without adding useful detail.

- Never invent, infer or duplicate information merely to satisfy
  the required number of supportingFacts.

- After selecting supportingFacts, inspect the remaining
  source-supported information for additional distinct useful facts.

            - Use secondaryFacts for useful practical or contextual information
              that is less important than supportingFacts but still helps the
              reader understand what happens next, why the update matters,
              what may be affected, or what additional context is useful.

            - If TWO OR MORE useful distinct facts remain after selecting
              supportingFacts, return exactly two secondaryFacts.

            - If exactly ONE useful distinct fact remains, return exactly
              one secondaryFact.

            - Return an empty secondaryFacts array only when no additional
              useful distinct fact remains.

            - Never duplicate title, headline, primaryFact or supportingFacts
              in secondaryFacts.

            - Never invent information merely to create secondaryFacts.

            - Do not move essential information out of supportingFacts merely
              to populate secondaryFacts.

            - For STANDARD economic or policy updates, good secondaryFacts
              may include source-supported outlook, expected effects,
              implementation context, affected products or groups, or another
              practical consequence not already shown above.

            SERIOUS
- Accidents, violent crime, major public disruption, major safety
  incidents, exploitation, severe misconduct, organised scams,
  coordinated fraud, coercive consumer conduct, threatening trade
  practices or other stories where the subject requires visible weight.
  
- Consumer warnings involving organised offenders, repeated deception,
  intimidation, coercion, unlawful trading or substantial risk of harm
  should normally be SERIOUS rather than STANDARD.

- This includes ordinary fatal traffic crashes, workplace deaths,
  industrial accidents and other single-incident fatalities where
  the story is primarily about the incident, safety, investigation,
  road closure, emergency response or public impact.
- A death does NOT automatically make a story SENSITIVE.
- Darker and more restrained than STANDARD, but still factual.
- Use an article-specific scene such as the actual type of road,
  workplace, emergency setting or affected environment.
- Do not add danger, emergency vehicles, damage or victims unless
  supported by the supplied source.

SENSITIVE
- Use only when the human loss itself requires exceptional restraint.
- Examples include child deaths, multiple fatalities, mass-casualty
  events, suicide, sexual violence, highly vulnerable victims,
  bereavement-focused stories, memorial coverage, or incidents where
  grief and victim suffering are the central subject.
- Do not classify an ordinary single-fatality traffic or workplace
  accident as SENSITIVE solely because a person died.
- Quiet, respectful and low-saturation.
- Never use the DAK mascot.
- Never turn deaths or victims into spectacle.
- Prefer symbolic or contextual imagery where a literal depiction
  would be insensitive.

            Examples of the distinction:

            A motorway collision, including a single fatal crash, with road
closures, investigation or emergency response is normally SERIOUS.

A school bus crash in which students died is SENSITIVE.

A workplace accident in which one worker died is normally SERIOUS.

A mass-casualty crash, child death, suicide or grief-focused memorial
story is SENSITIVE.

            A skilled-migration invitation round is STANDARD.

            A Census participation reminder may be LIGHT.

            When unsure between LIGHT and STANDARD, choose STANDARD.
            When unsure between SERIOUS and SENSITIVE, do not choose SENSITIVE
only because a death occurred.

Choose SENSITIVE only when exceptional human vulnerability, grief,
multiple deaths, children, suicide, sexual violence, mass-casualty
harm or comparable highly sensitive circumstances are central.
Otherwise choose SERIOUS.
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

              "secondaryFacts": [
  {
    "role": "SECONDARY",
    "emphasis": "NORMAL",
    "label": "짧은 라벨",
    "value": "추가로 알아둘 핵심 정보",
    "note": "필요한 경우 짧은 보충 설명",
    "icon": "LIGHTBULB"
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
- Use one to five distinct facts that help the reader understand:
  what happened, when, where, who was affected, the current situation,
  or why it matters.
- Make each label and value form a natural semantic pair in Korean.
  The label must describe the type of information shown in the value.
  For example, do not use a label meaning "분야" when the value
  describes locations or road sections; use "시험 구간" instead.
- Prefer short, natural Korean news wording over literal translations
  of English source phrasing.
- Never return zero supportingFacts merely because fewer than two
  useful facts are available.
- If exactly one useful distinct source-supported fact is available,
  return that one fact.
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

secondaryFacts:
- secondaryFacts are optional.
- Use zero to two secondaryFacts.
- They are useful practical or contextual information that is less important
  than the main supportingFacts.
- For LIGHT cards, prefer source-supported information such as:
  venue details, special programs, booking information, eligibility,
  practical tips, privacy information or another useful additional detail.
- Do not move essential information into secondaryFacts.
- Do not repeat title, headline, primaryFact or supportingFacts.
- Do not repeat the same underlying fact using different wording.
- Never invent secondaryFacts merely to fill empty visual space.
- If the source does not contain useful additional information,
  return an empty array.

CARD TEXT DENSITY:
- These facts will appear inside small visual panels, not in an article.
- Write for a mobile card that must be understood at a glance.

- label:
  Prefer 2-6 Korean characters.
  Use a category such as "현황", "영향 지역", "피해", "발생 시간",
  "건강 위험" or "현재 상황".

- value:
  Prefer one compact fact of roughly 3-12 Korean characters.
  It must remain visually concise and should fit within one or two short lines.
  Do not write a complete explanatory sentence as value.
  Do not preserve unnecessary detail merely because it appears in the source.

  Compress locations, dates and times to the shortest form that still
  communicates the useful fact.

  

AUSTRALIAN PLACE-NAME RULES:

- Do not literally translate Australian proper place names into Korean.
- Street names, road names, suburb names and other geographic proper names
  must retain their proper-name meaning.
- Transliterate the proper name into Korean where appropriate rather than
  translating the ordinary English meaning of its words.
- "Park Terrace" -> "파크 테라스", never "공원 테라스".
- "Salisbury Highway" -> "솔즈베리 하이웨이".
- "North Terrace" -> "노스 테라스", never "북쪽 테라스".
- "King William Street" -> "킹 윌리엄 스트리트".
- Suburb names such as "Salisbury" -> "솔즈베리".
- When shortening a location for a card, remove unnecessary detail rather
  than translating or altering the proper name.

  Examples:
  - "Park Terrace and Salisbury Highway intersection" -> "솔즈베리 교차로"
  - "29 July 2026 at approximately 6:30am" -> "7월 29일 오전 6시 30분경"
  - Do not include the year when it is unnecessary for understanding the card.

  Do not split one fact between value and note merely to make the value shorter.
  If the complete source detail would make the value too long, summarise it
  rather than moving the remaining words into note.

- note:
  Optional.
  Default to null.
  Most fact boxes should not need a note.

  Use note only when it provides genuinely separate context that is
  necessary to understand the value.
  Omit it when the value is already understandable by itself.
  When needed, use only one short Korean phrase, preferably 2-10 characters.
  Never use a complete sentence.
  Never add secondary background information merely to fill the note field.

  value and note must have different jobs:
  - value = the main fact.
  - note = only essential secondary context that adds new information.
  - Never move part of the main fact into note just to shorten value.
  - Never repeat, restate, narrow or paraphrase information already present
    in value.
  - A suburb or area name must not be used as note when that location is
    already stated or implied by value.
  - If removing note does not materially reduce understanding, use null.

  Good:
  label: "도로 현황"
  value: "오후 1시 30분 재개"
  note: null

  Bad:
  label: "발생 장소"
  value: "솔즈베리 교차로"
  note: "솔즈베리"
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
- Use action ONLY when the supplied article explicitly tells the reader
  to do, check, apply, avoid, contact, prepare, report or monitor something.
- Never invent advice or infer an action from general context.

- action represents exactly ONE reader action.
- It is NOT a checklist, sequence, workflow or summary of several recommendations.

- First identify every source-supported action in the article.
- Then rank those actions by immediate usefulness to the reader.
- Select ONLY the single highest-priority action for action.body.
- Discard the other actions from the ActionBlock.

- NEVER combine multiple actions into one body, even if they are all
  explicitly supported by the source.

- NEVER use:
  arrows such as "→",
  numbered steps,
  semicolons,
  slash-separated instructions,
  multiple imperative sentences,
  or labels such as "멈추세요 / 확인하세요 / 보호하세요"
  inside one ActionBlock.

- If action.body contains two different things the reader should do,
  it is invalid. Rewrite it so only ONE action remains.

- For active safety or public-health stories:
  immediate preventive action outranks later response action.
  Preventive action outranks symptom monitoring.
  Symptom monitoring outranks emergency escalation only when no
  preventive action is supplied.

- For scams or consumer warnings:
  if the source explicitly instructs victims to report the incident,
  contacting or reporting to the official authority may be selected
  as the single action.
  Do not additionally include checking, stopping payment, contacting
  a bank, contacting police or other steps in the same ActionBlock.

- title:
  Prefer 2-8 Korean characters.
  The title describes the ONE action only.
  Examples:
  "피해 신고"
  "지금 확인"
  "신청 방법"
  "주민 보호"

- body:
  Write exactly ONE Korean imperative instruction.
  Prefer roughly 15-30 Korean characters.
  The body must contain only one main verb/action.

  GOOD:
  "피해를 입었다면 Scamwatch에 신고하세요."

  GOOD:
  "의심스러운 거래는 즉시 중단하세요."

  GOOD:
  "CBS에 131 882로 신고하세요."

  GOOD:
  "연기나 냄새가 감지되면 문과 창문을 닫으세요."

  BAD:
  "멈추세요 → 확인하세요 → 보호하세요."

  BAD:
  "거래를 중단하고 업체를 확인하세요."

  BAD:
  "은행에 연락한 뒤 Scamwatch에 신고하세요."

  BAD:
  "공식 연락처를 확인하고 은행과 경찰에 신고하세요."

  BAD:
  "피해를 신고하세요. 그리고 은행에도 연락하세요."

- Before returning JSON, perform this final ActionBlock check:

  "Does action.body tell the reader to perform more than one distinct action?"

  If YES:
  remove every action except the single most important one.

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

List<CardSpec.CardFact> supportingFacts =
        parseCardFacts(node.path("supportingFacts"));

List<CardSpec.CardFact> secondaryFacts =
        parseCardFacts(node.path("secondaryFacts"));

if (secondaryFacts != null) {
    secondaryFacts =
            secondaryFacts.stream()
                    .limit(2)
                    .toList();
}

CardSpec.Callout callout =
        parseCallout(node.path("callout"));

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
secondaryFacts,
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

    String note = cleanFactNote(
            value,
            nullableText(node.path("note"))
    );

    return new CardSpec.CardFact(
            nullableText(node.path("role")),
            nullableText(node.path("emphasis")),
            nullableText(node.path("label")),
            value,
            note,
            nullableText(node.path("icon"))
    );
}
  
  private String cleanFactNote(
        String value,
        String note
) {

    if (note == null || note.isBlank()) {
        return null;
    }

    String cleanValue =
            value == null
                    ? ""
                    : value.replaceAll("\\s+", "").trim();

    String cleanNote =
            note.replaceAll("\\s+", "").trim();

    /*
     * A note must add genuinely separate context.
     *
     * Remove notes that merely repeat information already carried
     * by the value.
     */
    if (!cleanValue.isEmpty()
            && (cleanValue.contains(cleanNote)
            || cleanNote.contains(cleanValue))) {
        return null;
    }

    /*
     * Very short fragments are usually leftovers created when Claude
     * splits one location or fact between value and note.
     *
     * Examples:
     * value: "솔즈베리 교차로"
     * note:  "파크"
     *
     * Genuine contextual notes such as "연기 흡입" remain untouched.
     */
    if (cleanNote.length() <= 2) {
        return null;
    }

    return note.trim();
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