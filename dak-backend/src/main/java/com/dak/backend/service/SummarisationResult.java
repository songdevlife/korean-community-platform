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
    String koreanTitle,
    String koreanDraft
) {

public static SummarisationResult irrelevant(String reason) {
    return new SummarisationResult(false, reason, null, null);
}

/**
 * A draft with a headline of its own. The headline is written from the
 * article's facts like the body is, not translated from the source's, so
 * that the words a Korean reader would search for appear in the one field
 * that weighs most in a search result.
 */
public static SummarisationResult relevant(String koreanTitle, String koreanDraft, String reason) {
    return new SummarisationResult(true, reason, koreanTitle, koreanDraft);
}

/**
 * No headline available. Kept as a separate entry point because the callers
 * that use it are failure paths — no provider, a failed call, a malformed
 * reply — where inventing a headline would be worse than falling back to
 * the source's.
 */
public static SummarisationResult relevant(String koreanDraft, String reason) {
    return new SummarisationResult(true, reason, null, koreanDraft);
}
}