package com.dak.backend.repository;

import com.dak.backend.domain.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RentalRepository extends JpaRepository<Rental, UUID> {

    /**
     * The public listing: published and not expired, newest first.
     *
     * Expiry is treated the same way events treat their end date - a listing
     * for a property already let is not stale content, it is wrong content,
     * and a reader who messages about it has been misled by the site. A null
     * expiry is treated as current, which covers rows written before one was
     * set.
     */
    @Query("SELECT r FROM Rental r WHERE r.status = 'PUBLISHED' "
         + "AND (r.expiresAt IS NULL OR r.expiresAt >= :now) "
         + "AND (:suburb IS NULL OR LOWER(r.suburb) = LOWER(CAST(:suburb AS string))) "
         + "AND (:listingType IS NULL OR r.listingType = CAST(:listingType AS string)) "
         + "AND (:maxRent IS NULL OR r.rentMin <= :maxRent) "
         + "ORDER BY r.createdAt DESC")
    Page<Rental> findCurrent(OffsetDateTime now, String suburb, String listingType,
                              Integer maxRent, Pageable pageable);

    Optional<Rental> findBySlugAndStatus(String slug, String status);

    Optional<Rental> findByIdAndStatus(UUID id, String status);

    Page<Rental> findByStatus(String status, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    /** Published and current, for the sitemap. */
    @Query("SELECT r FROM Rental r WHERE r.status = 'PUBLISHED' "
         + "AND (r.expiresAt IS NULL OR r.expiresAt >= :now) "
         + "ORDER BY r.createdAt DESC")
    List<Rental> findCurrentForSitemap(OffsetDateTime now, Pageable pageable);
}