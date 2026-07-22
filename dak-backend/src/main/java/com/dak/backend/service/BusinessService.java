package com.dak.backend.service;

import com.dak.backend.domain.Business;
import com.dak.backend.domain.BusinessCategory;
import com.dak.backend.dto.BusinessCategoryResponse;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.BusinessSummaryResponse;
import com.dak.backend.dto.CreateBusinessRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.BusinessCategoryRepository;
import com.dak.backend.repository.BusinessRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository businessCategoryRepository;

    public BusinessService(BusinessRepository businessRepository,
                            BusinessCategoryRepository businessCategoryRepository) {
        this.businessRepository = businessRepository;
        this.businessCategoryRepository = businessCategoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<BusinessSummaryResponse> search(String suburb, String category, String keyword, Pageable pageable) {
        return businessRepository.search("PUBLISHED", suburb, category, keyword, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public BusinessDetailResponse getBySlug(String slug) {
        Business business = businessRepository.findBySlug(slug)
                .filter(b -> "PUBLISHED".equals(b.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Business not found."));

        return toDetail(business);
    }

    @Transactional
    public BusinessDetailResponse create(CreateBusinessRequest request) {
        String slug = generateUniqueSlug(request.name());

        Set<BusinessCategory> categories = request.categoryIds().stream()
                .map(id -> businessCategoryRepository.findById(id)
                        .orElseThrow(() -> ApiException.badRequest(
                                "INVALID_CATEGORY", "Category not found: " + id)))
                .collect(Collectors.toSet());

        Business business = Business.createNew(request.name().trim(), slug);
        business.setShortDescription(request.shortDescription());
        business.setDescription(request.description());
        business.setPhone(request.phone());
        business.setEmail(request.email());
        business.setWebsiteUrl(request.websiteUrl());
        business.setAddressLine(request.addressLine());
        business.setSuburb(request.suburb());
        business.setState(request.state());
        business.setPostcode(request.postcode());
        business.setLatitude(request.latitude());
        business.setLongitude(request.longitude());
        business.setCategories(categories);
        business.setStatus("PENDING");

        businessRepository.save(business);

        return toDetail(business);
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;

        while (businessRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String slugify(String input) {
        String normalised = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFKD);
        String withoutDiacritics = Pattern.compile("\\p{M}").matcher(normalised).replaceAll("");
        String slug = withoutDiacritics.replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return slug.isBlank() ? "business" : slug;
    }

    private BusinessSummaryResponse toSummary(Business b) {
        return new BusinessSummaryResponse(
                b.getId(), b.getName(), b.getSlug(), toCategoryResponses(b),
                b.getSuburb(), b.isVerified(), b.getKoreanAvailable(),
                b.getLatitude(), b.getLongitude()
        );
    }

    private BusinessDetailResponse toDetail(Business b) {
        return new BusinessDetailResponse(
                b.getId(), b.getName(), b.getSlug(), b.getShortDescription(), b.getDescription(),
                b.getPhone(), b.getEmail(), b.getWebsiteUrl(),
                b.getAddressLine(), b.getSuburb(), b.getState(), b.getPostcode(), b.getCountry(),
                b.getLatitude(), b.getLongitude(), b.getKoreanAvailable(), b.isVerified(), b.getStatus(),
                toCategoryResponses(b), b.getCreatedAt()
        );
    }

    private List<BusinessCategoryResponse> toCategoryResponses(Business b) {
        return b.getCategories().stream()
                .map(c -> new BusinessCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }
}