package com.knp.multitenant;

import com.knp.exception.TenantExpiredException;
import com.knp.model.entity.tenant.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TenantContext
 * Covers thread-local tenant management, MDC integration, and expiration validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantContext Unit Tests")
class TenantContextTest {

    @InjectMocks
    private TenantContext tenantContext;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // Clear context before each test
        tenantContext.clear();
        MDC.clear();

        // Create test tenant
        testTenant = Tenant.builder()
                .id(1L)
                .tenantId("test-tenant-001")
                .name("Test Tenant")
                .active(true)
                .expirationDate(LocalDate.now().plusDays(30))
                .build();
    }

    // ==================== setCurrentTenant Tests ====================

    @Test
    @DisplayName("Should set current tenant in thread local")
    void testSetCurrentTenant_Success() {
        // When
        tenantContext.setCurrentTenant(testTenant);

        // Then
        assertThat(tenantContext.getCurrentTenant())
                .isNotNull()
                .isEqualTo(testTenant);
    }

    @Test
    @DisplayName("Should put tenant ID in MDC")
    void testSetCurrentTenant_PutsTenantInMDC() {
        // When
        tenantContext.setCurrentTenant(testTenant);

        // Then
        assertThat(MDC.get("tenantId")).isEqualTo("test-tenant-001");
    }

    @Test
    @DisplayName("Should set tenant with different tenant IDs")
    void testSetCurrentTenant_MultipleTenants() {
        // Given
        Tenant tenant1 = Tenant.builder()
                .id(1L)
                .tenantId("tenant-1")
                .name("Tenant 1")
                .active(true)
                .build();

        Tenant tenant2 = Tenant.builder()
                .id(2L)
                .tenantId("tenant-2")
                .name("Tenant 2")
                .active(true)
                .build();

        // When & Then
        tenantContext.setCurrentTenant(tenant1);
        assertThat(tenantContext.getCurrentTenantId()).isEqualTo("tenant-1");

        tenantContext.setCurrentTenant(tenant2);
        assertThat(tenantContext.getCurrentTenantId()).isEqualTo("tenant-2");
    }

    @Test
    @DisplayName("Should handle tenant with null expiration date")
    void testSetCurrentTenant_NullExpirationDate() {
        // Given
        testTenant.setExpirationDate(null);

        // When
        tenantContext.setCurrentTenant(testTenant);

        // Then
        assertThat(tenantContext.getCurrentTenant().getExpirationDate()).isNull();
        assertThat(tenantContext.getCurrentTenantId()).isEqualTo("test-tenant-001");
    }

    // ==================== getCurrentTenant Tests ====================

    @Test
    @DisplayName("Should return null when no tenant is set")
    void testGetCurrentTenant_NoTenantSet() {
        // When & Then
        assertThat(tenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("Should return current tenant after setting")
    void testGetCurrentTenant_AfterSet() {
        // Given
        tenantContext.setCurrentTenant(testTenant);

        // When
        Tenant retrievedTenant = tenantContext.getCurrentTenant();

        // Then
        assertThat(retrievedTenant).isEqualTo(testTenant);
        assertThat(retrievedTenant.getTenantId()).isEqualTo("test-tenant-001");
    }

    // ==================== getCurrentTenantId Tests ====================

    @Test
    @DisplayName("Should return null when no tenant ID is set")
    void testGetCurrentTenantId_NoTenantSet() {
        // When & Then
        assertThat(tenantContext.getCurrentTenantId()).isNull();
    }

    @Test
    @DisplayName("Should return current tenant ID")
    void testGetCurrentTenantId_AfterSet() {
        // Given
        tenantContext.setCurrentTenant(testTenant);

        // When
        String tenantId = tenantContext.getCurrentTenantId();

        // Then
        assertThat(tenantId).isEqualTo("test-tenant-001");
    }

    @ParameterizedTest
    @ValueSource(strings = {"tenant-1", "tenant-2", "tenant-abc-123", "TENANT-XYZ"})
    @DisplayName("Should return various tenant IDs")
    void testGetCurrentTenantId_VariousIds(String tenantId) {
        // Given
        testTenant.setTenantId(tenantId);

        // When
        tenantContext.setCurrentTenant(testTenant);

        // Then
        assertThat(tenantContext.getCurrentTenantId()).isEqualTo(tenantId);
    }

    // ==================== clear Tests ====================

    @Test
    @DisplayName("Should clear current tenant from thread local")
    void testClear_RemovesTenant() {
        // Given
        tenantContext.setCurrentTenant(testTenant);
        assertThat(tenantContext.getCurrentTenant()).isNotNull();

        // When
        tenantContext.clear();

        // Then
        assertThat(tenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("Should clear tenant ID from MDC")
    void testClear_RemovesTenantFromMDC() {
        // Given
        tenantContext.setCurrentTenant(testTenant);
        assertThat(MDC.get("tenantId")).isEqualTo("test-tenant-001");

        // When
        tenantContext.clear();

        // Then
        assertThat(MDC.get("tenantId")).isNull();
    }

    @Test
    @DisplayName("Should be safe to clear when no tenant is set")
    void testClear_WhenNoTenantSet() {
        // When & Then - Should not throw exception
        assertThatNoException().isThrownBy(() -> tenantContext.clear());
    }

    @Test
    @DisplayName("Should be able to set tenant after clearing")
    void testClear_CanSetTenantAfter() {
        // Given
        tenantContext.setCurrentTenant(testTenant);
        tenantContext.clear();

        Tenant newTenant = Tenant.builder()
                .id(2L)
                .tenantId("new-tenant")
                .active(true)
                .build();

        // When
        tenantContext.setCurrentTenant(newTenant);

        // Then
        assertThat(tenantContext.getCurrentTenantId()).isEqualTo("new-tenant");
    }

    // ==================== validateTenantNotExpired Tests ====================

    @Test
    @DisplayName("Should pass validation when tenant is not expired")
    void testValidateTenantNotExpired_Success() {
        // Given
        testTenant.setExpirationDate(LocalDate.now().plusDays(30));
        tenantContext.setCurrentTenant(testTenant);

        // When & Then - Should not throw exception
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }

    @Test
    @DisplayName("Should pass validation when tenant has no expiration date")
    void testValidateTenantNotExpired_NoExpirationDate() {
        // Given
        testTenant.setExpirationDate(null);
        tenantContext.setCurrentTenant(testTenant);

        // When & Then - Should not throw exception
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }

    @Test
    @DisplayName("Should throw exception when tenant is expired")
    void testValidateTenantNotExpired_TenantExpired() {
        // Given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        testTenant.setExpirationDate(yesterday);
        tenantContext.setCurrentTenant(testTenant);

        // When & Then
        assertThatThrownBy(() -> tenantContext.validateTenantNotExpired())
                .isInstanceOf(TenantExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should throw exception with correct tenant ID in message")
    void testValidateTenantNotExpired_ExceptionContainsTenantId() {
        // Given
        testTenant.setExpirationDate(LocalDate.now().minusDays(1));
        tenantContext.setCurrentTenant(testTenant);

        // When & Then
        assertThatThrownBy(() -> tenantContext.validateTenantNotExpired())
                .isInstanceOf(TenantExpiredException.class)
                .hasMessageContaining("test-tenant-001");
    }

    @Test
    @DisplayName("Should throw exception when expiration date is today")
    void testValidateTenantNotExpired_ExpirationToday() {
        // Given
        testTenant.setExpirationDate(LocalDate.now());
        tenantContext.setCurrentTenant(testTenant);

        // When & Then
        assertThatThrownBy(() -> tenantContext.validateTenantNotExpired())
                .isInstanceOf(TenantExpiredException.class);
    }

    @Test
    @DisplayName("Should not throw exception when expiration is tomorrow")
    void testValidateTenantNotExpired_ExpirationTomorrow() {
        // Given
        testTenant.setExpirationDate(LocalDate.now().plusDays(1));
        tenantContext.setCurrentTenant(testTenant);

        // When & Then
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }

    @Test
    @DisplayName("Should return early when no tenant context is set")
    void testValidateTenantNotExpired_NoTenantSet() {
        // When & Then - Should not throw exception
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }

    @Test
    @DisplayName("Should not throw exception when expiration is more than 7 days away")
    void testValidateTenantNotExpired_MoreThan7DaysAway() {
        // Given
        testTenant.setExpirationDate(LocalDate.now().plusDays(10));
        tenantContext.setCurrentTenant(testTenant);

        // When & Then
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }

    @Test
    @DisplayName("Should warn when expiration is within 7 days")
    void testValidateTenantNotExpired_Within7Days() {
        // Given
        testTenant.setExpirationDate(LocalDate.now().plusDays(5));
        tenantContext.setCurrentTenant(testTenant);

        // When & Then - Should not throw, only warn
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @DisplayName("Should maintain separate tenant contexts in different threads")
    void testThreadSafety_SeparateContextsPerThread() throws InterruptedException {
        // Given
        Tenant tenant1 = Tenant.builder()
                .id(1L)
                .tenantId("thread-tenant-1")
                .active(true)
                .build();

        Tenant tenant2 = Tenant.builder()
                .id(2L)
                .tenantId("thread-tenant-2")
                .active(true)
                .build();

        String[] result = new String[2];

        // When
        Thread thread1 = new Thread(() -> {
            tenantContext.setCurrentTenant(tenant1);
            result[0] = tenantContext.getCurrentTenantId();
        });

        Thread thread2 = new Thread(() -> {
            tenantContext.setCurrentTenant(tenant2);
            result[1] = tenantContext.getCurrentTenantId();
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then
        assertThat(result[0]).isEqualTo("thread-tenant-1");
        assertThat(result[1]).isEqualTo("thread-tenant-2");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should complete full lifecycle: set -> validate -> clear")
    void testFullLifecycle_SetValidateClear() {
        // Given
        testTenant.setExpirationDate(LocalDate.now().plusDays(30));

        // When & Then - Set
        tenantContext.setCurrentTenant(testTenant);
        assertThat(tenantContext.getCurrentTenantId()).isEqualTo("test-tenant-001");

        // When & Then - Validate
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());

        // When & Then - Clear
        tenantContext.clear();
        assertThat(tenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("Should handle validation before setting tenant")
    void testValidateBeforeSet_NoError() {
        // When & Then - Should not throw even when no tenant is set
        assertThatNoException().isThrownBy(() -> tenantContext.validateTenantNotExpired());
    }
}

