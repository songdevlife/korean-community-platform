package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static SavedItem createNew(User user, String resourceType, UUID resourceId) {
        SavedItem item = new SavedItem();
        item.user = user;
        item.resourceType = resourceType;
        item.resourceId = resourceId;
        item.createdAt = OffsetDateTime.now();
        return item;
    }
}