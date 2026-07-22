package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "update_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateCategory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}