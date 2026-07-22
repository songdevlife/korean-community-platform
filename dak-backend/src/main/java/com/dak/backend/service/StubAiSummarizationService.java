package com.dak.backend.service;

import org.springframework.stereotype.Service;

/**
 * Placeholder implementation: extracts the first few sentences of the fetched body
 * text rather than calling a real AI model. This is NOT a Korean translation or a
 * real summary — it exists so the import pipeline (fetch -> draft -> admin review)
 * is fully wired and testable without an AI provider API key configured yet.
 *
 * 03 MVP Feature Specification §12 requires AI-generated content to be clearly
 * identified and reviewed by an administrator before publication — this stub
 * satisfies that by producing an obviously-incomplete draft that forces a human
 * to rewrite it (see the disclaimer text below) rather than silently producing
 * something that looks finished but isn't.
 */
@Service
public class StubAiSummarizationService implements AiSummarizationService {

    private static final String DISCLAIMER =
            "[자동 추출 초안 — AI 요약/번역 미적용, 관리자 검토 및 재작성 필요]\n\n";

    @Override
    public String summarize(String title, String bodyText) {
        String[] sentences = bodyText.split("(?<=[.!?])\\s+");
        StringBuilder extract = new StringBuilder();

        for (int i = 0; i < sentences.length && i < 3; i++) {
            extract.append(sentences[i].trim()).append(" ");
        }

        return DISCLAIMER + extract.toString().trim();
    }
}