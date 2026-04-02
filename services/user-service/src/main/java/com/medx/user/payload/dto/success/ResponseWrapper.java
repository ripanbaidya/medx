package com.medx.user.payload.dto.success;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.medx.user.context.CorrelationContext;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "success",
        "status",
        "message",
        "data",
        "service",
        "correlationId",
        "path",
        "timestamp"
})
public record ResponseWrapper<T>(
        boolean success,
        int status,
        String message,
        T data,
        String service,
        String correlationId,
        String path,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {

    public static <T> ResponseWrapper<T> ok(String message, String service, String path) {
        return build(HttpStatus.OK, message, null, service, path, null);
    }

    public static <T> ResponseWrapper<T> ok(String message, T data, String service, String path) {
        return build(HttpStatus.OK, message, data, service, path, null);
    }

    public static <T> ResponseWrapper<T> created(String message, T data, String service, String path) {
        return build(HttpStatus.CREATED, message, data, service, path, null);
    }

    public static <T> ResponseWrapper<T> accepted(String message, T data, String service, String path) {
        return build(HttpStatus.ACCEPTED, message, data, service, path, null);
    }

    public static <T> ResponseWrapper<T> of(HttpStatus status, String message, T data,
                                            String service, String path) {
        return build(status, message, data, service, path, null);
    }

    public static <T> ResponseWrapper<T> ok(String message, T data, String service,
                                            String path, String correlationId) {
        return build(HttpStatus.OK, message, data, service, path, correlationId);
    }

    public static <T> ResponseWrapper<T> created(String message, T data, String service,
                                                 String path, String correlationId) {
        return build(HttpStatus.CREATED, message, data, service, path, correlationId);
    }

    private static <T> ResponseWrapper<T> build(HttpStatus status, String message, T data,
                                                String service, String path, String explicitCorrelationId) {
        final String correlationId = explicitCorrelationId != null
                ? explicitCorrelationId
                : CorrelationContext.get();

        return new ResponseWrapper<>(
                true,
                status.value(),
                message,
                data,
                service,
                correlationId,
                path,
                Instant.now()
        );
    }
}