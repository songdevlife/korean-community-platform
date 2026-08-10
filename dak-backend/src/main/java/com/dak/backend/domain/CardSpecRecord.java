package com.dak.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The stored card specification for one piece of content.
 *
 * Named Record rather than CardSpec so it does not collide with the DTO the
 * renderer works from. This is the persisted form; CardSpec is the shape the
 * rest of the application uses.
 */
@Entity
@Table(name = "card_specs")
public class CardSpecRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spec_json", nullable = false, columnDefinition = "jsonb")
    private String specJson;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected CardSpecRecord() {
    }

    public CardSpecRecord(
            String contentType,
            UUID contentId,
            String specJson
    ) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.specJson = specJson;
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

    public String getSpecJson() {
        return specJson;
    }

    public void setSpecJson(String specJson) {
        this.specJson = specJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}