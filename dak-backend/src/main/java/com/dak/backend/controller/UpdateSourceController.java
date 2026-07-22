package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.UpdateSource;
import com.dak.backend.dto.CreateUpdateSourceRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.UpdateSourceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/update-sources")
public class UpdateSourceController {

    private final UpdateSourceRepository updateSourceRepository;

    public UpdateSourceController(UpdateSourceRepository updateSourceRepository) {
        this.updateSourceRepository = updateSourceRepository;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getSources() {
        List<Map<String, Object>> sources = updateSourceRepository.findAll().stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName(), "sourceType", s.getSourceType()))
                .toList();
        return ApiResponse.ok(sources);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> createSource(@Valid @RequestBody CreateUpdateSourceRequest request) {
        if (updateSourceRepository.findAll().stream().anyMatch(s -> s.getName().equalsIgnoreCase(request.name()))) {
            throw ApiException.conflict("SOURCE_ALREADY_EXISTS", "A source with this name already exists.");
        }

        UpdateSource source = UpdateSource.createNew(request.name(), request.sourceType());

        updateSourceRepository.save(source);

        return ApiResponse.ok(Map.of("id", source.getId(), "name", source.getName(), "sourceType", source.getSourceType()));
    }
}