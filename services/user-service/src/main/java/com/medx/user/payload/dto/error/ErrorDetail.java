package com.medx.user.payload.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.medx.commons.enums.ErrorType;
import com.medx.user.enums.ErrorCode;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type", "code", "message", "status", "service",
        "correlationId", "path", "errors", "timestamp"
})
public record ErrorDetail(
        String type,
        String code,
        String message,
        int status,
        String service,
        String correlationId,
        String path,
        List<FieldError> errors,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {

    public static class Builder {
        private ErrorType type;
        private String code;
        private String message;
        private String path;
        private String service;
        private String correlationId;
        private List<FieldError> errors;

        /**
         * Sets the error code and automatically derives type and default message.
         *
         * @param errorCode The predefined error code enum
         * @return This builder instance for method chaining
         */
        public Builder code(ErrorCode errorCode) {
            this.type = errorCode.getType();
            this.code = errorCode.name();
            this.message = errorCode.getDefaultMessage();
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder errors(List<FieldError> errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Builds the ErrorDetail instance with validation.
         * Automatically sets correlation ID from thread context and current timestamp.
         *
         * @return A new ErrorDetail instance
         * @throws IllegalStateException if required fields (type, code) are not set
         */
        public ErrorDetail build() {
            if (type == null || code == null) {
                throw new IllegalStateException(
                        "ErrorDetail requires a code — call .code(ErrorCode) before .build()");
            }
            return new ErrorDetail(
                    type.name(),
                    code,
                    message,
                    type.getStatusCode(),
                    service,
                    correlationId,
                    path,
                    errors,
                    Instant.now()
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}