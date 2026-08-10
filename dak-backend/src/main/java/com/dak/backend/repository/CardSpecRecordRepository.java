package com.dak.backend.repository;

import com.dak.backend.domain.CardSpecRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardSpecRecordRepository
        extends JpaRepository<CardSpecRecord, UUID> {

    Optional<CardSpecRecord> findByContentTypeAndContentId(
            String contentType,
            UUID contentId
    );

    void deleteByContentTypeAndContentId(
            String contentType,
            UUID contentId
    );
}