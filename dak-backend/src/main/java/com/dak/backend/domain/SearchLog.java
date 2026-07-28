package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single search, recorded anonymously.
 *
 * There is no author association and no setter for anything: a log entry is
 * written once and never edited. Adding a user reference later would change
 * what this table is, so it is left off rather than made nullable.
 */
@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "search_term", nullable = false, length = 200)
    private String searchTerm;

    // Zero is the useful value here — it marks a search the site could not
    // answer, which is the clearest signal of what is missing.
    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static SearchLog createNew(String searchTerm, int resultCount) {
        SearchLog log = new SearchLog();
        log.searchTerm = searchTerm;
        log.resultCount = resultCount;
        log.createdAt = OffsetDateTime.now();
        return log;
    }
}