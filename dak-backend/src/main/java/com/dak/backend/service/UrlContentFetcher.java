package com.dak.backend.service;

import com.dak.backend.exception.ApiException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class UrlContentFetcher {

    public record FetchedContent(String title, String bodyText) {}

    public FetchedContent fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; DAKBot/1.0; +https://discoveradelaidekorea.example)")
                    .timeout(10_000)
                    .get();

            String title = doc.title();
            String bodyText = doc.body().text();

            // Cap the extracted text so a huge page doesn't get sent whole into a
            // summarisation step later — a few thousand characters is plenty of context.
            if (bodyText.length() > 5000) {
                bodyText = bodyText.substring(0, 5000);
            }

            return new FetchedContent(title, bodyText);
        } catch (Exception e) {
            throw ApiException.badRequest("SOURCE_FETCH_FAILED",
                    "Could not retrieve content from the provided URL: " + e.getMessage());
        }
    }
}