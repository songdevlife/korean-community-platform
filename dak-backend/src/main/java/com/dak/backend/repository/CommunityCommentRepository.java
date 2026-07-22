package com.dak.backend.repository;

import com.dak.backend.domain.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, UUID> {

    List<CommunityComment> findByPostIdAndStatusOrderByCreatedAtAsc(UUID postId, String status);
}