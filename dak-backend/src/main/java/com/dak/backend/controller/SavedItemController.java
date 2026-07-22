package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.User;
import com.dak.backend.dto.SaveItemRequest;
import com.dak.backend.dto.SavedItemResponse;
import com.dak.backend.service.SavedItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/saved-items")
public class SavedItemController {

    private final SavedItemService savedItemService;

    public SavedItemController(SavedItemService savedItemService) {
        this.savedItemService = savedItemService;
    }

    @GetMapping
    public ApiResponse<List<SavedItemResponse>> getSavedItems(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type
    ) {
        return ApiResponse.ok(savedItemService.getSavedItems(user, type));
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> isSaved(
            @AuthenticationPrincipal User user,
            @RequestParam String resourceType,
            @RequestParam UUID resourceId
    ) {
        return ApiResponse.ok(savedItemService.isSaved(user, resourceType, resourceId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SavedItemResponse> save(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody SaveItemRequest request) {
        return ApiResponse.ok(savedItemService.save(user, request));
    }

    @DeleteMapping("/{savedItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal User user, @PathVariable UUID savedItemId) {
        savedItemService.remove(user, savedItemId);
    }
}