package com.dak.backend.common;

import java.util.List;

/**
 * Error body used inside ApiResponse.error.
 * code: UPPER_SNAKE_CASE per 05_API_Specification_DAK.docx section 2.6 / 2.8.
 */
public record ApiError(String code, String message, List<FieldError> details, String requestId) {

    public record FieldError(String field, String message) {}

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, null);
    }

    public static ApiError of(String code, String message, List<FieldError> details) {
        return new ApiError(code, message, details, null);
    }
}
