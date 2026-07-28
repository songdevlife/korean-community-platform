package com.dak.backend.service;

import com.dak.backend.domain.SearchLog;
import com.dak.backend.dto.CreateSearchLogRequest;
import com.dak.backend.repository.SearchLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchLogService {

    // Matches the column width. Anything longer is a paste rather than a
    // search, and the tail of it carries no information worth storing.
    private static final int MAX_TERM_LENGTH = 200;

    private final SearchLogRepository searchLogRepository;

    public SearchLogService(SearchLogRepository searchLogRepository) {
        this.searchLogRepository = searchLogRepository;
    }

    @Transactional
    public void record(CreateSearchLogRequest request) {
        String term = request.searchTerm() == null ? "" : request.searchTerm().trim();
        if (term.isEmpty()) {
            return;
        }
        if (term.length() > MAX_TERM_LENGTH) {
            term = term.substring(0, MAX_TERM_LENGTH);
        }

        int count = request.resultCount() == null || request.resultCount() < 0
                ? 0
                : request.resultCount();

        searchLogRepository.save(SearchLog.createNew(term, count));
    }
}