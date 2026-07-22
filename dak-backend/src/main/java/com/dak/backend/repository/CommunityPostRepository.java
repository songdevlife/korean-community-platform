package com.dak.backend.repository;

import com.dak.backend.domain.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, UUID> {

    @Query("""
            SELECT p FROM CommunityPost p
            WHERE p.status = 'PUBLISHED'
              AND (:category IS NULL OR p.category = :category)
              AND (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<CommunityPost> search(@Param("category") String category,
                                @Param("keyword") String keyword,
                                Pageable pageable);
}