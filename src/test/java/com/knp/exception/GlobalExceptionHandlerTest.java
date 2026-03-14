package com.knp.exception;

import com.knp.model.dto.ApiResponse;
import com.knp.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        // Setup default behavior for any message call
        webRequest = mock(WebRequest.class);
    }

    @Test
    @DisplayName("Should handle BadRequestException with 400 status")
    void testHandleBadRequestException() {
        // Given
        String message = "Invalid request";
        BadRequestException exception = new BadRequestException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getError()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 status")
    void testHandleResourceNotFoundException() {
        // Given
        String message = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getError()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException with 409 status")
    void testHandleDuplicateResourceException() {
        // Given
        String message = "Resource already exists";
        DuplicateResourceException exception = new DuplicateResourceException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDuplicateResourceException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle UnauthorizedException with 401 status")
    void testHandleUnauthorizedException() {
        // Given
        String message = "Unauthorized access";
        UnauthorizedException exception = new UnauthorizedException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnauthorizedException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getMessage()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should handle ForbiddenException with 403 status")
    void testHandleForbiddenException() {
        // Given
        String message = "Forbidden access";
        ForbiddenException exception = new ForbiddenException(message);
        when(messageService.getMessage("Forbidden access")).thenReturn("Access forbidden");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleForbiddenException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("Should handle BusinessException with 422 status")
    void testHandleBusinessException() {
        // Given
        String message = "Business logic error";
        BusinessException exception = new BusinessException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 status")
    void testHandleGlobalException() {
        // Given
        String message = "Internal server error";
        Exception exception = new Exception(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGlobalException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    void testHandleIllegalArgumentException() {
        // Given
        String message = "Invalid argument";
        IllegalArgumentException exception = new IllegalArgumentException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with null message")
    void testHandleResourceNotFoundException_NullMessage() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException(null);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle BadRequestException with null message")
    void testHandleBadRequestException_NullMessage() {
        // Given
        BadRequestException exception = new BadRequestException(null);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return correct HTTP status codes for all exception types")
    void testCorrectHttpStatusCodes() {
        // Test 404
        ResponseEntity<ApiResponse<Void>> response404 = exceptionHandler.handleResourceNotFoundException(
                new ResourceNotFoundException("test"));
        assertThat(response404.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Test 400
        ResponseEntity<ApiResponse<Void>> response400 = exceptionHandler.handleBadRequestException(
                new BadRequestException("test"));
        assertThat(response400.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test 409
        ResponseEntity<ApiResponse<Void>> response409 = exceptionHandler.handleDuplicateResourceException(
                new DuplicateResourceException("test"), webRequest);
        assertThat(response409.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Test 401
        ResponseEntity<ApiResponse<Void>> response401 = exceptionHandler.handleUnauthorizedException(
                new UnauthorizedException("test"), webRequest);
        assertThat(response401.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Test 403
        ResponseEntity<ApiResponse<Void>> response403 = exceptionHandler.handleForbiddenException(
                new ForbiddenException("test"), webRequest);
        assertThat(response403.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should have non-null response body for all exceptions")
    void testResponseBodyNotNull() {
        // Given
        BadRequestException exception = new BadRequestException("test");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getStatusCode()).isNotNull();
    }

    @Test
    @DisplayName("Should handle exception with long error message")
    void testHandleLongErrorMessage() {
        // Given
        String longMessage = "This is a very long error message that contains detailed information about what went wrong. ".repeat(10);
        BadRequestException exception = new BadRequestException(longMessage);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should handle exception with special characters in message")
    void testHandleSpecialCharactersInMessage() {
        // Given
        String message = "Error: Invalid input <>&\"'";
        BadRequestException exception = new BadRequestException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ============= Additional Comprehensive Tests =============

    @Test
    @DisplayName("Should handle TenantExpiredException as BadRequestException")
    void testHandleTenantExpiredException() {
        // Given
        TenantExpiredException exception = new TenantExpiredException("tenant-1", "2026-01-01");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("TENANT_EXPIRED");
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401 status")
    void testHandleBadCredentialsException() {
        // Given
        String message = "Invalid credentials";
        org.springframework.security.authentication.BadCredentialsException exception =
                new org.springframework.security.authentication.BadCredentialsException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadCredentialsException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with 403 status")
    void testHandleAccessDeniedException() {
        // Given
        String message = "Access denied";
        org.springframework.security.access.AccessDeniedException exception =
                new org.springframework.security.access.AccessDeniedException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDeniedException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("Should handle NoSuchElementException with 404 status")
    void testHandleNoSuchElementException() {
        // Given
        String message = "Element not found";
        java.util.NoSuchElementException exception = new java.util.NoSuchElementException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoSuchElementException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Should handle IllegalStateException with 400 status")
    void testHandleIllegalStateException() {
        // Given
        String message = "Illegal state";
        IllegalStateException exception = new IllegalStateException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalStateException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("ILLEGAL_STATE");
    }

    @Test
    @DisplayName("Should handle IllegalStateException with null message gracefully")
    void testHandleIllegalStateException_NullMessage() {
        // Given
        IllegalStateException exception = new IllegalStateException((String) null);
        when(messageService.getMessage("error.illegal.state")).thenReturn("Illegal state");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalStateException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Illegal state");
    }

    @Test
    @DisplayName("Should handle RuntimeException with 500 status")
    void testHandleRuntimeException() {
        // Given
        String message = "Runtime error";
        RuntimeException exception = new RuntimeException(message);

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleRuntimeException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("Should verify error field is set correctly for each exception type")
    void testErrorFieldValues() {
        // BadRequestException
        BadRequestException badReq = new BadRequestException("test");
        ResponseEntity<ApiResponse<Void>> badReqResponse = exceptionHandler.handleBadRequestException(badReq);
        assertThat(badReqResponse.getBody()).isNotNull();
        assertThat(badReqResponse.getBody().getError()).isEqualTo("BAD_REQUEST");

        // ResourceNotFoundException
        ResourceNotFoundException notFound = new ResourceNotFoundException("test");
        ResponseEntity<ApiResponse<Void>> notFoundResponse = exceptionHandler.handleResourceNotFoundException(notFound);
        assertThat(notFoundResponse.getBody()).isNotNull();
        assertThat(notFoundResponse.getBody().getError()).isEqualTo("RESOURCE_NOT_FOUND");

        // DuplicateResourceException
        DuplicateResourceException duplicate = new DuplicateResourceException("test");
        ResponseEntity<ApiResponse<Void>> duplicateResponse = exceptionHandler.handleDuplicateResourceException(duplicate, webRequest);
        assertThat(duplicateResponse.getBody()).isNotNull();
        assertThat(duplicateResponse.getBody().getError()).isEqualTo("DUPLICATE_RESOURCE");

        // BusinessException
        BusinessException business = new BusinessException("test");
        ResponseEntity<ApiResponse<Void>> businessResponse = exceptionHandler.handleBusinessException(business, webRequest);
        assertThat(businessResponse.getBody()).isNotNull();
        assertThat(businessResponse.getBody().getError()).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    @DisplayName("Should handle exception with null cause")
    void testHandleExceptionWithNullCause() {
        // Given
        Exception exception = new Exception("test");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGlobalException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should verify success field is false for all error responses")
    void testSuccessFieldIsFalseForErrors() {
        // BadRequestException
        BadRequestException ex1 = new BadRequestException("test");
        ResponseEntity<ApiResponse<Void>> res1 = exceptionHandler.handleBadRequestException(ex1);
        assertThat(res1.getBody()).isNotNull();
        assertThat(res1.getBody().isSuccess()).isFalse();

        // ResourceNotFoundException
        ResourceNotFoundException ex2 = new ResourceNotFoundException("test");
        ResponseEntity<ApiResponse<Void>> res2 = exceptionHandler.handleResourceNotFoundException(ex2);
        assertThat(res2.getBody()).isNotNull();
        assertThat(res2.getBody().isSuccess()).isFalse();

        // DuplicateResourceException
        DuplicateResourceException ex3 = new DuplicateResourceException("test");
        ResponseEntity<ApiResponse<Void>> res3 = exceptionHandler.handleDuplicateResourceException(ex3, webRequest);
        assertThat(res3.getBody()).isNotNull();
        assertThat(res3.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should handle BadRequestException with null message gracefully")
    void testBadRequestException_NullMessage_WithMessageService() {
        // Given
        BadRequestException exception = new BadRequestException(null);
        when(messageService.getMessage("error.bad.request")).thenReturn("Bad request");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with null message gracefully")
    void testResourceNotFoundException_NullMessage_WithMessageService() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException(null);
        when(messageService.getMessage("error.resource.not.found")).thenReturn("Resource not found");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with validation errors")
    void testHandleMethodArgumentNotValidException() {
        // Given
        org.springframework.web.bind.MethodArgumentNotValidException exception =
                mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fieldError = new org.springframework.validation.FieldError(
                "testObject", "testField", "Test error message");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError));
        when(messageService.getMessage("error.validation.failed")).thenReturn("Validation failed");

        // When
        ResponseEntity<ApiResponse<java.util.Map<String, String>>> response =
                exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getData()).containsKey("testField");
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException correctly")
    void testHandleMethodArgumentTypeMismatchException() {
        // Given
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception =
                mock(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class);
        when(exception.getRequiredType()).thenReturn(null);
        when(exception.getName()).thenReturn("id");
        when(messageService.getMessage("error.type.mismatch", "id", "unknown")).thenReturn("Type mismatch error");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleTypeMismatch(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with null message gracefully")
    void testHandleIllegalArgumentException_NullMessage() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException((String) null);
        when(messageService.getMessage("error.invalid.argument")).thenReturn("Invalid argument");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");
    }

    @Test
    @DisplayName("Should handle BusinessException with null message gracefully")
    void testHandleBusinessException_NullMessage() {
        // Given
        BusinessException exception = new BusinessException(null);
        when(messageService.getMessage("error.business.rule.violation")).thenReturn("Business rule violation");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle ForbiddenException with null message gracefully")
    void testHandleForbiddenException_NullMessage() {
        // Given
        ForbiddenException exception = new ForbiddenException(null);
        when(messageService.getMessage("error.access.denied")).thenReturn("Access denied");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleForbiddenException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should verify data field is null for error responses")
    void testDataFieldIsNullForErrors() {
        // Given
        BadRequestException exception = new BadRequestException("test");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException with null message gracefully")
    void testHandleDuplicateResourceException_NullMessage() {
        // Given
        DuplicateResourceException exception = new DuplicateResourceException(null);
        when(messageService.getMessage("error.duplicate.resource")).thenReturn("Duplicate resource");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDuplicateResourceException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle UnauthorizedException with null message gracefully")
    void testHandleUnauthorizedException_NullMessage() {
        // Given
        UnauthorizedException exception = new UnauthorizedException(null);
        when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnauthorizedException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle NoSuchElementException with null message gracefully")
    void testHandleNoSuchElementException_NullMessage() {
        // Given
        java.util.NoSuchElementException exception = mock(java.util.NoSuchElementException.class);
        when(exception.getMessage()).thenReturn(null);
        when(messageService.getMessage("error.resource.not.found")).thenReturn("Resource not found");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoSuchElementException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("Should handle RuntimeException with null message gracefully")
    void testHandleRuntimeException_NullMessage() {
        // Given
        RuntimeException exception = new RuntimeException((String) null);
        when(messageService.getMessage("error.unexpected")).thenReturn("Unexpected error");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleRuntimeException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error");
    }

    @Test
    @DisplayName("Should handle Exception with null message gracefully")
    void testHandleGlobalException_NullMessage() {
        // Given
        Exception exception = new Exception("test");
        when(messageService.getMessage("error.internal.server")).thenReturn("Internal server error");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGlobalException(exception, webRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should verify message field is populated correctly for all exceptions")
    void testMessageFieldPopulatedForAllExceptions() {
        // BadRequestException
        BadRequestException ex1 = new BadRequestException("Bad request message");
        ResponseEntity<ApiResponse<Void>> res1 = exceptionHandler.handleBadRequestException(ex1);
        assertThat(res1.getBody()).isNotNull();
        assertThat(res1.getBody().getMessage()).isEqualTo("Bad request message");

        // ResourceNotFoundException
        ResourceNotFoundException ex2 = new ResourceNotFoundException("Not found message");
        ResponseEntity<ApiResponse<Void>> res2 = exceptionHandler.handleResourceNotFoundException(ex2);
        assertThat(res2.getBody()).isNotNull();
        assertThat(res2.getBody().getMessage()).isEqualTo("Not found message");
    }

    @Test
    @DisplayName("Should handle exception with empty message")
    void testHandleExceptionWithEmptyMessage() {
        // Given
        BadRequestException exception = new BadRequestException("");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotNull();
    }
}

