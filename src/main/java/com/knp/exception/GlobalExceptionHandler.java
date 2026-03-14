package com.knp.exception;

import com.knp.model.dto.ApiResponse;
import com.knp.service.MessageService;
import com.knp.service.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    /**
     * Handle ResourceNotFoundException - 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.resource.not.found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RESOURCE_NOT_FOUND", message));
    }

    /**
     * Handle NoSuchElementException - 404 Not Found
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuchElementException(
            NoSuchElementException ex) {
        log.error("Element not found: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.resource.not.found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", message));
    }

    /**
     * Handle AccountLockedException - 403 Forbidden
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCOUNT_LOCKED", ex.getMessage()));
    }

    /**
     * Handle BadRequestException - 400 Bad Request
     */
    @ExceptionHandler({
            BadRequestException.class,
            TenantExpiredException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception ex) {
        log.error("Bad request: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.bad.request");
        if (ex instanceof TenantExpiredException tee) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("TENANT_EXPIRED", message));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("BAD_REQUEST", message));
    }

    /**
     * Handle IllegalArgumentException - 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        log.error("Illegal argument: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.invalid.argument");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_ARGUMENT", message));
    }

    /**
     * Handle DeviceConflictException - 409 Conflict (single-device login)
     */
    @ExceptionHandler(DeviceConflictException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDeviceConflictException(DeviceConflictException ex) {
        SessionInfo s = ex.getExistingSession();
        Map<String, Object> data = Map.of(
                "ipAddress", s.ipAddress() != null ? s.ipAddress() : "",
                "userAgent", s.userAgent() != null ? s.userAgent() : "",
                "loginAt", s.loginAt().toString()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .error("DEVICE_CONFLICT")
                        .message(messageService.getMessage("error.session.device.conflict"))
                        .data(data)
                        .build());
    }

    /**
     * Handle DuplicateResourceException - 409 Conflict
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex,
            WebRequest request) {
        log.error("Duplicate resource: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.duplicate.resource");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_RESOURCE", message));
    }

    /**
     * Handle UnauthorizedException - 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
            UnauthorizedException ex,
            WebRequest request) {
        log.error("Unauthorized: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", message));
    }

    /**
     * Handle BadCredentialsException - 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_CREDENTIALS", messageService.getMessage("error.invalid.credentials")));
    }

    /**
     * Handle IllegalStateException - 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {
        log.error("Illegal state: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.illegal.state");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("ILLEGAL_STATE", message));
    }

    /**
     * Handle BusinessException - 422 Unprocessable Entity
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex,
            WebRequest request) {
        log.error("Business rule violation: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.business.rule.violation");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", message));
    }

    /**
     * Handle AccessDeniedException - 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESS_DENIED", messageService.getMessage("error.access.denied")));
    }

    /**
     * Handle ForbiddenException - 403 Forbidden
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(
            ForbiddenException ex,
            WebRequest request) {
        log.error("Forbidden: {}", ex.getMessage());
        String message = ex.getMessage() != null ? messageService.getMessage(ex.getMessage()) : messageService.getMessage("error.access.denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", message));
    }

    /**
     * Handle validation errors - 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message(messageService.getMessage("error.validation.failed"))
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle type mismatch errors - 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        log.error("Type mismatch: {}", ex.getMessage());
        String typeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String error = messageService.getMessage("error.type.mismatch", ex.getName(), typeName);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("TYPE_MISMATCH", error));
    }

    /**
     * Handle generic RuntimeException - 500 Internal Server Error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        log.error("Runtime exception: ", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : messageService.getMessage("error.unexpected");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", message));
    }

    /**
     * Handle all other exceptions - 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", messageService.getMessage("error.internal.server")));
    }
}

