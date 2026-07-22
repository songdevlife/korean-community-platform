package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "update_sources")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateSource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    // Nullable: not every source is RSS-monitored (e.g. sources used only for manual URL import).
    @Column(name = "rss_feed_url", columnDefinition = "TEXT")
    private String rssFeedUrl;

    @Column(name = "last_polled_at")
    private OffsetDateTime lastPolledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static UpdateSource createNew(String name, String sourceType) {
        UpdateSource source = new UpdateSource();
        source.name = name;
        source.sourceType = sourceType;
        source.createdAt = OffsetDateTime.now();
        return source;
    }
}