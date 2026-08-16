package com.dak.backend.service;

/**
 * Abstraction over "decide whether this article matters to the audience, and if
 * so draft a Korean briefing about it".
 *
 * Swappable implementation: ClaudeSummarizationService is @Primary when an API
 * key is configured; StubAiSummarizationService stands in otherwise so the
 * import pipeline stays testable without one.
 *
 * Implementations must draft rather than translate. DAK publishes its own
 * account of the facts with a link to the original — a translated article is a
 * derivative work, and republishing one in Korean carries the same exposure as
 * republishing it in English.
 */
public interface AiSummarizationService {

    SummarisationResult summarize(
            String title,
            String bodyText
    );

    SummarisationResult summarizeManual(
            String title,
            String bodyText
    );
}