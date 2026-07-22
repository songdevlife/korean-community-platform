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

    @Query("""
            SELECT b FROM Business b
            WHERE b.status = :status
              AND (:suburb IS NULL OR b.suburb = :suburb)
              AND (:keyword IS NULL OR
                   LOWER(b.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(b.shortDescription) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<Business> search(@Param("status") String status,
                           @Param("suburb") String suburb,
                           @Param("keyword") String keyword,
                           Pageable pageable);
}