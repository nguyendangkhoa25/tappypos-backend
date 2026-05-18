package com.tappy.pos.service.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiAuditLogCleanupTask
 * Covers scheduling, error handling, and cleanup operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiAuditLogCleanupTask Unit Tests")
class ApiAuditLogCleanupTaskTest {

    @Mock
    private ApiAuditLogService auditLogService;

    @InjectMocks
    private ApiAuditLogCleanupTask cleanupTask;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(auditLogService);
    }

    // ==================== Basic Functionality Tests ====================

    @Test
    @DisplayName("Should successfully cleanup old logs with single deleted record")
    void testCleanupOldLogs_SuccessSingleRecord() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(1L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should successfully cleanup old logs with multiple deleted records")
    void testCleanupOldLogs_SuccessMultipleRecords() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(1000L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should successfully cleanup old logs with no deleted records")
    void testCleanupOldLogs_SuccessNoRecords() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(0L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should call cleanup with correct retention days parameter (90)")
    void testCleanupOldLogs_CorrectRetentionDays() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(5L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService).cleanupOldLogs(90);
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @ParameterizedTest(name = "Cleanup should handle {0} deleted records")
    @ValueSource(longs = {0L, 1L, 10L, 100L, 1000L, 10000L})
    @DisplayName("Should handle various deleted record counts")
    void testCleanupOldLogs_VariousRecordCounts(long deletedCount) {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(deletedCount);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle RuntimeException gracefully during cleanup")
    void testCleanupOldLogs_HandleRuntimeException() {
        // Given
        when(auditLogService.cleanupOldLogs(90))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then - Should not throw exception
        try {
            cleanupTask.cleanupOldLogs();
        } catch (Exception e) {
            throw new AssertionError("cleanupOldLogs should handle exceptions gracefully", e);
        }

        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle NullPointerException gracefully during cleanup")
    void testCleanupOldLogs_HandleNullPointerException() {
        // Given
        when(auditLogService.cleanupOldLogs(90))
                .thenThrow(new NullPointerException("Null value encountered"));

        // When & Then - Should not throw exception
        try {
            cleanupTask.cleanupOldLogs();
        } catch (Exception e) {
            throw new AssertionError("cleanupOldLogs should handle NullPointerException gracefully", e);
        }

        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException during cleanup")
    void testCleanupOldLogs_HandleIllegalArgumentException() {
        // Given
        when(auditLogService.cleanupOldLogs(90))
                .thenThrow(new IllegalArgumentException("Invalid parameter"));

        // When & Then - Should not throw exception
        try {
            cleanupTask.cleanupOldLogs();
        } catch (Exception e) {
            throw new AssertionError("cleanupOldLogs should handle IllegalArgumentException gracefully", e);
        }

        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    // ==================== Service Interaction Tests ====================

    @Test
    @DisplayName("Should call auditLogService exactly once per cleanup execution")
    void testCleanupOldLogs_ServiceCalledOnce() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(50L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(anyInt());
    }

    @Test
    @DisplayName("Should not call auditLogService more than once per cleanup")
    void testCleanupOldLogs_ServiceNotCalledMultipleTimes() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(100L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then - Verify exactly once, not more
        verify(auditLogService, times(1)).cleanupOldLogs(90);
        verifyNoMoreInteractions(auditLogService);
    }

    @Test
    @DisplayName("Should pass correct daysOld parameter to service")
    void testCleanupOldLogs_PassCorrectParameter() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(75L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle large number of deleted records")
    void testCleanupOldLogs_LargeNumberOfDeletedRecords() {
        // Given
        long largeCount = 1000000L;
        when(auditLogService.cleanupOldLogs(90)).thenReturn(largeCount);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    // ==================== Return Value Tests ====================

    @Test
    @DisplayName("Should continue execution after receiving zero deleted records")
    void testCleanupOldLogs_ContinuesAfterZeroRecords() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(0L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then - Should complete without exception
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle negative return value from cleanup (edge case)")
    void testCleanupOldLogs_NegativeReturnValue() {
        // Given - Edge case: service returns negative (shouldn't happen but test resilience)
        when(auditLogService.cleanupOldLogs(90)).thenReturn(-1L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    // ==================== Exception Message Tests ====================

    @Test
    @DisplayName("Should handle exception with message during cleanup")
    void testCleanupOldLogs_ExceptionWithMessage() {
        // Given
        when(auditLogService.cleanupOldLogs(90))
                .thenThrow(new RuntimeException("Connection timeout after 30 seconds"));

        // When & Then
        try {
            cleanupTask.cleanupOldLogs();
        } catch (Exception e) {
            throw new AssertionError("Should handle exception with message", e);
        }

        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle exception with cause during cleanup")
    void testCleanupOldLogs_ExceptionWithCause() {
        // Given
        Exception cause = new RuntimeException("Underlying database error");
        Exception exception = new RuntimeException("Cleanup failed", cause);
        when(auditLogService.cleanupOldLogs(90)).thenThrow(exception);

        // When & Then
        try {
            cleanupTask.cleanupOldLogs();
        } catch (Exception e) {
            throw new AssertionError("Should handle exception with cause", e);
        }

        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    // ==================== Multiple Invocation Tests ====================

    @Test
    @DisplayName("Should handle multiple consecutive cleanup calls")
    void testCleanupOldLogs_MultipleConsecutiveCalls() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(50L).thenReturn(30L).thenReturn(10L);

        // When
        cleanupTask.cleanupOldLogs();
        cleanupTask.cleanupOldLogs();
        cleanupTask.cleanupOldLogs();

        // Then
        verify(auditLogService, times(3)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle multiple cleanup calls with exceptions")
    void testCleanupOldLogs_MultipleCallsWithExceptions() {
        // Given
        when(auditLogService.cleanupOldLogs(90))
                .thenReturn(50L)
                .thenThrow(new RuntimeException("Error on second call"))
                .thenReturn(30L);

        // When & Then
        cleanupTask.cleanupOldLogs();  // Success
        cleanupTask.cleanupOldLogs();  // Exception
        cleanupTask.cleanupOldLogs();  // Success

        verify(auditLogService, times(3)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should handle state isolation between cleanup calls")
    void testCleanupOldLogs_StateIsolationBetweenCalls() {
        // Given
        when(auditLogService.cleanupOldLogs(90))
                .thenReturn(100L)
                .thenReturn(200L);

        // When
        cleanupTask.cleanupOldLogs();
        cleanupTask.cleanupOldLogs();

        // Then - Verify each call is independent
        verify(auditLogService, times(2)).cleanupOldLogs(90);
    }

    // ==================== Component Configuration Tests ====================

    @Test
    @DisplayName("Should be instantiated with ApiAuditLogService dependency")
    void testCleanupTask_HasRequiredDependency() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(0L);

        // When
        cleanupTask.cleanupOldLogs();

        // Then - Verify task executed successfully and called the service
        verify(auditLogService, times(1)).cleanupOldLogs(90);
    }

    @Test
    @DisplayName("Should have public cleanupOldLogs method accessible")
    void testCleanupTask_MethodIsPublic() {
        // Given
        when(auditLogService.cleanupOldLogs(90)).thenReturn(42L);

        // When & Then - Method should be callable and executable
        cleanupTask.cleanupOldLogs();

        verify(auditLogService).cleanupOldLogs(90);
    }
}


