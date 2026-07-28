package com.dak.backend.repository;

import com.dak.backend.domain.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Business> findByStatus(String status, Pageable pageable);

    /**
     * Directory and search share this query, per 05 API Spec 9.2's note that
     * business search may reuse the directory list endpoint's logic.
     *
     * Every filter is null-tolerant so callers can supply any subset. The
     * keyword clause is parenthesised as a unit: AND binds tighter than OR, so
     * without the inner brackets a keyword match would bypass the status,
     * suburb and category conditions entirely.
     */
    @Query("""
            SELECT b FROM Business b
            WHERE b.status = :status
              AND (:suburb IS NULL OR b.suburb = :suburb)
              AND (:category IS NULL OR EXISTS (
                    SELECT 1 FROM b.categories c WHERE c.name = :category
                  ))
              AND (:verified IS NULL OR b.verified = :verified)
              AND (:keyword IS NULL OR (
                    LOWER(b.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                    LOWER(b.shortDescription) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                  ))
            """)
    Page<Business> search(@Param("status") String status,
                           @Param("suburb") String suburb,
                           @Param("category") String category,
                           @Param("keyword") String keyword,
                           @Param("verified") Boolean verified,
                           Pageable pageable);
}