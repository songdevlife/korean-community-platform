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

    /**
     * The public address. Matches guides and events at 320 characters so the
     * three can share validation, and unique so a link resolves to one article.
     *
     * Set once, when the draft is created, and not revised when the title is:
     * an administrator rewrites a headline routinely, and a URL that followed
     * it would break every link already shared.
     */
    @Column(nullable = false, unique = true, length = 320)
    private String slug;

    /**
     * What DAK publishes: a Korean-language summary written by an administrator.
     *
     * Nullable as of V14. An import arrives with nothing here, and publication is
     * gated in AdminAustraliaUpdateService.updateStatus() rather than by the
     * column, so a draft can be saved half-finished without a blank summary ever
     * reaching a reader.
     */
    @Column(name = "korean_summary", columnDefinition = "TEXT")
    private String koreanSummary;

    /**
     * Raw text pulled from the source article, kept as the material a summary is
     * written from. Administrator-only: it appears on AdminUpdateSummaryResponse
     * and has no equivalent field on the public response, so it cannot reach a
     * reader through the API.
     *
     * Separate from koreanSummary as of V14. The two previously shared one column,
     * which meant an administrator who skipped the rewrite published the
     * publisher's own article text. Reproducing an article — in English or
     * translated — is republication rather than reporting on it.
     */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

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

    /**
     * When the update went public, as distinct from when it was imported.
     *
     * An article is fetched, waits in the queue until an administrator has
     * written a Korean summary for it, and goes out some days later. Ordering
     * by createdAt puts it below one imported afterwards and published sooner,
     * which is not the order anything happened in.
     *
     * Null until the first time the status becomes PUBLISHED.
     */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

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
     * Import from a source URL (05 API Spec §10.5). Category and geographic scope
     * are left unset; an administrator supplies them before publishing (enforced
     * in AdminAustraliaUpdateService.updateStatus()).
     *
     * The Korean draft is whatever the summariser produced, and may be null —
     * when no provider is configured, when a call fails, or when the model
     * returned nothing usable. A null draft blocks publication rather than
     * allowing something unreviewed through, which is the safe direction.
     *
     * aiGenerated tracks whether a draft actually came from a model, not whether
     * the item was imported. The flag drives a disclosure banner, so claiming an
     * AI process that did not run would be a false statement about editorial
     * process.
     */
    public static AustraliaUpdate createDraftFromImport(String title, String extractedText,
                                                         String koreanDraft) {
        OffsetDateTime now = OffsetDateTime.now();
        AustraliaUpdate update = new AustraliaUpdate();
        update.title = title;
        update.extractedText = extractedText;
        update.koreanSummary = koreanDraft;
        update.status = "DRAFT";
        update.aiGenerated = koreanDraft != null && !koreanDraft.isBlank();
        update.createdAt = now;
        update.updatedAt = now;
        return update;
        }

    /**
     * Overrides the generated setter so publication is recorded when it
     * happens.
     *
     * Set once. Restoring something from the archive puts it back where it
     * was rather than presenting it as new, so a second PUBLISHED does not
     * move the date.
     */
    public void setStatus(String status) {

        if ("PUBLISHED".equals(status) && publishedAt == null) {
            publishedAt = OffsetDateTime.now();
        }

        this.status = status;
    }
}