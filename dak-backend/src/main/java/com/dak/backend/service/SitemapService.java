package com.dak.backend.service;

import com.dak.backend.domain.AustraliaUpdate;
import com.dak.backend.domain.Business;
import com.dak.backend.domain.Event;
import com.dak.backend.domain.Guide;
import com.dak.backend.repository.AustraliaUpdateRepository;
import com.dak.backend.repository.BusinessRepository;
import com.dak.backend.repository.EventRepository;
import com.dak.backend.repository.GuideRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SitemapService {

    // Sitemaps are capped at 50,000 URLs and 50MB by the protocol. Well below
    // that here, but the bound keeps a runaway query from producing an
    // unusable document.
    private static final int MAX_ENTRIES_PER_TYPE = 5000;

    // Only pages with stable, meaningful content. Search results are excluded
    // deliberately: the query permutations are unbounded and each one is a
    // near-duplicate of the others.
    private static final List<String> STATIC_PATHS = List.of(
        "/", "/directory", "/australia-updates", "/guides", "/events");

            private final BusinessRepository businessRepository;
            private final AustraliaUpdateRepository australiaUpdateRepository;
            private final GuideRepository guideRepository;
            private final EventRepository eventRepository;
        
            @Value("${app.site.base-url:https://discoveradelaidekorea.au}")
            private String baseUrl;
        
            public SitemapService(BusinessRepository businessRepository,
                                   AustraliaUpdateRepository australiaUpdateRepository,
                                   GuideRepository guideRepository,
                                   EventRepository eventRepository) {
                this.businessRepository = businessRepository;
                this.australiaUpdateRepository = australiaUpdateRepository;
                this.guideRepository = guideRepository;
                this.eventRepository = eventRepository;
            }

    @Transactional(readOnly = true)
    public String generate() {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String path : STATIC_PATHS) {
            appendUrl(xml, baseUrl + path, null);
        }

        // Only PUBLISHED rows: drafts and archived items are not reachable to
        // an anonymous visitor, and listing them would send crawlers to 404s.
        businessRepository.findByStatus("PUBLISHED", PageRequest.of(0, MAX_ENTRIES_PER_TYPE))
                .forEach(b -> appendUrl(xml,
                        baseUrl + "/businesses/" + b.getSlug(),
                        lastModified(b)));

                        australiaUpdateRepository.findByStatus("PUBLISHED", PageRequest.of(0, MAX_ENTRIES_PER_TYPE))
                        .forEach(u -> appendUrl(xml,
                                baseUrl + "/australia-updates/" + u.getSlug(),
                                u.getCreatedAt()));

        // Guides carry a real updatedAt: unlike a business listing, a guide is
        // revised when the rules it describes change, and the revision is the
        // point. A stale lastmod would tell a crawler not to re-read the page
        // that most needs re-reading.
        guideRepository.findByStatus("PUBLISHED", PageRequest.of(0, MAX_ENTRIES_PER_TYPE))
                .forEach(g -> appendUrl(xml,
                        baseUrl + "/guides/" + g.getSlug(),
                        lastModified(g)));

        // Upcoming only, unlike every other type here. A past event's page
        // still resolves so a shared link does not break, but sending a
        // crawler to something that happened last month earns a search result
        // that wastes the reader's click - which is worse than no result.
        eventRepository.findUpcomingForSitemap(
                        OffsetDateTime.now(), PageRequest.of(0, MAX_ENTRIES_PER_TYPE))
                .forEach(e -> appendUrl(xml,
                        baseUrl + "/events/" + e.getSlug(),
                        lastModified(e)));

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String location, OffsetDateTime lastModified) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escape(location)).append("</loc>\n");
        if (lastModified != null) {
            xml.append("    <lastmod>")
               .append(lastModified.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
               .append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    private OffsetDateTime lastModified(Business b) {
        return b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
    }

    private OffsetDateTime lastModified(Guide g) {
        return g.getUpdatedAt() != null ? g.getUpdatedAt() : g.getPublishedAt();
    }

    private OffsetDateTime lastModified(Event e) {
        return e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt();
    }

    /**
     * Slugs are generated from a restricted character set, so an ampersand in a
     * URL is unlikely — but an unescaped one would make the whole document
     * invalid XML rather than just breaking one entry.
     */
    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}