package com.dak.backend.service;

import com.dak.backend.domain.Business;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.UpdateBusinessStatusRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.BusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dak.backend.dto.BusinessCategoryResponse;

@Service
public class AdminBusinessService {

    private final BusinessRepository businessRepository;

    public AdminBusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Transactional
    public BusinessDetailResponse updateStatus(UUID businessId, UpdateBusinessStatusRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("Business not found."));

        // Publishing requires at least one category, per 04 Database Design §8.2.3
        // ("At least one category may be required before a Business can be published").
        if ("PUBLISHED".equals(request.status()) && business.getCategories().isEmpty()) {
            throw ApiException.badRequest("MISSING_CATEGORY",
                    "A business must have at least one category before it can be published.");
        }

        business.setStatus(request.status());

        return toDetail(business);
    }

    private BusinessDetailResponse toDetail(Business b) {
        List<BusinessCategoryResponse> categories = b.getCategories().stream()
                .map(c -> new BusinessCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .collect(Collectors.toList());

        return new BusinessDetailResponse(
                b.getId(), b.getName(), b.getSlug(), b.getShortDescription(), b.getDescription(),
                b.getPhone(), b.getEmail(), b.getWebsiteUrl(),
                b.getAddressLine(), b.getSuburb(), b.getState(), b.getPostcode(), b.getCountry(),
                b.getLatitude(), b.getLongitude(), b.getKoreanAvailable(), b.isVerified(), b.getStatus(),
                categories, b.getCreatedAt()
        );
    }
}