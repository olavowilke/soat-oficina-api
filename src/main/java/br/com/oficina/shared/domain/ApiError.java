package br.com.oficina.shared.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        List<FieldError> fields
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, LocalDateTime.now(), null);
    }

    public static ApiError of(int status, String error, String message, List<FieldError> fields) {
        return new ApiError(status, error, message, LocalDateTime.now(), fields);
    }

    public record FieldError(String field, String message) {}
}
