package com.tappy.pos.service.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("R2StorageService Unit Tests")
class R2StorageServiceTest {

    private R2StorageService disabled() {
        // endpoint blank → disabled, no real client built
        return new R2StorageService("", "", "", "", "https://img.tappy.vn/");
    }

    private R2StorageService enabled(S3Client mockClient) {
        R2StorageService svc = new R2StorageService(
                "https://account.r2.cloudflarestorage.com", "ak", "sk", "bkt", "https://img.tappy.vn/");
        ReflectionTestUtils.setField(svc, "s3Client", mockClient);
        return svc;
    }

    // ── disabled mode ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("upload is a no-op returning empty string when disabled")
    void upload_disabled() {
        assertThat(disabled().upload("k.jpg", new byte[]{1}, "image/jpeg")).isEmpty();
    }

    @Test
    @DisplayName("delete is a no-op when disabled")
    void delete_disabled() {
        disabled().delete("k.jpg"); // must not throw
    }

    // ── publicUrl normalisation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("trailing slash is stripped from the configured public URL")
    void publicUrl_trailingSlashStripped() {
        assertThat(ReflectionTestUtils.getField(disabled(), "publicUrl")).isEqualTo("https://img.tappy.vn");
    }

    // ── keyFromUrl ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("keyFromUrl extracts the key from a matching public URL")
    void keyFromUrl_match() {
        assertThat(disabled().keyFromUrl("https://img.tappy.vn/products/abc/42.jpg"))
                .isEqualTo("products/abc/42.jpg");
    }

    @Test
    @DisplayName("keyFromUrl strips a cache-busting query string")
    void keyFromUrl_withQuery() {
        assertThat(disabled().keyFromUrl("https://img.tappy.vn/products/abc/42.jpg?v=9"))
                .isEqualTo("products/abc/42.jpg");
    }

    @Test
    @DisplayName("keyFromUrl returns null for a non-matching host")
    void keyFromUrl_noMatch() {
        assertThat(disabled().keyFromUrl("https://other.example.com/x.jpg")).isNull();
    }

    @Test
    @DisplayName("keyFromUrl returns null for blank/null input")
    void keyFromUrl_blank() {
        assertThat(disabled().keyFromUrl(null)).isNull();
        assertThat(disabled().keyFromUrl("  ")).isNull();
    }

    @Test
    @DisplayName("keyFromUrl returns null when public URL is not configured")
    void keyFromUrl_noPublicUrl() {
        R2StorageService svc = new R2StorageService("", "", "", "", "");
        assertThat(svc.keyFromUrl("https://img.tappy.vn/x.jpg")).isNull();
    }

    // ── enabled mode ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("upload puts the object and returns the public URL when enabled")
    void upload_enabled() {
        S3Client client = mock(S3Client.class);
        R2StorageService svc = enabled(client);

        String url = svc.upload("products/abc/42.jpg", new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(url).isEqualTo("https://img.tappy.vn/products/abc/42.jpg");
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("delete removes the object when enabled")
    void delete_enabled() {
        S3Client client = mock(S3Client.class);
        R2StorageService svc = enabled(client);

        svc.delete("products/abc/42.jpg");

        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("delete ignores a blank key even when enabled")
    void delete_blankKey() {
        S3Client client = mock(S3Client.class);
        R2StorageService svc = enabled(client);

        svc.delete("  ");
        svc.delete(null);

        verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("delete swallows S3 exceptions")
    void delete_swallowsException() {
        S3Client client = mock(S3Client.class);
        doThrow(new RuntimeException("boom")).when(client).deleteObject(any(DeleteObjectRequest.class));
        R2StorageService svc = enabled(client);

        svc.delete("products/abc/42.jpg"); // must not throw
    }
}
