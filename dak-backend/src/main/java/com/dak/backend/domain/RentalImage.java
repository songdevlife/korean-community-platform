package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A photograph of a rental, in display order. Matches EventImage so the
 * shared Gallery component works without change.
 *
 * Only ever populated where the advertiser supplied the image or agreed to
 * its use - the facts of a listing are not copyright, the photographs are.
 */
@Entity
@Table(name = "rental_images")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalImage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    public static RentalImage createNew(Rental rental, String imageUrl,
                                         String altText, int displayOrder) {
        RentalImage image = new RentalImage();
        image.rental = rental;
        image.imageUrl = imageUrl;
        image.altText = altText;
        image.displayOrder = displayOrder;
        return image;
    }
}