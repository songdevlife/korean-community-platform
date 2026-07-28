package com.dak.backend.repository;

import com.dak.backend.domain.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GuideRepository extends JpaRepository<Guide, UUID> {

    Optional<Guide> findBySlugAndStatus(String slug, String status);

    boolean existsBySlug(String slug);

    // Used when editing an existing guide, so its own slug is not treated as a collision.
    boolean existsBySlugAndIdNot(String slug, UUID id);

    Page<Guide> findByStatus(String status, Pageable pageable);

    // Supports the "reject deletion of a category still in use" rule in 05 API Spec.
    long countByCategoryId(UUID categoryId);

    /**
     * Keyword matches title, summary and body. Body is included because a guide
     * is long and specific: someone looking for the practical test will search
     * VORT, and someone looking for a certified translation will search NAATI —
     * terms that appear in the article and nowhere in its title or summary.
     *
     * CAST(:keyword AS string) is required — see 07 Entry 8, where the untyped
     * parameter caused the same query to fail at runtime.
     */
    @Query("""
            SELECT g FROM Guide g
            WHERE g.status = :status
              AND (:categoryId IS NULL OR g.category.id = :categoryId)
              AND (:keyword IS NULL OR
                   LOWER(g.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(g.summary) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(g.body) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<Guide> search(@Param("status") String status,
                       @Param("categoryId") UUID categoryId,
                       @Param("keyword") String keyword,
                       Pageable pageable);
}