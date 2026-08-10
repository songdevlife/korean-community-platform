package com.dak.backend.repository;

import com.dak.backend.domain.CardAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardAssetRepository extends JpaRepository<CardAsset, UUID> {

    Optional<CardAsset> findByContentTypeAndContentIdAndAssetKindAndSpecHash(
            String contentType,
            UUID contentId,
            String assetKind,
            String specHash
    );

    List<CardAsset> findByContentTypeAndContentIdAndAssetKindOrderByCreatedAtDesc(
            String contentType,
            UUID contentId,
            String assetKind
    );

    List<CardAsset> findByContentTypeAndContentIdAndAssetKind(
            String contentType,
            UUID contentId,
            String assetKind
    );
}