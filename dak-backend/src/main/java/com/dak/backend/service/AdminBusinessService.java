package com.dak.backend.service;

import com.dak.backend.domain.Business;
import com.dak.backend.dto.AdminBusinessSummaryResponse;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.UpdateBusinessStatusRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.BusinessRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminBusinessService {

    private final BusinessRepository businessRepository;

    public AdminBusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminBusinessSummaryResponse> listAll(String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));
        Page<Business> businesses = (status != null)
                ? businessRepository.findByStatus(status, pageable)
                : businessRepository.findAll(pageable);

        return businesses.map(b -> new AdminBusinessSummaryResponse(b.getId(), b.getName(), b.getSlug(), b.getStatus()));
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

        return toDetail(business);
    }

    private BusinessDetailResponse toDetail(Business b) {
        List<com.dak.backend.dto.BusinessCategoryResponse> categories = b.getCategories().stream()
                .map(c -> new com.dak.backend.dto.BusinessCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();

        return new BusinessDetailResponse(
                b.getId(), b.getName(), b.getSlug(), b.getShortDescription(), b.getDescription(),
                b.getPhone(), b.getEmail(), b.getWebsiteUrl(),
                b.getAddressLine(), b.getSuburb(), b.getState(), b.getPostcode(), b.getCountry(),
                b.getLatitude(), b.getLongitude(), b.getKoreanAvailable(), b.isVerified(), b.getStatus(),
                categories, b.getCreatedAt()
        );
    }
}