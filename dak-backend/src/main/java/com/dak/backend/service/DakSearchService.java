package com.dak.backend.service;

import com.dak.backend.dto.AustraliaUpdateSummaryResponse;
import com.dak.backend.dto.DakSearchResult;
import com.dak.backend.dto.GuideSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Content search across published guides and Australia Updates.
 *
 * This exists because the repositories match a keyword as a single LIKE
 * substring, which works for a search box but not for a sentence. Someone
 * typing "렌트 계약 중간에 나가면 어떻게 되나요" into a chat produces a keyword
 * that appears in no article, so the query returns nothing every time. The work
 * here is turning an utterance into terms worth searching, running each, and
 * merging what comes back.
 *
 * Nothing in this class knows about KakaoTalk. The website's /search and any
 * later assistant should be able to call it unchanged.
 */
@Service
public class DakSearchService {

    private static final Logger log = LoggerFactory.getLogger(DakSearchService.class);

    /**
     * Searching every token would run a query per word for no gain - the tail of
     * a sentence is rarely what distinguishes it. Four is enough to carry the
     * subject of a question.
     */
    private static final int MAX_TERMS = 4;

    /**
     * Single characters match far too much. Korean content words are almost
     * always two syllables or more, and a one-character term would pull in
     * every article containing it anywhere in its body.
     */
    private static final int MIN_TERM_LENGTH = 2;

    /**
     * Words that carry no subject: question forms, polite endings, and requests.
     * Left in, they match nothing useful but still cost a query each, and they
     * crowd out the real terms under the MAX_TERMS cap.
     */
    private static final Set<String> STOPWORDS = Set.of(
            "뭐", "뭔가", "무엇", "어떻게", "어떡해", "어디", "언제", "누구", "왜", "얼마",
            "알려줘", "알려주세요", "가르쳐줘", "해줘", "해주세요", "궁금해", "궁금해요",
            "있나요", "있어요", "있음", "인가요", "일까요", "되나요", "될까요", "하나요",
            "그리고", "그런데", "근데", "혹시", "좀", "제발", "please", "the", "and", "for"
    );

    /**
     * Korean is agglutinative: a particle attaches directly to the noun, so
     * "렌트를" and "렌트는" are distinct strings from "렌트" and a LIKE search for
     * the inflected form misses the article every time. Stripping a trailing
     * particle recovers the stem in the common cases without needing a
     * morphological analyser.
     *
     * Longer forms come first so that "에서" is tried before "서".
     */
    private static final List<String> PARTICLES = List.of(
            "에서는", "에서도", "으로는", "이라고", "라고", "에서", "에게", "한테",
            "으로", "부터", "까지", "보다", "처럼", "이나", "든지",
            "은", "는", "이", "가", "을", "를", "의", "에", "도", "만", "과", "와", "로"
    );

    private final GuideService guideService;
    private final AustraliaUpdateService australiaUpdateService;
    private final String siteBaseUrl;

    public DakSearchService(GuideService guideService,
                            AustraliaUpdateService australiaUpdateService,
                            @Value("${app.site.base-url}") String siteBaseUrl) {
        this.guideService = guideService;
        this.australiaUpdateService = australiaUpdateService;
        // Trailing slash would produce a double slash in every URL built below.
        this.siteBaseUrl = siteBaseUrl.endsWith("/")
                ? siteBaseUrl.substring(0, siteBaseUrl.length() - 1)
                : siteBaseUrl;
    }

