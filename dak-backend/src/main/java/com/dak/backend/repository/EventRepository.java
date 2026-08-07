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
     * Detail lookup. Deliberately does not filter on the date: a link shared
     * before an event should still resolve afterwards rather than 404, and the
     * page can say it has passed.
     */
    Optional<Event> findByIdAndStatus(UUID id, String status);

    /**
     * The same lookup by the address readers actually use. Both forms resolve
     * so that UUID links shared before V24 do not break; the detail page
     * redirects one to the other.
     */
    Optional<Event> findBySlugAndStatus(String slug, String status);

    Page<Event> findByStatus(String status, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    /**
     * Published events that have not finished, for the sitemap.
     *
     * Separate from findUpcoming, which takes a category filter and a page
     * the sitemap has no use for. Past events are excluded on purpose: their
     * pages still resolve so a shared link does not break, but a search
     * result leading to something that happened last month is worse than no
     * search result at all.
     */
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' "
         + "AND COALESCE(e.endsAt, e.startsAt) >= :now "
         + "ORDER BY e.startsAt ASC")
    List<Event> findUpcomingForSitemap(OffsetDateTime now, Pageable pageable);

    /**
     * Published events that have not finished, for the sitemap.
     *
     * Separate from findUpcoming because that one takes a category filter and
     * an ordering the sitemap has no use for. Past events are excluded on
     * purpose: their pages still resolve, so a shared link does not break, but
     * a search result leading to something that happened last month is worse
     * than no search result at all.
     */
    /**
     * Title, description, venue and organiser, case-insensitively.
     *
     * The searchable text is concatenated before matching rather than tested
     * field by field. Four ORs inside one AND is a precedence trap in JPQL,
     * and every field but the title is nullable — a null in a LIKE yields
     * null rather than false, which propagates through the OR chain and
     * discards rows that should have matched. COALESCE to empty string makes
     * a missing field contribute nothing instead of poisoning the test.
     *
     * Deliberately not the source URL: a reader searching for a word does not
     * mean a URL that happens to contain it.
     */
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' "
         + "AND COALESCE(e.endsAt, e.startsAt) >= :now "
         + "AND (:categoryId IS NULL OR e.category.id = :categoryId) "
         + "AND (:keyword IS NULL OR "
         + "     LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR "
         + "     LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR "
         + "     LOWER(COALESCE(e.venueName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR "
         + "     LOWER(COALESCE(e.organiser, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) "
         + "ORDER BY e.startsAt ASC")
    Page<Event> findUpcoming(OffsetDateTime now, UUID categoryId, String keyword, Pageable pageable);
}