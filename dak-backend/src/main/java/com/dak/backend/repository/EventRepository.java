package com.dak.backend.repository;

import com.dak.backend.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * The public listing: published, not yet finished, soonest first.
     *
     * Compares against endsAt where there is one, so a festival running until
     * Sunday stays listed on Saturday rather than disappearing the moment it
     * begins.
     */
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' "
         + "AND COALESCE(e.endsAt, e.startsAt) >= :now "
         + "AND (:categoryId IS NULL OR e.category.id = :categoryId) "
         + "ORDER BY e.startsAt ASC")
    Page<Event> findUpcoming(OffsetDateTime now, UUID categoryId, Pageable pageable);

    /**
     * Detail lookup. Deliberately does not filter on the date: a link shared
     * before an event should still resolve afterwards rather than 404, and the
     * page can say it has passed.
     */
    Optional<Event> findByIdAndStatus(UUID id, String status);

    Page<Event> findByStatus(String status, Pageable pageable);

    /**
     * Published events that have not finished, for the sitemap.
     *
     * Separate from findUpcoming because that one takes a category filter and
     * an ordering the sitemap has no use for. Past events are excluded on
     * purpose: their pages still resolve, so a shared link does not break, but
     * a search result leading to something that happened last month is worse
     * than no search result at all.
     */
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' "
         + "AND COALESCE(e.endsAt, e.startsAt) >= :now")
    List<Event> findUpcomingForSitemap(OffsetDateTime now, Pageable pageable);
}