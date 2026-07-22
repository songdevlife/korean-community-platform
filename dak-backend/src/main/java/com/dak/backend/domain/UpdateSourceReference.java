package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "update_source_references")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateSourceReference {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "australia_update_id", nullable = false)
    private AustraliaUpdate australiaUpdate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private UpdateSource source;

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_title", length = 300)
    private String sourceTitle;

    @Column(name = "accessed_at", nullable = false)
    private OffsetDateTime accessedAt;

    public static UpdateSourceReference createNew(AustraliaUpdate update, UpdateSource source,
                                                   String sourceUrl, String sourceTitle) {
        UpdateSourceReference ref = new UpdateSourceReference();
        ref.australiaUpdate = update;
        ref.source = source;
        ref.sourceUrl = sourceUrl;
        ref.sourceTitle = sourceTitle;
        ref.accessedAt = OffsetDateTime.now();
        return ref;
    }
}