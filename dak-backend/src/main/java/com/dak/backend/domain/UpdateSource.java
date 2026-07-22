package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "update_sources")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateSource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}