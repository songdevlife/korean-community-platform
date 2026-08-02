package com.dak.backend.service;

import com.dak.backend.domain.Event;
import com.dak.backend.domain.EventCategory;
import com.dak.backend.dto.CreateEventRequest;
import com.dak.backend.dto.EventResponse;
import com.dak.backend.dto.UpdateEventRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.EventCategoryRepository;
import com.dak.backend.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminEventService {

    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final EventService eventService;

    public AdminEventService(EventRepository eventRepository,
                              EventCategoryRepository categoryRepository,
                              EventService eventService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.eventService = eventService;
    }

    /**
     * Every event regardless of status or date, newest first.
     *
     * Unlike the public listing this does not hide what has passed: the queue
     * is where a past event is archived or corrected, so it has to be visible
     * there to be dealt with.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> listAll(String status, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, Math.min(pageSize, 100));

        Page<Event> events = (status != null && !status.isBlank())
                ? eventRepository.findByStatus(status, pageable)
                : eventRepository.findAll(pageable);

        return events.map(eventService::toDetail);
    }

    /**
     * One event regardless of status.
     *
     * The public lookup returns only PUBLISHED, which is exactly the wrong
     * behaviour for an edit screen - a draft is the thing most likely to be
     * being edited.
     */
    @Transactional(readOnly = true)
    public EventResponse getById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found."));
        return eventService.toDetail(event);
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Event event = Event.createNew(request.title().trim(), request.startsAt());

        applyOptionalFields(event, request.description(), request.endsAt(),
                request.venueName(), request.venueAddress(),
                request.isFree(), request.priceNote(),
                request.organiser(), request.organiserContact(), request.sourceUrl());

        if (request.categoryId() != null) {
            event.setCategory(findCategory(request.categoryId()));
        }

        validateDates(event);
        eventRepository.save(event);

        return eventService.toDetail(event);
    }

    /**
     * Null means "leave alone" for every field.
     *
     * An edit usually corrects one thing, and requiring the whole record back
     * means a caller that does not know about a field can blank it without
     * meaning to.
     */
    @Transactional
    public EventResponse update(UUID eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found."));

        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw ApiException.badRequest("INVALID_TITLE", "Title cannot be empty.");
            }
            event.setTitle(title);
        }

        if (request.description() != null) event.setDescription(blankToNull(request.description()));
        if (request.startsAt() != null) event.setStartsAt(request.startsAt());
        if (request.endsAt() != null) event.setEndsAt(request.endsAt());
        if (request.venueName() != null) event.setVenueName(blankToNull(request.venueName()));
        if (request.venueAddress() != null) event.setVenueAddress(blankToNull(request.venueAddress()));
        if (request.isFree() != null) event.setFree(request.isFree());
        if (request.priceNote() != null) event.setPriceNote(blankToNull(request.priceNote()));
        if (request.organiser() != null) event.setOrganiser(blankToNull(request.organiser()));
        if (request.organiserContact() != null) event.setOrganiserContact(blankToNull(request.organiserContact()));
        if (request.sourceUrl() != null) event.setSourceUrl(blankToNull(request.sourceUrl()));

        if (request.categoryId() != null) {
            event.setCategory(findCategory(request.categoryId()));
        }

        if (request.status() != null) {
            String status = request.status().trim().toUpperCase();
            if (!VALID_STATUSES.contains(status)) {
                throw ApiException.badRequest("INVALID_STATUS",
                        "Status must be one of: " + String.join(", ", VALID_STATUSES));
            }
            // Publishing without a category would put an event in a list no
            // filter reaches, which is close enough to unpublished to be worth
            // refusing. Same guard Australia Updates carries.
            if ("PUBLISHED".equals(status) && event.getCategory() == null) {
                throw ApiException.badRequest("MISSING_CATEGORY",
                        "An event must have a category before it can be published.");
            }
            event.setStatus(status);
        }

        validateDates(event);
        event.setUpdatedAt(OffsetDateTime.now());

        return eventService.toDetail(event);
    }

    @Transactional
    public void delete(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found."));
        eventRepository.delete(event);
    }

    private void applyOptionalFields(Event event, String description, OffsetDateTime endsAt,
                                      String venueName, String venueAddress,
                                      boolean isFree, String priceNote,
                                      String organiser, String organiserContact, String sourceUrl) {
        event.setDescription(blankToNull(description));
        event.setEndsAt(endsAt);
        event.setVenueName(blankToNull(venueName));
        event.setVenueAddress(blankToNull(venueAddress));
        event.setFree(isFree);
        event.setPriceNote(blankToNull(priceNote));
        event.setOrganiser(blankToNull(organiser));
        event.setOrganiserContact(blankToNull(organiserContact));
        event.setSourceUrl(blankToNull(sourceUrl));
    }

    /**
     * An end before a start is a transcription slip rather than a decision -
     * usually a date typed for the wrong day. Caught here because the listing
     * query treats endsAt as authoritative, so a backwards pair would hide the
     * event the moment it was published.
     */
    private void validateDates(Event event) {
        if (event.getEndsAt() != null && event.getEndsAt().isBefore(event.getStartsAt())) {
            throw ApiException.badRequest("INVALID_DATES",
                    "The end time cannot be before the start time.");
        }
    }

    private EventCategory findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_CATEGORY", "Category not found."));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}