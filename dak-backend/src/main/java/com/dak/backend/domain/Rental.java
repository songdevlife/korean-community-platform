package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A room, share house, whole property or lease transfer on offer.
 *
 * The field that shapes everything else is consentStatus. A listing recorded
 * from someone else's advertisement carries facts and a link back; one whose
 * advertiser has agreed may carry their contact details and their photographs.
 * The distinction is enforced in the service, in a check constraint, and in
 * content-policy.md section 17 - three places, because getting it wrong
 * publishes a private person's phone number without their knowing.
 */
@Entity
@Table(name = "rentals")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rental {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Suburb rather than a street address. A room advertisement rarely gives
     * one, and printing the address of a house people live in would tell a
     * reader nothing they need and expose the occupants.
     */
    @Column(nullable = false, length = 120)
    private String suburb;

    @Column(name = "listing_type", nullable = false, length = 30)
    private String listingType;

    /** Comma-separated: SINGLE, SHARED, WHOLE. A property can be offered as more than one. */
    @Column(name = "room_types", length = 120)
    private String roomTypes;

    @Column(name = "rent_min", nullable = false)
    private Integer rentMin;

    /** Set only where the advertisement gives a range, usually because one listing covers several rooms. */
    @Column(name = "rent_max")
    private Integer rentMax;

    /**
     * Weeks, as advertisements state it and as the legal cap is expressed:
     * four weeks at or below $800 a week, six above it.
     */
    @Column(name = "bond_weeks", precision = 3, scale = 1)
    private BigDecimal bondWeeks;

    @Column(name = "bills_included", nullable = false, length = 20)
    private String billsIncluded = "UNKNOWN";

    /** Free text because what "$50/week" covers differs listing to listing. */
    @Column(name = "bills_note", length = 300)
    private String billsNote;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "min_term_months")
    private Integer minTermMonths;

    private Boolean furnished;

    /**
     * How many rooms are let in the property, which decides the tenancy's
     * legal character: two or more is a rooming house under Part 7 of the
     * Residential Tenancies Act, one leaves the occupant outside the Act.
     * Five or more requires the operator to register with CBS.
     *
     * Rarely stated in an advertisement, so null is common and means unknown
     * rather than none. The detail page says which of the three a reader is
     * looking at, or that it could not be determined - which is the thing no
     * other listing site tells them.
     */
    @Column(name = "rooms_let")
    private Integer roomsLet;

    @Column(name = "gender_preference", length = 20)
    private String genderPreference;

    @Column(name = "couples_allowed")
    private Boolean couplesAllowed;

    @Column(name = "pets_allowed")
    private Boolean petsAllowed;

    @Column(name = "smoking_allowed")
    private Boolean smokingAllowed;

    @Column(name = "inspection_note", length = 300)
    private String inspectionNote;

    /** NONE, LINK_ONLY or FULL. See the class comment. */
    @Column(name = "consent_status", nullable = false, length = 20)
    private String consentStatus = "NONE";

    /** When and through what channel permission was given. Administrator-only. */
    @Column(name = "consent_note", columnDefinition = "TEXT")
    private String consentNote;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /** Permitted only where consentStatus is FULL. */
    @Column(length = 300)
    private String contact;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    /**
     * A listing for a property already let costs a reader a message and DAK
     * its credibility, so expiry is set on publication rather than left to
     * someone remembering to archive it.
     */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /**
     * When an external listing was last verified against its source.
     *
     * Only meaningful where consentStatus is NOT FULL: those are the listings
     * DAK recorded from someone else's advertisement, with no way of learning
     * that the room has gone. Printed on the page because a reader deciding
     * whether to send a message deserves to know whether the information is
     * from this morning or from a fortnight ago.
     *
     * Not updatedAt, which moves whenever anything is edited and would claim a
     * check that never happened.
     */
    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "rental", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<RentalImage> images = new ArrayList<>();

    public static Rental createNew(String title, String suburb,
                                    String listingType, Integer rentMin) {
        OffsetDateTime now = OffsetDateTime.now();
        Rental rental = new Rental();
        rental.title = title;
        rental.suburb = suburb;
        rental.listingType = listingType;
        rental.rentMin = rentMin;
        rental.status = "DRAFT";
        rental.createdAt = now;
        rental.updatedAt = now;
        return rental;
    }
}