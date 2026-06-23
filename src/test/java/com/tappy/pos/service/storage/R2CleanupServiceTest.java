package com.tappy.pos.service.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("R2CleanupService Unit Tests")
class R2CleanupServiceTest {

    @Mock private R2StorageService r2StorageService;

    @InjectMocks
    private R2CleanupService service;

    @Test
    @DisplayName("deleteAsync delegates to the storage service for a valid key")
    void deleteAsync_success() {
        service.deleteAsync("products/tenant-abc/42.jpg");

        verify(r2StorageService).delete("products/tenant-abc/42.jpg");
    }

    @Test
    @DisplayName("deleteAsync swallows exceptions from the storage service")
    void deleteAsync_swallowsException() {
        doThrow(new RuntimeException("boom")).when(r2StorageService).delete(anyString());

        service.deleteAsync("products/tenant-abc/42.jpg"); // must not throw

        verify(r2StorageService).delete("products/tenant-abc/42.jpg");
    }

    @Test
    @DisplayName("deleteAsync is a no-op for a null key")
    void deleteAsync_nullKey() {
        service.deleteAsync(null);

        verify(r2StorageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("deleteAsync is a no-op for a blank key")
    void deleteAsync_blankKey() {
        service.deleteAsync("   ");

        verify(r2StorageService, never()).delete(anyString());
    }
}
