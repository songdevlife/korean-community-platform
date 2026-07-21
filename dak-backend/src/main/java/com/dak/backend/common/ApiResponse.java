package com.dak.backend.common;

/**
 * Standard API response envelope.
 * Mirrors 05_API_Specification_DAK.docx section 11.2 (Response Models) / 2.6 (Error Response Format).
 *
 * Success: { "success": true, "data": {...} }
 * Error:   { "success": false, "error": {...} }
 */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
