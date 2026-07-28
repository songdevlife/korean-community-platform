package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guide_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideCategory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static GuideCategory createNew(String name, String slug) {
        OffsetDateTime now = OffsetDateTime.now();
        GuideCategory category = new GuideCategory();
        category.name = name;
        category.slug = slug;
        category.createdAt = now;
        category.updatedAt = now;
        return category;
    }
}