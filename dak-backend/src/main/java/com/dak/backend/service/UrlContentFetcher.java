package com.dak.backend.service;

import com.dak.backend.exception.ApiException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UrlContentFetcher {

    private static final Logger log = LoggerFactory.getLogger(UrlContentFetcher.class);

    private static final int MAX_BODY_CHARS = 5000;

    // A body element that yields less than this is almost certainly a caption or
    // a stray container rather than the article, so fall through to the next
    // candidate instead of accepting it.
    private static final int MIN_ARTICLE_CHARS = 200;

    // Paragraphs shorter than this are rarely prose. News pages mark topic
    // labels, timestamps and bylines up as <p>, and those were appearing at the
    // top of every extract.
    private static final int MIN_PARAGRAPH_CHARS = 40;

    // Tried in order, most specific first. Most news sites mark the article with
    // at least one of these; ABC News uses <article>.
    private static final List<String> ARTICLE_SELECTORS = List.of(
            "article",
            "[itemprop=articleBody]",
            "main",
            "[role=main]",
            "#main-content",
            ".article-body",
            ".story-body"
    );

    // Removed before text extraction. Taking body().text() wholesale was the
    // original defect: navigation and menu labels appear early in the document,
    // so truncating to a character limit kept the chrome and discarded the article.
    private static final String NOISE_SELECTOR =
            "script, style, noscript, nav, header, footer, aside, form, " +
            "figure figcaption, [role=navigation], [role=banner], [role=contentinfo], " +
            ".advertisement, .ad, .social-share, .related-articles, .newsletter-signup";

    public record FetchedContent(String title, String bodyText) {}

    public FetchedContent fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; DAKBot/1.0; +https://discoveradelaidekorea.example)")
                    .timeout(10_000)
                    .get();

            String title = resolveTitle(doc);
            doc.select(NOISE_SELECTOR).remove();

            String bodyText = extractArticleText(doc, url);

            if (bodyText.length() > MAX_BODY_CHARS) {
                bodyText = bodyText.substring(0, MAX_BODY_CHARS);
            }

            return new FetchedContent(title, bodyText);
        } catch (Exception e) {
            throw ApiException.badRequest("SOURCE_FETCH_FAILED",
                    "Could not retrieve content from the provided URL: " + e.getMessage());
        }
    }

    /**
     * Prefers the Open Graph title, which is the headline publishers intend for
     * sharing. The document title usually carries a site-name suffix
     * ("Headline - ABC News") that has to be stripped otherwise.
     */
    private String resolveTitle(Document doc) {
        String ogTitle = doc.select("meta[property=og:title]").attr("content").trim();
        if (!ogTitle.isEmpty()) {
            return ogTitle;
        }

        Element h1 = doc.selectFirst("article h1, main h1, h1");
        if (h1 != null && !h1.text().isBlank()) {
            return h1.text().trim();
        }

        return doc.title();
    }

    /**
     * Walks the candidate selectors and returns the first that yields a plausible
     * amount of paragraph text. Falls back to all paragraphs in the document, and
     * only then to the whole body.
     */
    private String extractArticleText(Document doc, String url) {
        for (String selector : ARTICLE_SELECTORS) {
            Element container = doc.selectFirst(selector);
            if (container == null) continue;

            String text = paragraphText(container);
            if (text.length() >= MIN_ARTICLE_CHARS) {
                return text;
            }
        }

        String allParagraphs = paragraphText(doc);
        if (allParagraphs.length() >= MIN_ARTICLE_CHARS) {
            log.debug("No article container matched for {}; used document paragraphs", url);
            return allParagraphs;
        }

        // Last resort. Logged because reaching here means the result is likely
        // unusable and the page needs a selector adding.
        log.warn("Could not locate article content for {}; falling back to full body text", url);
        return doc.body().text();
    }

    /**
     * Joins paragraph elements with blank lines. Using <p> rather than raw text
     * keeps the extract to prose and drops stray link lists and button labels
     * that survive the noise removal.
     */
    private String paragraphText(Element root) {
        Elements paragraphs = root.select("p");
        StringBuilder sb = new StringBuilder();

        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() < MIN_PARAGRAPH_CHARS) continue;
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(text);
        }

        return sb.toString().trim();
    }
}