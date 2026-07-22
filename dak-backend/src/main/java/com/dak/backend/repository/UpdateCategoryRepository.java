package com.dak.backend.repository;

import com.dak.backend.domain.UpdateCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UpdateCategoryRepository extends JpaRepository<UpdateCategory, UUID> {

    Optional<UpdateCategory> findBySlug(String slug);
}