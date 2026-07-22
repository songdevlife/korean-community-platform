package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.User;
import com.dak.backend.dto.*;
import com.dak.backend.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community-posts")
public class CommunityPostController {

    private final CommunityService communityService;

    public CommunityPostController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ApiResponse<Page<CommunityPostResponse>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));
        return ApiResponse.ok(communityService.search(category, keyword, pageable));
    }

    @GetMapping("/{postId}")
    public ApiResponse<CommunityPostResponse> getById(@PathVariable UUID postId) {
        return ApiResponse.ok(communityService.getById(postId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityPostResponse> create(@AuthenticationPrincipal User author,
                                                       @Valid @RequestBody CreateCommunityPostRequest request) {
        return ApiResponse.ok(communityService.create(author, request));
    }

    @PatchMapping("/{postId}")
    public ApiResponse<CommunityPostResponse> update(@AuthenticationPrincipal User currentUser,
                                                       @PathVariable UUID postId,
                                                       @Valid @RequestBody UpdateCommunityPostRequest request) {
        return ApiResponse.ok(communityService.update(currentUser, postId, request));
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID postId) {
        communityService.delete(currentUser, postId);
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<List<CommunityCommentResponse>> getComments(@PathVariable UUID postId) {
        return ApiResponse.ok(communityService.getComments(postId));
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityCommentResponse> createComment(
            @AuthenticationPrincipal User author,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommunityCommentRequest request) {
        return ApiResponse.ok(communityService.createComment(author, postId, request));
    }
}