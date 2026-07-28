package com.dak.backend.service;

/**
 * What the summariser reports back about one fetched article.
 *
 * Relevance and drafting are decided in a single call because the article text
 * has to be sent either way — asking twice would double the cost for no gain.
 *
 * @param relevant     whether this article is worth an administrator's attention
 * @param reason       why, in one line. Recorded for irrelevant items so the
 *                     prompt can be tuned against real misses rather than guesses
 * @param koreanDraft  a Korean-language briefing written from the article's
 *                     facts. Null when not relevant. Never a translation: a
 *                     translated article is a derivative work and carries the
 *                     same exposure as republishing the original
 */
public record SummarisationResult(
        boolean relevant,
        String reason,
        String koreanDraft
) {

    public static SummarisationResult irrelevant(String reason) {
        return new SummarisationResult(false, reason, null);
    }

    public static SummarisationResult relevant(String koreanDraft, String reason) {
        return new SummarisationResult(true, reason, koreanDraft);
    }
}