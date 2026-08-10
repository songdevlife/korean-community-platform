package com.dak.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A stored image belonging to a piece of content.
 *
 * HERO is the generated illustration or photograph that goes inside a card.
 * CARD is the finished 1080x1350 image that gets published.
 */
@Entity
@Table(name = "card_assets")
public class CardAsset {

    public static final String KIND_HERO = "HERO";
    public static final String KIND_CARD = "CARD";

    // The column defaults to gen_random_uuid(), but Hibernate needs to know
    // the value comes from the application rather than from a sequence.
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "asset_kind", nullable = false)
    private String assetKind;

    @Column(name = "spec_hash")
    private String specHash;

    @Column(name = "layout_type")
    private String layoutType;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected CardAsset() {
    }

    public static CardAsset hero(
            String contentType,
            UUID contentId,
            String specHash,
            String layoutType,
            String imageUrl,
            String publicId
    ) {
        CardAsset asset = new CardAsset();
        asset.contentType = contentType;
        asset.contentId = contentId;
        asset.assetKind = KIND_HERO;
        asset.specHash = specHash;
        asset.layoutType = layoutType;
        asset.imageUrl = imageUrl;
        asset.publicId = publicId;
        return asset;
    }

    public static CardAsset card(
            String contentType,
            UUID contentId,
            String layoutType,
            String imageUrl,
            String publicId
    ) {
        CardAsset asset = new CardAsset();
        asset.contentType = contentType;
        asset.contentId = contentId;
        asset.assetKind = KIND_CARD;
        asset.layoutType = layoutType;
        asset.imageUrl = imageUrl;
        asset.publicId = publicId;
        return asset;
    }

    public UUID getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public UUID getContentId() {
        return contentId;
    }

    public String getAssetKind() {
        return assetKind;
    }

    public String getSpecHash() {
        return specHash;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPublicId() {
        return publicId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}