package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps to the `business_images` table.
 *
 * MVP holds an external URL rather than an uploaded file. When object storage
 * arrives (04 DB Design §8.10), this entity gains a storage key and imageUrl
 * becomes a derived value; consumers of the API need not change.
 */
@Entity
@Table(name = "business_images")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessImage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static BusinessImage createNew(Business business, String imageUrl,
                                          String altText, int displayOrder) {
        BusinessImage image = new BusinessImage();
        image.business = business;
        image.imageUrl = imageUrl;
        image.altText = altText;
        image.displayOrder = displayOrder;
        image.createdAt = OffsetDateTime.now();
        return image;
    }
}