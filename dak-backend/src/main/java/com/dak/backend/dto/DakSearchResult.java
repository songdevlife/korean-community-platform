package com.dak.backend.dto;

/**
 * A single search hit, stripped of whichever content type produced it.
 *
 * Guides and Australia Updates have different shapes (summary vs koreanSummary,
 * publishedAt vs createdAt) and different DTOs. Callers that only need to show
 * "here is something on DAK about this" should not have to know which is which,
 * so both collapse into this one record.
 *
 * Deliberately not tied to KakaoTalk. The website's /search and any future
 * assistant should be able to use the same service without a Kakao-shaped
 * result object getting in the way.
 */
public record DakSearchResult(
        Type type,
        String title,
        String slug,
        String summary,
        String url,
        int score
) {
    public enum Type {
        GUIDE,
        UPDATE
    }
}