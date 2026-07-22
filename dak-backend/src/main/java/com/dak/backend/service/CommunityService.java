package com.dak.backend.service;

import com.dak.backend.domain.CommunityComment;
import com.dak.backend.domain.CommunityPost;
import com.dak.backend.domain.User;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.CommunityCommentRepository;
import com.dak.backend.repository.CommunityPostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommunityService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;

    public CommunityService(CommunityPostRepository communityPostRepository,
                             CommunityCommentRepository communityCommentRepository) {
        this.communityPostRepository = communityPostRepository;
        this.communityCommentRepository = communityCommentRepository;
    }

    @Transactional(readOnly = true)
    public Page<CommunityPostResponse> search(String category, String keyword, Pageable pageable) {
        return communityPostRepository.search(category, keyword, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CommunityPostResponse getById(UUID postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Post not found."));
        return toResponse(post);
    }

    @Transactional
    public CommunityPostResponse create(User author, CreateCommunityPostRequest request) {
        CommunityPost post = CommunityPost.createNew(
                author, request.category(), request.title().trim(), request.content());
        communityPostRepository.save(post);
        return toResponse(post);
    }

    @Transactional
    public CommunityPostResponse update(User currentUser, UUID postId, UpdateCommunityPostRequest request) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Post not found."));

        requireOwnerOrAdmin(currentUser, post.getAuthor().getId());

        post.setTitle(request.title().trim());
        post.setContent(request.content());

        return toResponse(post);
    }

    @Transactional
    public void delete(User currentUser, UUID postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Post not found."));

        requireOwnerOrAdmin(currentUser, post.getAuthor().getId());

        // Soft delete, per 05 API Spec §7.1 notes ("Deleted posts should normally be soft deleted").
        post.setStatus("DELETED");
    }

    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getComments(UUID postId) {
        return communityCommentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, "PUBLISHED").stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommunityCommentResponse createComment(User author, UUID postId, CreateCommunityCommentRequest request) {
        CommunityPost post = communityPostRepository.findById(postId)
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Post not found."));

        CommunityComment parent = null;
        if (request.parentCommentId() != null) {
            parent = communityCommentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_PARENT_COMMENT", "Parent comment not found."));

            // 04 Database Design §9.6: "A reply must belong to the same Community Post as its parent comment."
            if (!parent.getPost().getId().equals(postId)) {
                throw ApiException.badRequest("INVALID_PARENT_COMMENT",
                        "Parent comment does not belong to this post.");
            }
        }

        CommunityComment comment = CommunityComment.createNew(post, author, parent, request.content());
        communityCommentRepository.save(comment);

        return toResponse(comment);
    }

    /**
     * Ownership check: the current user must either be the resource's author, or hold
     * the ADMINISTRATOR role (05 API Spec §7.1 "Only the author or an Administrator
     * may update a post"). This is checked here in the service, not in SecurityConfig,
     * because SecurityConfig's path-based rules can express "must be logged in" or
     * "must have role X" but cannot express "must own this specific row" — that
     * requires loading the actual resource first to compare its owner.
     */
    private void requireOwnerOrAdmin(User currentUser, UUID resourceAuthorId) {
        boolean isOwner = currentUser.getId().equals(resourceAuthorId);
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMINISTRATOR".equals(r.getName()));

        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("You do not have permission to modify this resource.");
        }
    }

    private CommunityPostResponse toResponse(CommunityPost p) {
        return new CommunityPostResponse(
                p.getId(), p.getCategory(), p.getTitle(), p.getContent(),
                new CommunityPostResponse.AuthorSummary(p.getAuthor().getId(), p.getAuthor().getDisplayName()),
                p.getCreatedAt()
        );
    }

    private CommunityCommentResponse toResponse(CommunityComment c) {
        UUID parentId = c.getParentComment() != null ? c.getParentComment().getId() : null;
        return new CommunityCommentResponse(
                c.getId(), parentId, c.getContent(),
                new CommunityPostResponse.AuthorSummary(c.getAuthor().getId(), c.getAuthor().getDisplayName()),
                c.getCreatedAt()
        );
    }
}