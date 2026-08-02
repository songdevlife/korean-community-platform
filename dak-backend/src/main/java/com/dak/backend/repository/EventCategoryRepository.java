package com.dak.backend.repository;

import com.dak.backend.domain.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {

    Optional<EventCategory> findBySlug(String slug);

    List<EventCategory> findAllByOrderByNameAsc();
}