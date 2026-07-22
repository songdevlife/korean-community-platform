package com.dak.backend.repository;

import com.dak.backend.domain.UpdateSourceReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UpdateSourceReferenceRepository extends JpaRepository<UpdateSourceReference, UUID> {
}