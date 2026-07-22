package com.dak.backend.service;

/**
 * Abstraction over "turn source content into a draft Korean summary".
 *
 * Swappable implementation: StubAiSummarizationService (naive extraction, no real
 * AI call) is active for now. When a real AI provider/API key is available, add a
 * new implementation (e.g. ClaudeSummarizationService) and switch which bean is
 * @Primary — no other code in the import pipeline needs to change.
 */
public interface AiSummarizationService {

    String summarize(String title, String bodyText);
}