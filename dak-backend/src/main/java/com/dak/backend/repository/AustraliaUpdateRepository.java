package com.dak.backend.repository;

import com.dak.backend.domain.AustraliaUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AustraliaUpdateRepository extends JpaRepository<AustraliaUpdate, UUID> {

    @Query("""
            SELECT u FROM AustraliaUpdate u
            WHERE u.status = :status
              AND (:categoryId IS NULL OR u.category.id = :categoryId)
              AND (:keyword IS NULL OR
                   LOWER(u.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<AustraliaUpdate> search(@Param("status") String status,
                                  @Param("categoryId") UUID categoryId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);
}