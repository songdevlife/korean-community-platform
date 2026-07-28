package com.dak.backend.service;

import com.dak.backend.domain.Business;
import com.dak.backend.domain.BusinessCategory;
import com.dak.backend.dto.AdminBusinessSummaryResponse;
import com.dak.backend.dto.BusinessCategoryResponse;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.BusinessImageResponse;
import com.dak.backend.dto.UpdateBusinessRequest;
import com.dak.backend.dto.UpdateBusinessStatusRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.BusinessCategoryRepository;
import com.dak.backend.repository.BusinessRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminBusinessService {

    // Mirrors the korean_available check constraint.
    private static final Set<String> VALID_KOREAN_AVAILABLE = Set.of(
            "KOREAN_SPEAKING_OWNER", "KOREAN_SPEAKING_STAFF",
            "BY_APPOINTMENT", "TRANSLATION_ASSISTANCE", "UNVERIFIED");

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository businessCategoryRepository;

    public AdminBusinessService(BusinessRepository businessRepository,
                                 BusinessCategoryRepository businessCategoryRepository) {
        this.businessRepository = businessRepository;
        this.businessCategoryRepository = businessCategoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminBusinessSummaryResponse> listAll(String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));
        Page<Business> businesses = (status != null)
                ? businessRepository.findByStatus(status, pageable)
                : businessRepository.findAll(pageable);

        return businesses.map(b -> new AdminBusinessSummaryResponse(b.getId(), b.getName(), b.getSlug(), b.getStatus()));
    }

    /**
     * Edits a business after it has been created. Published listings need
     * correcting too — a phone number changes, a description has a typo — and
     * until now the only mutable field was status.
     *
     * The slug is deliberately not regenerated when the name changes: it is
     * part of the public URL, and silently breaking existing links to fix a
     * typo would be a worse outcome than a slug that no longer matches.
     */
    @Transactional
    public BusinessDetailResponse update(UUID businessId, UpdateBusinessRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("Business not found."));

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw ApiException.badRequest("INVALID_NAME", "Name cannot be empty.");
            }
            business.setName(name);
        }

        if (request.shortDescription() != null) business.setShortDescription(request.shortDescription());
        if (request.description() != null) business.setDescription(request.description());
        if (request.phone() != null) business.setPhone(request.phone());
        if (request.email() != null) business.setEmail(request.email());
        if (request.websiteUrl() != null) business.setWebsiteUrl(request.websiteUrl());
        if (request.addressLine() != null) business.setAddressLine(request.addressLine());
        if (request.suburb() != null) business.setSuburb(request.suburb());
        if (request.state() != null) business.setState(request.state());
        if (request.postcode() != null) business.setPostcode(request.postcode());
        if (request.latitude() != null) business.setLatitude(request.latitude());
        if (request.longitude() != null) business.setLongitude(request.longitude());
        if (request.verified() != null) business.setVerified(request.verified());

        if (request.koreanAvailable() != null) {
            String value = request.koreanAvailable().trim().toUpperCase();
            if (!VALID_KOREAN_AVAILABLE.contains(value)) {
                throw ApiException.badRequest("INVALID_KOREAN_AVAILABLE",
                        "Must be one of: " + String.join(", ", VALID_KOREAN_AVAILABLE));
            }
            business.setKoreanAvailable(value);
        }

        if (request.categoryIds() != null) {
            if (request.categoryIds().isEmpty()) {
                throw ApiException.badRequest("MISSING_CATEGORY",
                        "A business must have at least one category.");
            }
            Set<BusinessCategory> categories = request.categoryIds().stream()
                    .map(id -> businessCategoryRepository.findById(id)
                            .orElseThrow(() -> ApiException.badRequest(
                                    "INVALID_CATEGORY", "Category not found: " + id)))
                    .collect(Collectors.toSet());
            business.setCategories(categories);
        }

        business.setUpdatedAt(OffsetDateTime.now());

        return toDetail(business);
    }

    @Transactional
    public BusinessDetailResponse updateStatus(UUID businessId, UpdateBusinessStatusRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("Business not found."));

        if ("PUBLISHED".equals(request.status()) && business.getCategories().isEmpty()) {
            throw ApiException.badRequest("MISSING_CATEGORY",
                    "A business must have at least one category before it can be published.");
        }

        business.setStatus(request.status());
        business.setUpdatedAt(OffsetDateTime.now());

        return toDetail(business);
    }

    // Duplicated from BusinessService — both build the same response shape.
    // Worth extracting to a shared mapper if a third caller appears.
    private BusinessDetailResponse toDetail(Business b) {
        List<BusinessCategoryResponse> categories = b.getCategories().stream()
                .map(c -> new BusinessCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();

        List<BusinessImageResponse> images = b.getImages().stream()
                .map(i -> new BusinessImageResponse(
                        i.getId(), i.getImageUrl(), i.getAltText(), i.getDisplayOrder()))
                .toList();

        return new BusinessDetailResponse(
                b.getId(), b.getName(), b.getSlug(), b.getShortDescription(), b.getDescription(),
                b.getPhone(), b.getEmail(), b.getWebsiteUrl(),
                b.getAddressLine(), b.getSuburb(), b.getState(), b.getPostcode(), b.getCountry(),
                b.getLatitude(), b.getLongitude(), b.getKoreanAvailable(), b.isVerified(), b.getStatus(),
                categories, images, b.getCreatedAt()
        );
    }
}