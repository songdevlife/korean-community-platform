package com.dak.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for known/handled API errors.
 * code should be UPPER_SNAKE_CASE, e.g. RESOURCE_NOT_FOUND, EMAIL_ALREADY_EXISTS.
 * See 05_API_Specification_DAK.docx section 2.6 for the canonical error code list.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
