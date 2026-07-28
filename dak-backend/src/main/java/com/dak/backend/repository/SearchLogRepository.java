package com.dak.backend.repository;

import com.dak.backend.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Write-only for now. Analysis runs as SQL against the table rather than
 * through the API — there is no admin screen for this, and building one before
 * there is data to look at would be guessing at the questions.
 */
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {
}