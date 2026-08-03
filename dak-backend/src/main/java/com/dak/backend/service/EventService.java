package com.dak.backend.service;

import com.dak.backend.domain.Event;
import com.dak.backend.dto.EventCategoryResponse;
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
    public EventResponse getById(UUID id) {
        Event event = eventRepository.findByIdAndStatus(id, "PUBLISHED")
                .orElseThrow(() -> ApiException.notFound("Event not found."));
        return toDetail(event);
    }

    @Transactional(readOnly = true)
    public List<EventCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new EventCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    EventSummaryResponse toSummary(Event e) {
        return new EventSummaryResponse(
                e.getId(), e.getTitle(), e.getStartsAt(), e.getEndsAt(),
                e.getVenueName(), e.isFree(), e.getPriceNote(),
                toCategory(e));
    }

    EventResponse toDetail(Event e) {
        // Against endsAt where there is one, so a festival running until Sunday
        // does not read as finished on Saturday.
        OffsetDateTime finishesAt = e.getEndsAt() != null ? e.getEndsAt() : e.getStartsAt();

        return new EventResponse(
                e.getId(), e.getTitle(), e.getDescription(),
                e.getStartsAt(), e.getEndsAt(),
                e.getVenueName(), e.getVenueAddress(),
                e.isFree(), e.getPriceNote(),
                e.getOrganiser(), e.getOrganiserContact(), e.getSourceUrl(),
                toCategory(e), e.getStatus(),
                finishesAt.isBefore(OffsetDateTime.now()),
                e.getCreatedAt());
    }

    private EventCategoryResponse toCategory(Event e) {
        return e.getCategory() == null ? null : new EventCategoryResponse(
                e.getCategory().getId(), e.getCategory().getName(), e.getCategory().getSlug());
    }
}