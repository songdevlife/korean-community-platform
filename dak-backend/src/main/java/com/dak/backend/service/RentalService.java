package com.dak.backend.service;

import com.dak.backend.domain.Rental;
import com.dak.backend.domain.RentalImage;
import com.dak.backend.dto.RentalImageResponse;
import com.dak.backend.dto.RentalResponse;
import com.dak.backend.dto.RentalSummaryResponse;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.RentalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;

    public RentalService(RentalRepository rentalRepository) {
        this.rentalRepository = rentalRepository;
    }

    /**
     * Published listings that have not expired, newest first.
     *
     * The expiry filter is what separates this from a classifieds page that
     * nobody prunes. A room advertised six weeks ago is almost certainly let,
     * and a reader who messages about it has been sent on an errand by the
     * site rather than merely underserved by it.
     */
    @Transactional(readOnly = true)
    public Page<RentalSummaryResponse> listCurrent(String suburb, String listingType,
                                                     Integer maxRent, int page, int pageSize) {
        String trimmedSuburb = blankToNull(suburb);
        String trimmedType = blankToNull(listingType);

        return rentalRepository
                .findCurrent(OffsetDateTime.now(), trimmedSuburb, trimmedType, maxRent,
                        PageRequest.of(page, Math.min(pageSize, 100)))
                .map(this::toSummary);
    }

    /**
     * Detail, without the expiry filter.
     *
     * A link shared last week should still resolve, and the response says so
     * through hasExpired rather than by returning 404 - the same choice the
     * event detail makes, for the same reason.
     */
    @Transactional(readOnly = true)
    public RentalResponse getByIdentifier(String identifier) {
        Rental rental = resolve(identifier)
                .orElseThrow(() -> ApiException.notFound("Rental not found."));
        return toDetail(rental);
    }

    /** Accepts a slug or a UUID, as events and updates do. */
    private Optional<Rental> resolve(String identifier) {
        try {
            return rentalRepository.findByIdAndStatus(UUID.fromString(identifier), "PUBLISHED");
        } catch (IllegalArgumentException notAUuid) {
            return rentalRepository.findBySlugAndStatus(identifier, "PUBLISHED");
        }
    }

    RentalSummaryResponse toSummary(Rental r) {
        return new RentalSummaryResponse(
                r.getId(), r.getSlug(), r.getTitle(), r.getSuburb(),
                r.getListingType(), r.getRoomTypes(),
                r.getRentMin(), r.getRentMax(),
                r.getBillsIncluded(), r.getAvailableFrom(), r.getFurnished(),
                thumbnailOf(r));
    }

    RentalResponse toDetail(Rental r) {
        OffsetDateTime expires = r.getExpiresAt();

        return new RentalResponse(
                r.getId(), r.getSlug(), r.getTitle(), r.getDescription(), r.getSuburb(),
                r.getListingType(), r.getRoomTypes(),
                r.getRentMin(), r.getRentMax(), r.getBondWeeks(),
                r.getBillsIncluded(), r.getBillsNote(),
                r.getAvailableFrom(), r.getMinTermMonths(), r.getFurnished(),
                r.getRoomsLet(), tenancyStatusOf(r.getRoomsLet()),
                r.getGenderPreference(), r.getCouplesAllowed(),
                r.getPetsAllowed(), r.getSmokingAllowed(),
                r.getInspectionNote(),
                r.getConsentStatus(), r.getSourceUrl(),
                // Belt as well as braces: the column has a check constraint
                // and the write path refuses it, and this makes a row that
                // somehow acquired one anyway harmless to a reader.
                "FULL".equals(r.getConsentStatus()) ? r.getContact() : null,
                toImages(r), r.getStatus(),
                expires != null && expires.isBefore(OffsetDateTime.now()),
                r.getCreatedAt());
    }

    /**
     * Which part of the Residential Tenancies Act, if any, covers an occupant.
     *
     * The single most useful thing DAK can say about a rental and the thing no
     * classifieds site says at all. Two or more rooms let makes the property a
     * rooming house under Part 7: the bond must be lodged with CBS, sixty
     * days' notice is required to remove a resident, and the room must have a
     * lock. One room let in an owner-occupied house falls outside the Act
     * under section 5(b), where the only protection is whatever the agreement
     * says. Five or more rooms additionally requires the operator to register.
     *
     * Advertisements rarely state the number, so UNKNOWN is the usual answer
     * and is worth printing rather than hiding: a reader who knows to count
     * the rooms before signing is better off than one who does not know the
     * question decides anything.
     */
    static String tenancyStatusOf(Integer roomsLet) {
        if (roomsLet == null) return "UNKNOWN";
        if (roomsLet >= 5) return "REGISTERED_ROOMING_HOUSE";
        if (roomsLet >= 2) return "ROOMING_HOUSE";
        return "OUTSIDE_ACT";
    }

    private String thumbnailOf(Rental r) {
        List<RentalImage> images = r.getImages();
        return (images == null || images.isEmpty()) ? null : images.get(0).getImageUrl();
    }

    private List<RentalImageResponse> toImages(Rental r) {
        if (r.getImages() == null) return List.of();
        return r.getImages().stream()
                .map(i -> new RentalImageResponse(
                        i.getId(), i.getImageUrl(), i.getAltText(), i.getDisplayOrder()))
                .toList();
    }

    /**
     * Prefers an administrator's slug and falls back to the title, then to a
     * dated one. Suburb-led where there is nothing else, because a rental
     * without a usable title is still a room somewhere.
     *
     * No date suffix as a matter of course, unlike events: a listing does not
     * recur, so a collision means two rooms in the same street rather than the
     * same room next week, and a number is the honest way to say so.
     */
    static String resolveSlug(String requested, String title, String suburb,
                               Predicate<String> taken) {
        String base = slugify(requested);
        if (base.isBlank()) base = slugify(title);
        if (base.isBlank()) base = slugify(suburb);

        if (base.isBlank()) {
            base = "rental-"
                    + OffsetDateTime.now().atZoneSameInstant(ZoneId.of("Australia/Adelaide"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String candidate = base;
        int suffix = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    /** Same rules as guides, events and updates. */
    static String slugify(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}