package com.dak.backend.service;

import com.dak.backend.domain.Rental;
import com.dak.backend.domain.RentalImage;
import com.dak.backend.dto.AdminRentalSummaryResponse;
import com.dak.backend.dto.CreateRentalRequest;
import com.dak.backend.dto.RentalResponse;
import com.dak.backend.dto.UpdateRentalRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.RentalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminRentalService {

    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Set<String> VALID_LISTING_TYPES = Set.of(
            "SHARE_ROOM", "WHOLE_PROPERTY", "LEASE_TRANSFER", "STUDENT_ACCOMMODATION");
    private static final Set<String> VALID_BILLS = Set.of(
            "INCLUDED", "EXCLUDED", "OPTIONAL", "UNKNOWN");
    private static final Set<String> VALID_CONSENT = Set.of("NONE", "LINK_ONLY", "FULL");

    /**
     * Twenty-one days from publication.
     *
     * A room in Adelaide is usually taken within one to three weeks, and the
     * advertiser almost never comes back to say so - which is the failure mode
     * that kills a listings board, because a page of let properties teaches a
     * reader not to return. Expiry is therefore the default rather than
     * something someone has to remember, and an advertiser who is still
     * looking says so to have it extended. Silence is treated as let, which is
     * right far more often than it is wrong.
     */
    private static final int LISTING_DAYS = 21;

    /**
     * Beyond this a bond is unlawful: four weeks at or below $800 a week, six
     * above it. Not refused, because DAK records what an advertisement says
     * rather than correcting it - but flagged, so an administrator sees it
     * before a reader does.
     */
    private static final BigDecimal BOND_CAP_STANDARD = new BigDecimal("4");
    private static final BigDecimal BOND_CAP_HIGH_RENT = new BigDecimal("6");
    private static final int HIGH_RENT_THRESHOLD = 800;

    private final RentalRepository rentalRepository;
    private final RentalService rentalService;

    public AdminRentalService(RentalRepository rentalRepository, RentalService rentalService) {
        this.rentalRepository = rentalRepository;
        this.rentalService = rentalService;
    }

    @Transactional(readOnly = true)
    public Page<AdminRentalSummaryResponse> listAll(String status, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, Math.min(pageSize, 100));

        Page<Rental> rentals = (status != null && !status.isBlank())
                ? rentalRepository.findByStatus(status, pageable)
                : rentalRepository.findAll(pageable);

        OffsetDateTime now = OffsetDateTime.now();

        return rentals.map(r -> new AdminRentalSummaryResponse(
                r.getId(), r.getSlug(), r.getTitle(), r.getSuburb(), r.getRentMin(),
                r.getStatus(), r.getConsentStatus(), r.getConsentNote(), r.getSourceUrl(),
                r.getContact() != null,
                r.getImages() == null ? 0 : r.getImages().size(),
                r.getExpiresAt(),
                r.getExpiresAt() != null && r.getExpiresAt().isBefore(now),
                r.getCreatedAt()));
    }

    /** One listing regardless of status, for the edit screen. */
    @Transactional(readOnly = true)
    public RentalResponse getById(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> ApiException.notFound("Rental not found."));
        return rentalService.toDetail(rental);
    }

    @Transactional
    public RentalResponse create(CreateRentalRequest request) {
        String consent = normaliseConsent(request.consentStatus());

        Rental rental = Rental.createNew(
                request.title().trim(),
                request.suburb().trim(),
                normaliseListingType(request.listingType()),
                request.rentMin());

        rental.setSlug(RentalService.resolveSlug(
                request.slug(), request.title(), request.suburb(),
                rentalRepository::existsBySlug));

        rental.setConsentStatus(consent);
        rental.setConsentNote(blankToNull(request.consentNote()));

        applyFields(rental, request.description(), request.roomTypes(), request.rentMax(),
                request.bondWeeks(), request.billsIncluded(), request.billsNote(),
                request.availableFrom(), request.minTermMonths(), request.furnished(),
                request.roomsLet(), request.genderPreference(), request.couplesAllowed(),
                request.petsAllowed(), request.smokingAllowed(), request.inspectionNote(),
                request.sourceUrl());

        // Refused rather than dropped. A listing that quietly lost a number
        // would look like a defect; one that quietly kept it would be a breach
        // of the undertaking in content-policy.md section 17.
        applyConsentGatedFields(rental, request.contact(), request.imageUrls());

        validate(rental);
        rentalRepository.save(rental);

        return rentalService.toDetail(rental);
    }

    /**
     * Null means "leave alone" for every field, as the event update does.
     *
     * The exception is consentStatus: lowering it from FULL clears the contact
     * and the images in the same operation, whether or not the request
     * mentioned them. An advertiser who withdraws permission has withdrawn it
     * from everything, and leaving the photographs behind because the request
     * only named the status would be the kind of partial compliance that is
     * worse than none.
     */
    @Transactional
    public RentalResponse update(UUID rentalId, UpdateRentalRequest request) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> ApiException.notFound("Rental not found."));

        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw ApiException.badRequest("INVALID_TITLE", "Title cannot be empty.");
            }
            rental.setTitle(title);
        }
        if (request.suburb() != null) {
            String suburb = request.suburb().trim();
            if (suburb.isEmpty()) {
                throw ApiException.badRequest("INVALID_SUBURB", "Suburb cannot be empty.");
            }
            rental.setSuburb(suburb);
        }
        if (request.listingType() != null) {
            rental.setListingType(normaliseListingType(request.listingType()));
        }
        if (request.rentMin() != null) rental.setRentMin(request.rentMin());

        if (request.consentStatus() != null) {
            String consent = normaliseConsent(request.consentStatus());
            if (!"FULL".equals(consent)) {
                rental.setContact(null);
                rental.getImages().clear();
            }
            rental.setConsentStatus(consent);
        }
        if (request.consentNote() != null) {
            rental.setConsentNote(blankToNull(request.consentNote()));
        }

        applyFields(rental, request.description(), request.roomTypes(), request.rentMax(),
                request.bondWeeks(), request.billsIncluded(), request.billsNote(),
                request.availableFrom(), request.minTermMonths(), request.furnished(),
                request.roomsLet(), request.genderPreference(), request.couplesAllowed(),
                request.petsAllowed(), request.smokingAllowed(), request.inspectionNote(),
                request.sourceUrl());

        if (request.contact() != null || request.imageUrls() != null) {
            applyConsentGatedFields(rental,
                    request.contact() != null ? request.contact() : rental.getContact(),
                    request.imageUrls());
        }

        if (request.status() != null) {
            String status = request.status().trim().toUpperCase();
            if (!VALID_STATUSES.contains(status)) {
                throw ApiException.badRequest("INVALID_STATUS",
                        "Status must be one of: " + String.join(", ", VALID_STATUSES));
            }
            // Publishing starts the clock. Doing it here rather than on
            // create means a draft that sits for a week still gets its full
            // twenty-one days once it goes up.
            if ("PUBLISHED".equals(status) && !"PUBLISHED".equals(rental.getStatus())) {
                rental.setExpiresAt(OffsetDateTime.now().plusDays(LISTING_DAYS));
            }
            rental.setStatus(status);
        }

        // Set explicitly, this wins - which is how an extension is granted
        // when an advertiser says they are still looking.
        if (request.expiresAt() != null) {
            rental.setExpiresAt(request.expiresAt());
        }

        validate(rental);
        rental.setUpdatedAt(OffsetDateTime.now());

        return rentalService.toDetail(rental);
    }

    @Transactional
    public void delete(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> ApiException.notFound("Rental not found."));
        rentalRepository.delete(rental);
    }

    /** Grants another full listing period from now. */
    @Transactional
    public RentalResponse extend(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> ApiException.notFound("Rental not found."));
        rental.setExpiresAt(OffsetDateTime.now().plusDays(LISTING_DAYS));
        rental.setUpdatedAt(OffsetDateTime.now());
        return rentalService.toDetail(rental);
    }

    private void applyFields(Rental rental, String description, String roomTypes,
                              Integer rentMax, BigDecimal bondWeeks, String billsIncluded,
                              String billsNote, java.time.LocalDate availableFrom,
                              Integer minTermMonths, Boolean furnished, Integer roomsLet,
                              String genderPreference, Boolean couplesAllowed,
                              Boolean petsAllowed, Boolean smokingAllowed,
                              String inspectionNote, String sourceUrl) {
        if (description != null) rental.setDescription(blankToNull(description));
        if (roomTypes != null) rental.setRoomTypes(blankToNull(roomTypes));
        if (rentMax != null) rental.setRentMax(rentMax);
        if (bondWeeks != null) rental.setBondWeeks(bondWeeks);
        if (billsIncluded != null) rental.setBillsIncluded(normaliseBills(billsIncluded));
        if (billsNote != null) rental.setBillsNote(blankToNull(billsNote));
        if (availableFrom != null) rental.setAvailableFrom(availableFrom);
        if (minTermMonths != null) rental.setMinTermMonths(minTermMonths);
        if (furnished != null) rental.setFurnished(furnished);
        if (roomsLet != null) rental.setRoomsLet(roomsLet);
        if (genderPreference != null) rental.setGenderPreference(blankToNull(genderPreference));
        if (couplesAllowed != null) rental.setCouplesAllowed(couplesAllowed);
        if (petsAllowed != null) rental.setPetsAllowed(petsAllowed);
        if (smokingAllowed != null) rental.setSmokingAllowed(smokingAllowed);
        if (inspectionNote != null) rental.setInspectionNote(blankToNull(inspectionNote));
        if (sourceUrl != null) rental.setSourceUrl(blankToNull(sourceUrl));
    }

    /**
     * The contact and the photographs, which exist only with permission.
     *
     * Both are refused with a message naming the reason rather than accepted
     * and discarded. The check constraint on the table would catch the contact
     * anyway, but it reports a violated constraint rather than what was wrong,
     * and there is no constraint at all on the images.
     */
    private void applyConsentGatedFields(Rental rental, String contact, List<String> imageUrls) {
        boolean permitted = "FULL".equals(rental.getConsentStatus());

        String trimmedContact = blankToNull(contact);
        if (trimmedContact != null && !permitted) {
            throw ApiException.badRequest("CONSENT_REQUIRED",
                    "Contact details can only be published where the advertiser has given "
                    + "permission. Set consent to FULL, or leave the field empty and link "
                    + "to the original advertisement instead.");
        }
        rental.setContact(trimmedContact);

        if (imageUrls == null) return;

        List<String> urls = imageUrls.stream()
                .map(this::blankToNull)
                .filter(u -> u != null)
                .toList();

        if (!urls.isEmpty() && !permitted) {
            throw ApiException.badRequest("CONSENT_REQUIRED",
                    "Photographs can only be published where the advertiser has supplied them "
                    + "or agreed to their use.");
        }

        // clear() then add(), not a new list: orphanRemoval means Hibernate
        // owns this collection and replacing the reference throws.
        rental.getImages().clear();
        int order = 0;
        for (String url : urls) {
            rental.getImages().add(RentalImage.createNew(rental, url, null, order++));
        }
    }

    /**
     * The things that make a listing wrong rather than incomplete.
     *
     * A bond above the statutory cap is not refused: DAK records what an
     * advertisement says, and an unlawful demand is worth showing a reader
     * rather than hiding from them. It is worth an administrator seeing
     * first, though, which is what the message is for.
     */
    private void validate(Rental rental) {
        if (rental.getRentMax() != null && rental.getRentMax() < rental.getRentMin()) {
            throw ApiException.badRequest("INVALID_RENT_RANGE",
                    "The upper rent cannot be below the lower one.");
        }

        BigDecimal bond = rental.getBondWeeks();
        if (bond != null) {
            BigDecimal cap = rental.getRentMin() > HIGH_RENT_THRESHOLD
                    ? BOND_CAP_HIGH_RENT : BOND_CAP_STANDARD;
            if (bond.compareTo(cap) > 0) {
                throw ApiException.badRequest("BOND_ABOVE_CAP",
                        "The bond exceeds the legal maximum of " + cap.toPlainString()
                        + " weeks for this rent. Check the advertisement; if it really says "
                        + "this, note it in the description rather than the bond field.");
            }
        }

        if (!"FULL".equals(rental.getConsentStatus())
                && blankToNull(rental.getSourceUrl()) == null) {
            throw ApiException.badRequest("SOURCE_REQUIRED",
                    "Without the advertiser's permission a listing must link to the original "
                    + "advertisement, which is the only way a reader can make contact.");
        }
    }

    private String normaliseListingType(String value) {
        String upper = value == null ? "" : value.trim().toUpperCase();
        if (!VALID_LISTING_TYPES.contains(upper)) {
            throw ApiException.badRequest("INVALID_LISTING_TYPE",
                    "Listing type must be one of: " + String.join(", ", VALID_LISTING_TYPES));
        }
        return upper;
    }

    private String normaliseBills(String value) {
        String upper = value == null ? "" : value.trim().toUpperCase();
        if (!VALID_BILLS.contains(upper)) {
            throw ApiException.badRequest("INVALID_BILLS",
                    "Bills must be one of: " + String.join(", ", VALID_BILLS));
        }
        return upper;
    }

    private String normaliseConsent(String value) {
        String upper = value == null ? "" : value.trim().toUpperCase();
        if (!VALID_CONSENT.contains(upper)) {
            throw ApiException.badRequest("INVALID_CONSENT",
                    "Consent must be one of: " + String.join(", ", VALID_CONSENT));
        }
        return upper;
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}