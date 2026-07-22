package com.dak.backend.repository;

import com.dak.backend.domain.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, UUID> {

    Optional<BusinessCategory> findBySlug(String slug);
}