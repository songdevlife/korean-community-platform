package com.dak.backend.service;

import com.dak.backend.domain.AustraliaUpdate;
import com.dak.backend.domain.UpdateSource;
import com.dak.backend.domain.UpdateSourceReference;
import com.dak.backend.repository.AustraliaUpdateRepository;
import com.dak.backend.repository.UpdateSourceReferenceRepository;
import com.dak.backend.repository.UpdateSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Periodically polls every UpdateSource that has an rss_feed_url configured, and
 * creates a DRAFT AustraliaUpdate for each new (not-already-imported) article found.
 * Mirrors the manual URL-import pipeline (AdminAustraliaUpdateService.importFromUrl)
 * but runs unattended — every result still starts as DRAFT, never PUBLISHED
 * (05 API Spec §10.5: "Imported content must remain a draft until reviewed").
 */
@Service
public class RssPollingService {

    private static final Logger log = LoggerFactory.getLogger(RssPollingService.class);

    private final UpdateSourceRepository updateSourceRepository;
    private final UpdateSourceReferenceRepository updateSourceReferenceRepository;
    private final AustraliaUpdateRepository australiaUpdateRepository;
    private final RssFeedReader rssFeedReader;
    private final UrlContentFetcher urlContentFetcher;
    private final AiSummarizationService aiSummarizationService;

    public RssPollingService(UpdateSourceRepository updateSourceRepository,
                              UpdateSourceReferenceRepository updateSourceReferenceRepository,
                              AustraliaUpdateRepository australiaUpdateRepository,
                              RssFeedReader rssFeedReader,
                              UrlContentFetcher urlContentFetcher,
                              AiSummarizationService aiSummarizationService) {
        this.updateSourceRepository = updateSourceRepository;
        this.updateSourceReferenceRepository = updateSourceReferenceRepository;
        this.australiaUpdateRepository = australiaUpdateRepository;
        this.rssFeedReader = rssFeedReader;
        this.urlContentFetcher = urlContentFetcher;
        this.aiSummarizationService = aiSummarizationService;
    }

    // Runs every hour. fixedRate (not fixedDelay) means "every 3,600,000 ms from
    // the start of the previous run" — fine here since polling a handful of feeds
    // takes seconds, not hours.
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void pollAllFeeds() {
        List<UpdateSource> sources = updateSourceRepository.findAll().stream()
                .filter(s -> s.getRssFeedUrl() != null && !s.getRssFeedUrl().isBlank())
                .toList();

        log.info("RSS polling started for {} source(s)", sources.size());

        for (UpdateSource source : sources) {
            pollSource(source);
        }

        log.info("RSS polling finished");
    }

    private void pollSource(UpdateSource source) {
        List<RssFeedReader.FeedItem> items = rssFeedReader.readFeed(source.getRssFeedUrl());

        for (RssFeedReader.FeedItem item : items) {
            if (updateSourceReferenceRepository.existsBySourceUrl(item.link())) {
                continue; // Already imported this article in a previous poll — skip it.
            }

            try {
                UrlContentFetcher.FetchedContent content = urlContentFetcher.fetch(item.link());
                String title = !item.title().isBlank() ? item.title() : content.title();
                String draftSummary = aiSummarizationService.summarize(title, content.bodyText());

                AustraliaUpdate update = AustraliaUpdate.createDraftFromImport(title, draftSummary);
                australiaUpdateRepository.save(update);

                UpdateSourceReference reference = UpdateSourceReference.createFromPoll(
                        update, source, item.link(), item.title());
                updateSourceReferenceRepository.save(reference);

            } catch (Exception e) {
                // One bad article must not stop the rest of this source's items,
                // or other sources, from being processed.
                log.warn("Failed to import article from {} during RSS poll: {}", item.link(), e.getMessage());
            }
        }

        source.setLastPolledAt(OffsetDateTime.now());
    }
}