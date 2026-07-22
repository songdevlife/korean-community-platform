package com.dak.backend.repository;

import com.dak.backend.domain.SavedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedItemRepository extends JpaRepository<SavedItem, UUID> {

    List<SavedItem> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SavedItem> findByUserIdAndResourceTypeOrderByCreatedAtDesc(UUID userId, String resourceType);

    boolean existsByUserIdAndResourceTypeAndResourceId(UUID userId, String resourceType, UUID resourceId);

    Optional<SavedItem> findByIdAndUserId(UUID id, UUID userId);

    Optional<SavedItem> findByUserIdAndResourceTypeAndResourceId(UUID userId, String resourceType, UUID resourceId);
}