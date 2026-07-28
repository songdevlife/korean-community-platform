package com.dak.backend.service;

import org.springframework.stereotype.Service;

/**
 * Fallback for when no AI provider is configured. Judges nothing and drafts
 * nothing: every article is passed through as relevant with no Korean text, so
 * an administrator sees it in the queue and writes the summary by hand.
 *
 * This is deliberately not the old behaviour. The previous stub extracted the
 * first few English sentences behind a Korean disclaimer and put them in the
 * summary field, which is how unreviewed source text reached publication. An
 * empty draft cannot be published at all — updateStatus refuses it — so the
 * failure mode is now a blocked item rather than a leaked one.
 */
@Service
public class StubAiSummarizationService implements AiSummarizationService {

    @Override
    public SummarisationResult summarize(String title, String bodyText) {
        return SummarisationResult.relevant(
                null, "No AI provider configured — relevance not assessed.");
    }
}