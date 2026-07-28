package com.dak.backend.service;

import com.dak.backend.domain.AustraliaUpdate;
import com.dak.backend.domain.UpdateSource;
import com.dak.backend.domain.UpdateSourceReference;
import com.dak.backend.repository.AustraliaUpdateRepository;
import com.dak.backend.repository.UpdateSourceReferenceRepository;
import com.dak.backend.repository.UpdateSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * Scheduled polling is gated behind app.rss.polling-enabled. The check lives
 * inside the method rather than on the bean, because AdminRssPollController
 * depends on this service for manual triggering and must still start.
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

    @Value("${app.rss.polling-enabled:false}")
    private boolean pollingEnabled;

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
    public void scheduledPoll() {
        if (!pollingEnabled) {
            log.debug("Scheduled RSS polling is disabled (app.rss.polling-enabled=false)");
            return;
        }
        pollAllFeeds();
    }

    /**
     * Runs a poll regardless of the schedule setting. Called by the admin
     * controller so a poll can still be triggered deliberately while the
     * unattended schedule is off.
     */
    @Transactional
    public void pollAllFeeds() {
        List<UpdateSource> sources = updateSourceRepository.findAll().stream()
                .filter(s -> s.getRssFeedUrl() != null && !s.getRssFeedUrl().isBlank())
                .toList();

        log.info("RSS polling started for {} source(s)", sources.size());

        int succeeded = 0;
        for (UpdateSource source : sources) {
            // A feed that is unreachable, malformed, or has moved must not stop
            // the other sources being polled.
            try {
                pollSource(source);
                succeeded++;
            } catch (Exception e) {
                log.warn("Failed to poll source '{}' ({}): {}",
                        source.getName(), source.getRssFeedUrl(), e.getMessage());
            }
        }

        log.info("RSS polling finished: {} of {} source(s) polled successfully",
                succeeded, sources.size());
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

                SummarisationResult result = aiSummarizationService.summarize(title, content.bodyText());

                AustraliaUpdate update = AustraliaUpdate.createDraftFromImport(
                        title, content.bodyText(), result.koreanDraft());
                // Geographic scope is a property of the feed far more often than
                // of the individual article: an Adelaide feed produces Adelaide
                // news. Seeding it here leaves the admin only the category to
                // decide. Null where the source covers no single area.
                update.setGeographicScope(source.getDefaultGeographicScope());

                // Archived rather than skipped when judged irrelevant. The
                // judgement is a model's and will sometimes be wrong, so the
                // article stays recoverable from the archive — and the archive
                // doubles as the record of what the filter rejected, which is
                // what the prompt gets tuned against.
                if (!result.relevant()) {
                    update.setStatus("ARCHIVED");
                    log.info("Filtered out '{}': {}", title, result.reason());
                }

                australiaUpdateRepository.save(update);

                UpdateSourceReference reference = UpdateSourceReference.createFromPoll(
                        update, source, item.link(), item.title());
                updateSourceReferenceRepository.save(reference);
                update.getSources().add(reference);

            } catch (Exception e) {
                // One bad article must not stop the rest of this source's items,
                // or other sources, from being processed.
                log.warn("Failed to import article from {} during RSS poll: {}", item.link(), e.getMessage());
            }
        }

        source.setLastPolledAt(OffsetDateTime.now());
    }
}