    /**
     * Searches guides and updates for anything matching the utterance.
     *
     * Guides are returned before updates regardless of score. A guide is written
     * to stay correct and has been through verification; an update is news and
     * is stale within weeks. Someone asking how second working holiday visas
     * work wants the guide, not last Tuesday's article about processing delays.
     *
     * @param utterance   raw text as the user typed it
     * @param maxGuides   how many guides to return at most
     * @param maxUpdates  how many updates to return at most
     */
    @Transactional(readOnly = true)
    public List<DakSearchResult> search(String utterance, int maxGuides, int maxUpdates) {

        List<String> terms = extractTerms(utterance);
        if (terms.isEmpty()) {
            return List.of();
        }

        log.debug("DAK search running with {} term(s)", terms.size());

        // Keyed by slug so the same article found through two different terms is
        // one result whose score has been added up, not two rows.
        Map<String, DakSearchResult> guideHits = new LinkedHashMap<>();
        Map<String, DakSearchResult> updateHits = new LinkedHashMap<>();

        for (String term : terms) {
            // Over-fetch relative to the caps: the best result for the whole
            // question may be ranked low for any single term, and it can only
            // win on combined score if it was retrieved at all.
            guideService.search(null, term, 0, 5)
                    .forEach(g -> accumulate(guideHits, toResult(g)));

            Pageable updatePage = PageRequest.of(0, 5,
                    Sort.by(Sort.Direction.DESC, "createdAt"));
            australiaUpdateService.search(null, null, term, updatePage)
                    .forEach(u -> accumulate(updateHits, toResult(u)));
        }

        List<DakSearchResult> results = new ArrayList<>();
        results.addAll(rankAndCap(guideHits, maxGuides));
        results.addAll(rankAndCap(updateHits, maxUpdates));
        return results;
    }

    /**
     * Breaks an utterance into terms worth searching.
     *
     * Both the inflected word and its stripped stem are kept where they differ.
     * The stem usually matches the title, but the inflected form can match body
     * text, and there is no reliable way to tell in advance which will hit.
     */
    private List<String> extractTerms(String utterance) {
        if (utterance == null || utterance.isBlank()) {
            return List.of();
        }

        // Anything that is not a letter or digit is a separator. Covers spaces,
        // Korean and Latin punctuation, and the emoji people put in chat.
        String[] words = utterance.trim().split("[^\\p{L}\\p{N}]+");

        // LinkedHashSet: order follows the sentence, which puts the subject
        // first in most questions, and duplicates collapse.
        Set<String> terms = new LinkedHashSet<>();

        for (String word : words) {
            if (terms.size() >= MAX_TERMS) {
                break;
            }
            String candidate = word.trim();
            if (candidate.length() < MIN_TERM_LENGTH) {
                continue;
            }
            if (STOPWORDS.contains(candidate.toLowerCase())) {
                continue;
            }

            terms.add(candidate);

            String stem = stripParticle(candidate);
            if (!stem.equals(candidate) && stem.length() >= MIN_TERM_LENGTH
                    && terms.size() < MAX_TERMS) {
                terms.add(stem);
            }
        }

        return List.copyOf(terms);
    }

    /**
     * Removes one trailing particle if the remainder is still a plausible word.
     *
     * The length guard matters: "이자" would otherwise be reduced to "이", which
     * matches almost everything.
     */
    private String stripParticle(String word) {
        for (String particle : PARTICLES) {
            if (word.length() > particle.length() + 1 && word.endsWith(particle)) {
                return word.substring(0, word.length() - particle.length());
            }
        }
        return word;
    }

    /**
     * Adds a hit, or increments the score of one already found.
     *
     * The score is simply how many distinct terms found this article. An article
     * matching three terms of a question is more likely to be about that
     * question than one matching a single term.
     */
    private void accumulate(Map<String, DakSearchResult> hits, DakSearchResult result) {
        DakSearchResult existing = hits.get(result.slug());
        if (existing == null) {
            hits.put(result.slug(), result);
        } else {
            hits.put(result.slug(), new DakSearchResult(
                    existing.type(), existing.title(), existing.slug(),
                    existing.summary(), existing.url(), existing.score() + 1));
        }
    }

    /**
     * Highest score first. Ties keep insertion order, which is the order the
     * repositories returned them - newest published first.
     */
    private List<DakSearchResult> rankAndCap(Map<String, DakSearchResult> hits, int cap) {
        if (cap <= 0) {
            return List.of();
        }
        return hits.values().stream()
                .sorted(Comparator.comparingInt(DakSearchResult::score).reversed())
                .limit(cap)
                .toList();
    }

    private DakSearchResult toResult(GuideSummaryResponse g) {
        return new DakSearchResult(
                DakSearchResult.Type.GUIDE,
                g.title(),
                g.slug(),
                g.summary(),
                siteBaseUrl + "/guides/" + g.slug(),
                1);
    }

    private DakSearchResult toResult(AustraliaUpdateSummaryResponse u) {
        return new DakSearchResult(
                DakSearchResult.Type.UPDATE,
                u.title(),
                u.slug(),
                u.koreanSummary(),
                siteBaseUrl + "/australia-updates/" + u.slug(),
                1);
    }
}