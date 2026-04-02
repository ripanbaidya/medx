package com.medx.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FallbackResponse(
        String code,
        String message,
        int status,
        String service,
        String correlationId,
        String path,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {
    public static FallbackResponse of(String code, String message, String service,
                                      String correlationId, String path) {
        return new FallbackResponse(code, message, HttpStatus.SERVICE_UNAVAILABLE.value(),
                service, correlationId, path, Instant.now());
    }
}
