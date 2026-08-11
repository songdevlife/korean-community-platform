package com.dak.backend.repository;

import com.dak.backend.domain.AustraliaUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AustraliaUpdateRepository extends JpaRepository<AustraliaUpdate, UUID> {

        Page<AustraliaUpdate> findByStatus(String status, Pageable pageable);

        /**
         * The admin queue, ordered by when each item went out.
         *
         * Nulls last rather than first: rows published before publishedAt
         * existed were backfilled from createdAt, so a null here means a row
         * that has never been published, and those belong at the bottom.
         */
        @Query("""
                SELECT u FROM AustraliaUpdate u
                WHERE u.status = :status
                ORDER BY u.publishedAt DESC NULLS LAST, u.createdAt DESC
                """)
        Page<AustraliaUpdate> findByStatusOrderByPublished(
                @Param("status") String status,
                Pageable pageable
        );
    
        /**
         * Detail lookup by the address readers actually use. Both forms resolve so
         * that UUID links shared before V26 do not break; the detail page redirects
         * one to the other.
         */
        Optional<AustraliaUpdate> findBySlugAndStatus(String slug, String status);
    
        boolean existsBySlug(String slug);

    /** On edit, the update's own slug must not count as a collision. */
    boolean existsBySlugAndIdNot(String slug, UUID id);

    /**
     * Category and scope are independent axes: a reader may want immigration
     * news, Adelaide news, or immigration news about Adelaide. Both filters are
     * null-tolerant so any combination works.
     *
     * Keyword matches the Korean summary as well as the title. Titles here are
     * the publisher's own English headlines, so searching on title alone meant a
     * Korean speaker searching in Korean found nothing — on the one section of
     * the site written specifically for them. The summary is what they read and
     * what they will remember a phrase from.
     */
    @Query("""
            SELECT u FROM AustraliaUpdate u
            WHERE u.status = :status
              AND (:categoryId IS NULL OR u.category.id = :categoryId)
              AND (:scope IS NULL OR u.geographicScope = :scope)
              AND (:keyword IS NULL OR
                   LOWER(u.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(u.koreanSummary) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<AustraliaUpdate> search(@Param("status") String status,
                                  @Param("categoryId") UUID categoryId,
                                  @Param("scope") String scope,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);
}