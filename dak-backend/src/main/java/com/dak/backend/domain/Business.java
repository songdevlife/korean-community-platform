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

/**
 * Maps to the `businesses` table (04_Database_Design_DAK.docx §8.2.1).
 * Deferred to later: opening hours, reviews, business managers, verification
 * records — each has its own table, not yet implemented.
 */
@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String phone;
    private String email;

    @Column(name = "website_url", columnDefinition = "TEXT")
    private String websiteUrl;

    @Column(name = "address_line")
    private String addressLine;
    private String suburb;
    private String state;
    private String postcode;

    @Column(nullable = false, length = 2)
    private String country = "AU";

    private Double latitude;
    private Double longitude;

    @Column(name = "korean_available", nullable = false, length = 40)
    private String koreanAvailable = "UNVERIFIED";

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "business_category_assignments",
        joinColumns = @JoinColumn(name = "business_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<BusinessCategory> categories = new HashSet<>();

    public static Business createNew(String name, String slug) {
        OffsetDateTime now = OffsetDateTime.now();
        Business business = new Business();
        business.name = name;
        business.slug = slug;
        business.createdAt = now;
        business.updatedAt = now;
        return business;
    }
}