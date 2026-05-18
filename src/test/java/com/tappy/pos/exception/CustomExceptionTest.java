package com.tappy.pos.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Custom Exception Classes Unit Tests")
class CustomExceptionTest {

    // ============= BadRequestException Tests =============

    @Test
    @DisplayName("Should create BadRequestException with message")
    void testBadRequestException_WithMessage() {
        // Given
        String message = "Invalid request parameter";

        // When
        BadRequestException exception = new BadRequestException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create BadRequestException with message and cause")
    void testBadRequestException_WithMessageAndCause() {
        // Given
        String message = "Invalid request parameter";
        Throwable cause = new IllegalArgumentException("Invalid value");

        // When
        BadRequestException exception = new BadRequestException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should be throwable - BadRequestException")
    void testBadRequestException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new BadRequestException("Invalid request");
        }).isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid request");
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - BadRequestException")
    void testBadRequestException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new BadRequestException("Invalid request");
        }).isInstanceOf(RuntimeException.class);
    }

    // ============= ResourceNotFoundException Tests =============

    @Test
    @DisplayName("Should create ResourceNotFoundException with message")
    void testResourceNotFoundException_WithMessage() {
        // Given
        String message = "Resource not found";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create ResourceNotFoundException with message and cause")
    void testResourceNotFoundException_WithMessageAndCause() {
        // Given
        String message = "Resource not found";
        Throwable cause = new NullPointerException("Resource is null");

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause()).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should be throwable - ResourceNotFoundException")
    void testResourceNotFoundException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException("Product not found");
        }).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - ResourceNotFoundException")
    void testResourceNotFoundException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException("Product not found");
        }).isInstanceOf(RuntimeException.class);
    }

    // ============= DuplicateResourceException Tests =============

    @Test
    @DisplayName("Should create DuplicateResourceException with message")
    void testDuplicateResourceException_WithMessage() {
        // Given
        String message = "Resource already exists";

        // When
        DuplicateResourceException exception = new DuplicateResourceException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create DuplicateResourceException with message and cause")
    void testDuplicateResourceException_WithMessageAndCause() {
        // Given
        String message = "Duplicate username";
        Throwable cause = new Exception("Database constraint violation");

        // When
        DuplicateResourceException exception = new DuplicateResourceException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be throwable - DuplicateResourceException")
    void testDuplicateResourceException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new DuplicateResourceException("Username already exists");
        }).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists");
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - DuplicateResourceException")
    void testDuplicateResourceException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new DuplicateResourceException("Username already exists");
        }).isInstanceOf(RuntimeException.class);
    }

    // ============= UnauthorizedException Tests =============

    @Test
    @DisplayName("Should create UnauthorizedException with message")
    void testUnauthorizedException_WithMessage() {
        // Given
        String message = "Invalid credentials";

        // When
        UnauthorizedException exception = new UnauthorizedException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create UnauthorizedException with message and cause")
    void testUnauthorizedException_WithMessageAndCause() {
        // Given
        String message = "Invalid credentials";
        Throwable cause = new SecurityException("Token expired");

        // When
        UnauthorizedException exception = new UnauthorizedException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be throwable - UnauthorizedException")
    void testUnauthorizedException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new UnauthorizedException("Authentication failed");
        }).isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication failed");
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - UnauthorizedException")
    void testUnauthorizedException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new UnauthorizedException("Authentication failed");
        }).isInstanceOf(RuntimeException.class);
    }

    // ============= ForbiddenException Tests =============

    @Test
    @DisplayName("Should create ForbiddenException with message")
    void testForbiddenException_WithMessage() {
        // Given
        String message = "Access denied";

        // When
        ForbiddenException exception = new ForbiddenException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create ForbiddenException with message and cause")
    void testForbiddenException_WithMessageAndCause() {
        // Given
        String message = "Insufficient permissions";
        Throwable cause = new Exception("User lacks required role");

        // When
        ForbiddenException exception = new ForbiddenException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be throwable - ForbiddenException")
    void testForbiddenException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new ForbiddenException("You do not have permission");
        }).isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission");
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - ForbiddenException")
    void testForbiddenException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new ForbiddenException("You do not have permission");
        }).isInstanceOf(RuntimeException.class);
    }

    // ============= BusinessException Tests =============

    @Test
    @DisplayName("Should create BusinessException with message")
    void testBusinessException_WithMessage() {
        // Given
        String message = "Business rule violation";

        // When
        BusinessException exception = new BusinessException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create BusinessException with message and cause")
    void testBusinessException_WithMessageAndCause() {
        // Given
        String message = "Insufficient inventory";
        Throwable cause = new Exception("Stock level too low");

        // When
        BusinessException exception = new BusinessException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be throwable - BusinessException")
    void testBusinessException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new BusinessException("Cannot process order");
        }).isInstanceOf(BusinessException.class)
                .hasMessage("Cannot process order");
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - BusinessException")
    void testBusinessException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new BusinessException("Cannot process order");
        }).isInstanceOf(RuntimeException.class);
    }

    // ============= TenantExpiredException Tests =============

    @Test
    @DisplayName("Should create TenantExpiredException with tenantId and expirationDate")
    void testTenantExpiredException_WithTenantIdAndDate() {
        // Given
        String tenantId = "kim-ngan-phat";
        String expirationDate = "2026-03-15";

        // When
        TenantExpiredException exception = new TenantExpiredException(tenantId, expirationDate);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getTenantId()).isEqualTo(tenantId);
        assertThat(exception.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(exception.getMessage()).contains(tenantId);
        assertThat(exception.getMessage()).contains(expirationDate);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should format message correctly in TenantExpiredException")
    void testTenantExpiredException_MessageFormat() {
        // Given
        String tenantId = "test-tenant";
        String expirationDate = "2025-12-31";

        // When
        TenantExpiredException exception = new TenantExpiredException(tenantId, expirationDate);

        // Then
        assertThat(exception.getMessage())
                .contains("test-tenant")
                .contains("2025-12-31")
                .contains("expired")
                .contains("support");
    }

    @Test
    @DisplayName("Should be throwable - TenantExpiredException")
    void testTenantExpiredException_IsThrowable() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new TenantExpiredException("tenant-1", "2026-01-01");
        }).isInstanceOf(TenantExpiredException.class);
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException - TenantExpiredException")
    void testTenantExpiredException_CatchableAsRuntimeException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new TenantExpiredException("tenant-1", "2026-01-01");
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should have correct getter methods in TenantExpiredException")
    void testTenantExpiredException_Getters() {
        // Given
        String tenantId = "my-tenant";
        String expirationDate = "2026-06-30";

        // When
        TenantExpiredException exception = new TenantExpiredException(tenantId, expirationDate);

        // Then
        assertThat(exception.getTenantId()).isNotNull();
        assertThat(exception.getTenantId()).isEqualTo(tenantId);
        assertThat(exception.getExpirationDate()).isNotNull();
        assertThat(exception.getExpirationDate()).isEqualTo(expirationDate);
    }

    @Test
    @DisplayName("Should handle null message in exception constructors")
    void testExceptions_WithNullMessage() {
        // BadRequestException
        BadRequestException ex1 = new BadRequestException(null);
        assertThat(ex1.getMessage()).isNull();

        // ResourceNotFoundException
        ResourceNotFoundException ex2 = new ResourceNotFoundException(null);
        assertThat(ex2.getMessage()).isNull();

        // DuplicateResourceException
        DuplicateResourceException ex3 = new DuplicateResourceException(null);
        assertThat(ex3.getMessage()).isNull();

        // UnauthorizedException
        UnauthorizedException ex4 = new UnauthorizedException(null);
        assertThat(ex4.getMessage()).isNull();

        // ForbiddenException
        ForbiddenException ex5 = new ForbiddenException(null);
        assertThat(ex5.getMessage()).isNull();

        // BusinessException
        BusinessException ex6 = new BusinessException(null);
        assertThat(ex6.getMessage()).isNull();
    }

    @Test
    @DisplayName("Should preserve cause chain in exceptions")
    void testExceptions_CauseChain() {
        // Given
        Throwable root = new Exception("Root cause");
        Throwable intermediate = new BadRequestException("Intermediate", root);
        Throwable top = new RuntimeException("Top level", intermediate);

        // When & Then
        assertThat(intermediate.getCause()).isEqualTo(root);
        assertThat(top.getCause()).isEqualTo(intermediate);
        assertThat(top.getCause().getCause()).isEqualTo(root);
    }

    @Test
    @DisplayName("Should handle empty message in exceptions")
    void testExceptions_WithEmptyMessage() {
        // BadRequestException
        BadRequestException ex1 = new BadRequestException("");
        assertThat(ex1.getMessage()).isEmpty();

        // ResourceNotFoundException
        ResourceNotFoundException ex2 = new ResourceNotFoundException("");
        assertThat(ex2.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("Should handle very long message in exceptions")
    void testExceptions_WithVeryLongMessage() {
        // Given
        String longMessage = "Error message: ".repeat(100) + "end";

        // When
        BadRequestException exception = new BadRequestException(longMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(longMessage);
        assertThat(exception.getMessage().length()).isGreaterThan(100);
    }

    @Test
    @DisplayName("Should handle special characters in exception messages")
    void testExceptions_WithSpecialCharacters() {
        // Given
        String specialMessage = "Error: <>&\"' @ # $ % ^ * () [] {} ";

        // When
        BadRequestException exception = new BadRequestException(specialMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(specialMessage);
    }

    @Test
    @DisplayName("Should handle unicode characters in exception messages")
    void testExceptions_WithUnicodeCharacters() {
        // Given
        String unicodeMessage = "Error: Khách hàng 顧客 고객";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(unicodeMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(unicodeMessage);
    }

    @Test
    @DisplayName("Should have correct inheritance hierarchy")
    void testExceptions_InheritanceHierarchy() {
        // All custom exceptions should extend RuntimeException
        assertThat(BadRequestException.class.getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(ResourceNotFoundException.class.getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(DuplicateResourceException.class.getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(UnauthorizedException.class.getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(ForbiddenException.class.getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(BusinessException.class.getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(TenantExpiredException.class.getSuperclass()).isEqualTo(RuntimeException.class);
    }

    @Test
    @DisplayName("Should be stackable exceptions")
    void testExceptions_Stackable() {
        // When & Then - Multiple nested exceptions
        assertThatThrownBy(() -> {
            try {
                throw new IllegalArgumentException("Invalid input");
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Failed to process request", e);
            }
        }).isInstanceOf(BadRequestException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    // ============= PawnStatusNotAllowException Tests =============

    @Test
    @DisplayName("PawnStatusNotAllowException default constructor has built-in message")
    void testPawnStatusNotAllowException_DefaultMessage() {
        PawnStatusNotAllowException ex = new PawnStatusNotAllowException();
        assertThat(ex.getMessage()).isEqualTo("Pawn status does not allow this action");
        assertThat(ex).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("PawnStatusNotAllowException accepts custom message")
    void testPawnStatusNotAllowException_CustomMessage() {
        PawnStatusNotAllowException ex = new PawnStatusNotAllowException("Hợp đồng đã thanh lý");
        assertThat(ex.getMessage()).isEqualTo("Hợp đồng đã thanh lý");
    }

    @Test
    @DisplayName("PawnStatusNotAllowException is throwable and catchable as BusinessException")
    void testPawnStatusNotAllowException_IsThrowable() {
        assertThatThrownBy(() -> { throw new PawnStatusNotAllowException(); })
                .isInstanceOf(PawnStatusNotAllowException.class)
                .isInstanceOf(BusinessException.class);
    }

    // ============= DeviceConflictException Tests =============

    @Test
    @DisplayName("DeviceConflictException has built-in message and stores session")
    void testDeviceConflictException_SessionInfo() {
        com.tappy.pos.service.auth.SessionInfo session = new com.tappy.pos.service.auth.SessionInfo(
                "session-123", "192.168.1.1", "Mozilla/5.0", java.time.LocalDateTime.now());
        DeviceConflictException ex = new DeviceConflictException(session);
        assertThat(ex.getMessage()).contains("active");
        assertThat(ex.getExistingSession()).isSameAs(session);
    }

    // ============= AccountLockedException Tests =============

    @Test
    @DisplayName("AccountLockedException wraps message")
    void testAccountLockedException_Message() {
        AccountLockedException ex = new AccountLockedException("Tài khoản bị khóa");
        assertThat(ex.getMessage()).isEqualTo("Tài khoản bị khóa");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}

