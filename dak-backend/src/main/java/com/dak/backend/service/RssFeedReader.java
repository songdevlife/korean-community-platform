package com.dak.backend.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RssFeedReader {

    public record FeedItem(String title, String link) {}

    public List<FeedItem> readFeed(String feedUrl) {
        List<FeedItem> items = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(feedUrl)
                    .userAgent("Mozilla/5.0 (compatible; DAKBot/1.0; +https://discoveradelaidekorea.example)")
                    .timeout(10_000)
                    .parser(Parser.xmlParser())
                    .get();

            for (Element item : doc.select("item")) {
                String title = item.selectFirst("title") != null ? item.selectFirst("title").text() : "";
                String link = item.selectFirst("link") != null ? item.selectFirst("link").text() : "";

                if (!title.isBlank() && !link.isBlank()) {
                    items.add(new FeedItem(title, link));
                }
            }
        } catch (Exception e) {
            // A single bad/unreachable feed must not break polling for other sources —
            // return an empty list rather than throwing, so the caller (scheduled job)
            // can simply move on to the next source.
            return List.of();
        }

        return items;
    }
}