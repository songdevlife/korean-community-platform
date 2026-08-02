package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Seeded in V21. A table rather than an enum so that a category can be added
 * without a deployment - the six chosen were drawn from what the incumbent
 * Korean Adelaide board actually posts about, and that reading may be wrong
 * for the working-holiday audience DAK is also aiming at.
 */
@Entity
@Table(name = "event_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventCategory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}