package com.tappy.pos.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fire-and-forget R2 object deletion.
 *
 * Must be a separate bean — @Async only works via Spring's AOP proxy.
 * Self-calls within the same bean bypass the proxy and run synchronously.
 * See: ActivityLogServiceImpl for the same pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class R2CleanupService {

    private final R2StorageService r2StorageService;

    /**
     * Deletes an R2 object asynchronously. Safe to call from any @Transactional context.
     * Exceptions are swallowed — this must never fail the caller's transaction.
     *
     * @param key the R2 object key, e.g. "products/tenant-abc/42.jpg". Null/blank = no-op.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteAsync(String key) {
        if (key == null || key.isBlank()) return;
        try {
            r2StorageService.delete(key);
        } catch (Exception e) {
            log.warn("Async R2 delete failed for key {}: {}", key, e.getMessage());
        }
    }
}
