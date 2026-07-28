package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guides")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, unique = true, length = 320)
    private String slug;

    // Separate from body so cards, search results and meta descriptions do not
    // have to truncate raw markdown, which would leak syntax into the output.
    @Column(length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // Nullable for the same reason as australia_updates: a draft may be
    // incomplete, and publication is blocked in the service layer instead.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private GuideCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static Guide createNew(String title, String slug, String summary, String body,
                                  GuideCategory category, User author) {
        OffsetDateTime now = OffsetDateTime.now();
        Guide guide = new Guide();
        guide.title = title;
        guide.slug = slug;
        guide.summary = summary;
        guide.body = body;
        guide.category = category;
        guide.author = author;
        guide.createdAt = now;
        guide.updatedAt = now;
        return guide;
    }

    /**
     * Sets publishedAt only on first publication, so correcting a typo and
     * re-publishing does not reset the date and push the guide back to the top
     * of a date-sorted list.
     */
    public void markPublished() {
        this.status = "PUBLISHED";
        if (this.publishedAt == null) {
            this.publishedAt = OffsetDateTime.now();
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}