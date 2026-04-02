package com.medx.user.exception.handler;

import com.medx.user.enums.ErrorCode;
import com.medx.user.exception.BaseException;
import com.medx.user.payload.dto.error.ErrorDetail;
import com.medx.user.payload.dto.error.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * Service name injected from each service's application.yml
     * via app.service-name property.
     * Every service that uses this handler just needs to define
     * app.service-name in its config — nothing else.
     */
    @Value("${app.service-name}")
    private String serviceName;

    /*
     * Handles all custom exceptions extending BaseException.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorDetail> handleBaseException(BaseException ex,
                                                           HttpServletRequest request
    ) {
        var error = ErrorDetail.builder()
            .code(ex.getErrorCode())
            .message(ex.getResolvedMessage())
            .service(serviceName)
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(ex.getErrorCode().getType().getStatusCode())
            .body(error);
    }

    /*
     * Handles @Valid failures on @RequestBody.
     * Collects ALL field errors — not just the first one.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetail> handleValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request
    ) {
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new FieldError(
                fe.getField(),
                fe.getDefaultMessage()
            ))
            .toList();

        var error = ErrorDetail.builder()
            .code(ErrorCode.VALIDATION_FAILED)
            .service(serviceName)
            .path(request.getRequestURI())
            .errors(fieldErrors)
            .build();

        return ResponseEntity.status(error.status()).body(error);
    }

    /*
     * Handles @Validated failures on @RequestParam and @PathVariable.
     * Strips method prefix from property path for clean field names.
     * e.g. "registerUser.email" becomes "email"
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                 HttpServletRequest request
    ) {
        List<FieldError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(cv -> {
                String field = cv.getPropertyPath().toString();
                // Strip method prefix: "createUser.email" → "email"
                if (field.contains(".")) {
                    field = field.substring(field.lastIndexOf('.') + 1);
                }
                return new FieldError(field, cv.getMessage());
            })
            .toList();

        var error = ErrorDetail.builder()
            .code(ErrorCode.VALIDATION_FAILED)
            .service(serviceName)
            .path(request.getRequestURI())
            .errors(fieldErrors)
            .build();

        return ResponseEntity.status(error.status()).body(error);
    }

    /*
     * Handles type mismatch on path variables or request params.
     * e.g. GET /users/abc when UUID expected → clear message returned.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                          HttpServletRequest request
    ) {
        String expectedType = ex.getRequiredType() != null
            ? ex.getRequiredType().getSimpleName()
            : "unknown";

        String detail = String.format("Parameter '%s' must be of type %s", ex.getName(), expectedType);

        var error = ErrorDetail.builder()
            .code(ErrorCode.VALIDATION_FAILED)
            .message(detail)
            .service(serviceName)
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(error.status()).body(error);
    }

    /*
     * Handles IllegalArgumentException and IllegalStateException.
     * Typically thrown by internal logic when invalid state is detected.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorDetail> handleIllegalArgument(RuntimeException ex,
                                                             HttpServletRequest request
    ) {
        var error = ErrorDetail.builder()
            .code(ErrorCode.VALIDATION_FAILED)
            .message(ex.getMessage())
            .service(serviceName)
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(error.status()).body(error);
    }

    /*
     * Handles database-level failures.
     * Logs root cause for debugging but never exposes it to client.
     * Always returns generic INTERNAL_SERVER_ERROR to client.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorDetail> handleDataAccess(DataAccessException ex,
                                                        HttpServletRequest request
    ) {
        var error = ErrorDetail.builder()
            .code(ErrorCode.INTERNAL_SERVER_ERROR)
            .service(serviceName)
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(error.status()).body(error);
    }

    /*
     * Catch-all fallback — no stack traces ever reach the client.
     * All unexpected failures logged at ERROR for investigation.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> handleUnexpected(Exception ex,
                                                        HttpServletRequest request
    ) {
        var error = ErrorDetail.builder()
            .code(ErrorCode.INTERNAL_SERVER_ERROR)
            .service(serviceName)
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(error.status()).body(error);
    }
}