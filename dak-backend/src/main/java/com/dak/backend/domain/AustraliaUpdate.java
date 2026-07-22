package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "australia_updates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AustraliaUpdate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "korean_summary", nullable = false, columnDefinition = "TEXT")
    private String koreanSummary;

    // Nullable as of V5: an AI-imported draft has no category until an admin assigns one.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private UpdateCategory category;

    // Nullable as of V5: same reasoning as category.
    @Column(name = "geographic_scope", length = 50)
    private String geographicScope;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "australiaUpdate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UpdateSourceReference> sources = new HashSet<>();

    /** Manual creation by an administrator — category and scope are known up front. */
    public static AustraliaUpdate createNew(String title, String koreanSummary,
                                             UpdateCategory category, String geographicScope) {
        OffsetDateTime now = OffsetDateTime.now();
        AustraliaUpdate update = new AustraliaUpdate();
        update.title = title;
        update.koreanSummary = koreanSummary;
        update.category = category;
        update.geographicScope = geographicScope;
        update.createdAt = now;
        update.updatedAt = now;
        return update;
    }

    /**
     * AI-assisted import (05 API Spec §10.5) — category and geographic scope are
     * deliberately left unset; an administrator must complete these before publishing
     * (enforced in AdminAustraliaUpdateService.updateStatus()).
     */
    public static AustraliaUpdate createDraftFromImport(String title, String draftSummary) {
        OffsetDateTime now = OffsetDateTime.now();
        AustraliaUpdate update = new AustraliaUpdate();
        update.title = title;
        update.koreanSummary = draftSummary;
        update.status = "DRAFT";
        update.aiGenerated = true;
        update.createdAt = now;
        update.updatedAt = now;
        return update;
    }
}