package com.dak.backend.repository;

import com.dak.backend.domain.UpdateSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UpdateSourceRepository extends JpaRepository<UpdateSource, UUID> {

    Optional<UpdateSource> findByNameIgnoreCase(String name);
}