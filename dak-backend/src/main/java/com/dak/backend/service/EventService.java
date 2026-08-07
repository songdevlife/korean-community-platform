package com.dak.backend.service;

import com.dak.backend.domain.Event;
import com.dak.backend.domain.EventImage;
import com.dak.backend.dto.EventCategoryResponse;
import com.dak.backend.dto.EventImageResponse;
import com.dak.backend.dto.EventResponse;
import com.dak.backend.dto.EventSummaryResponse;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.EventCategoryRepository;
import com.dak.backend.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;

    public EventService(EventRepository eventRepository,
                         EventCategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Published events that have not finished, soonest first.
     *
     * The date filter is the whole point of this listing: an event that has
     * happened is not stale content, it is wrong content, and a reader who
     * turns up to something that finished last week has been misled by the
     * site rather than merely underserved by it.
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> listUpcoming(String categorySlug, String keyword,
                                                    int page, int pageSize) {
        UUID categoryId = null;

        if (categorySlug != null && !categorySlug.isBlank()) {
            categoryId = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> ApiException.badRequest(
                            "INVALID_CATEGORY", "Category not found."))
                    .getId();
        }

        // Blank is the same as absent. The search page sends the parameter on
        // every request, empty when nobody has typed anything, and an empty
        // LIKE pattern would match everything rather than nothing - which is
        // the right answer here, but only by accident.
        String trimmed = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return eventRepository
                .findUpcoming(OffsetDateTime.now(), categoryId, trimmed,
                        PageRequest.of(page, Math.min(pageSize, 100)))
                .map(this::toSummary);
    }

    /**
     * Detail, without the date filter.
     *
     * A link shared before an event should still resolve afterwards. The
     * response says so through hasPassed rather than by returning 404, since a
     * dead link tells a reader nothing about what they missed.
     */
    @Transactional(readOnly = true)
    public EventResponse getByIdentifier(String identifier) {
        Event event = resolve(identifier)
                .orElseThrow(() -> ApiException.notFound("Event not found."));
        return toDetail(event);
    }

    /**
     * Accepts either form of address.
     *
     * Events were addressed by UUID until V25 and those links are already in
     * KakaoTalk threads and search results, so both resolve. The response
     * carries the canonical slug and the detail page redirects to it, which
     * keeps one address per event without breaking the other.
     *
     * A slug can never look like a UUID: slugify strips everything but
     * lowercase letters, digits and hyphens, and a bare 36-character hyphenated
     * hex string is not a shape any title reduces to.
     */
    private Optional<Event> resolve(String identifier) {
        try {
            return eventRepository.findByIdAndStatus(UUID.fromString(identifier), "PUBLISHED");
        } catch (IllegalArgumentException notAUuid) {
            return eventRepository.findBySlugAndStatus(identifier, "PUBLISHED");
        }
    }

    @Transactional(readOnly = true)
    public List<EventCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new EventCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    EventSummaryResponse toSummary(Event e) {
        return new EventSummaryResponse(
                e.getId(), e.getSlug(), e.getTitle(), e.getStartsAt(), e.getEndsAt(),
                e.getVenueName(), e.isFree(), e.getPriceNote(),
                thumbnailOf(e), toCategory(e));
    }

    EventResponse toDetail(Event e) {
        // Against endsAt where there is one, so a festival running until Sunday
        // does not read as finished on Saturday.
        OffsetDateTime finishesAt = e.getEndsAt() != null ? e.getEndsAt() : e.getStartsAt();

        return new EventResponse(
                e.getId(), e.getSlug(), e.getTitle(), e.getDescription(),
                e.getStartsAt(), e.getEndsAt(),
                e.getVenueName(), e.getVenueAddress(),
                e.isFree(), e.getPriceNote(),
                e.getOrganiser(), e.getOrganiserContact(), e.getSourceUrl(),
                toImages(e), toCategory(e), e.getStatus(),
                finishesAt.isBefore(OffsetDateTime.now()),
                e.getCreatedAt());
    }

    /**
     * The lowest display_order, which is what a card shows.
     *
     * Sending the whole list on a twenty-item page would inflate the response
     * to fill one slot per card. Ordering comes from @OrderBy on the entity,
     * so this is the first element rather than a search for a minimum.
     */
    private String thumbnailOf(Event e) {
        List<EventImage> images = e.getImages();
        return (images == null || images.isEmpty()) ? null : images.get(0).getImageUrl();
    }

    private List<EventImageResponse> toImages(Event e) {
        if (e.getImages() == null) return List.of();
        return e.getImages().stream()
                .map(i -> new EventImageResponse(
                        i.getId(), i.getImageUrl(), i.getAltText(), i.getDisplayOrder()))
                .toList();
    }

    private EventCategoryResponse toCategory(Event e) {
        return e.getCategory() == null ? null : new EventCategoryResponse(
                e.getCategory().getId(), e.getCategory().getName(), e.getCategory().getSlug());
    }
}