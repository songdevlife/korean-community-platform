package com.dak.backend.repository;

import com.dak.backend.domain.GuideCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuideCategoryRepository extends JpaRepository<GuideCategory, UUID> {
}