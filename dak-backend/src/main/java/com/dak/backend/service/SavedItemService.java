package com.dak.backend.service;

import com.dak.backend.domain.Business;
import com.dak.backend.domain.SavedItem;
import com.dak.backend.domain.User;
import com.dak.backend.dto.SaveItemRequest;
import com.dak.backend.dto.SavedItemResponse;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.AustraliaUpdateRepository;
import com.dak.backend.repository.BusinessRepository;
import com.dak.backend.repository.SavedItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SavedItemService {

    private final SavedItemRepository savedItemRepository;
    private final BusinessRepository businessRepository;
    private final AustraliaUpdateRepository australiaUpdateRepository;

    public SavedItemService(SavedItemRepository savedItemRepository,
                             BusinessRepository businessRepository,
                             AustraliaUpdateRepository australiaUpdateRepository) {
        this.savedItemRepository = savedItemRepository;
        this.businessRepository = businessRepository;
        this.australiaUpdateRepository = australiaUpdateRepository;
    }

    @Transactional(readOnly = true)
    public List<SavedItemResponse> getSavedItems(User user, String resourceType) {
        List<SavedItem> items = (resourceType != null)
                ? savedItemRepository.findByUserIdAndResourceTypeOrderByCreatedAtDesc(user.getId(), resourceType)
                : savedItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SavedItemResponse save(User user, SaveItemRequest request) {
        if (savedItemRepository.existsByUserIdAndResourceTypeAndResourceId(
                user.getId(), request.resourceType(), request.resourceId())) {
            throw ApiException.conflict("ITEM_ALREADY_SAVED", "This item is already saved.");
        }

        validateResourceExists(request.resourceType(), request.resourceId());

        SavedItem item = SavedItem.createNew(user, request.resourceType(), request.resourceId());
        savedItemRepository.save(item);

        return toResponse(item);
    }

    @Transactional
    public void remove(User user, UUID savedItemId) {
        SavedItem item = savedItemRepository.findByIdAndUserId(savedItemId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Saved item not found."));

        savedItemRepository.delete(item);
    }

    @Transactional
    public void removeByResource(User user, String resourceType, UUID resourceId) {
        savedItemRepository.findByUserIdAndResourceTypeAndResourceId(user.getId(), resourceType, resourceId)
                .ifPresent(savedItemRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isSaved(User user, String resourceType, UUID resourceId) {
        return savedItemRepository.existsByUserIdAndResourceTypeAndResourceId(
                user.getId(), resourceType, resourceId);
    }

    private void validateResourceExists(String resourceType, UUID resourceId) {
        boolean exists = switch (resourceType) {
            case "BUSINESS" -> businessRepository.existsById(resourceId);
            case "AUSTRALIA_UPDATE" -> australiaUpdateRepository.existsById(resourceId);
            default -> throw ApiException.badRequest("UNSUPPORTED_RESOURCE_TYPE",
                    "Saving this resource type is not yet supported.");
        };

        if (!exists) {
            throw ApiException.notFound("The resource you are trying to save does not exist.");
        }
    }

    private SavedItemResponse toResponse(SavedItem item) {
        String title = "(삭제된 항목)";
        String slugOrId = item.getResourceId().toString();

        if ("BUSINESS".equals(item.getResourceType())) {
            Business business = businessRepository.findById(item.getResourceId()).orElse(null);
            if (business != null) {
                title = business.getName();
                slugOrId = business.getSlug();
            }
        } else if ("AUSTRALIA_UPDATE".equals(item.getResourceType())) {
            var update = australiaUpdateRepository.findById(item.getResourceId()).orElse(null);
            if (update != null) {
                title = update.getTitle();
                slugOrId = update.getId().toString();
            }
        }

        return new SavedItemResponse(
                item.getId(), item.getResourceType(), item.getResourceId(), title, slugOrId, item.getCreatedAt());
    }
}