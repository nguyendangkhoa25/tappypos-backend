package com.tappy.pos.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/**
 * Thin wrapper around Cloudflare R2 (S3-compatible).
 *
 * All uploads go to: {bucket}/{key}
 * Public URL served at: {publicUrl}/{key}
 *
 * When R2_ENDPOINT is blank (local dev without R2), upload/delete are no-ops
 * and a placeholder URL is returned so the app still works locally.
 */
@Slf4j
@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrl;
    private final boolean enabled;

    public R2StorageService(
            @Value("${r2.endpoint:}") String endpoint,
            @Value("${r2.access-key:}") String accessKey,
            @Value("${r2.secret-key:}") String secretKey,
            @Value("${r2.bucket:}") String bucket,
            @Value("${r2.public-url:}") String publicUrl) {

        this.bucket = bucket;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        this.enabled = !endpoint.isBlank() && !accessKey.isBlank() && !bucket.isBlank();

        if (this.enabled) {
            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of("auto"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            log.info("R2StorageService initialised — bucket: {}, public URL: {}", bucket, this.publicUrl);
        } else {
            this.s3Client = null;
            log.warn("R2StorageService disabled — R2_ENDPOINT / R2_ACCESS_KEY / R2_BUCKET not set. Image uploads will be no-ops.");
        }
    }

    /**
     * Upload bytes to R2 under the given key.
     *
     * @param key         e.g. "products/tenant-abc/42.jpg"
     * @param bytes       raw file bytes (already resized/compressed)
     * @param contentType MIME type, e.g. "image/jpeg"
     * @return public URL to access the uploaded object
     */
    public String upload(String key, byte[] bytes, String contentType) {
        if (!enabled) {
            log.debug("R2 disabled — skipping upload for key: {}", key);
            return "";
        }
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes));
        String url = publicUrl + "/" + key;
        log.info("R2 upload OK — key: {}, url: {}", key, url);
        return url;
    }

    /**
     * Delete an object from R2. Silently ignores errors (fire-and-forget safe).
     *
     * @param key e.g. "products/tenant-abc/42.jpg"
     */
    public void delete(String key) {
        if (!enabled || key == null || key.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("R2 delete OK — key: {}", key);
        } catch (Exception e) {
            log.warn("R2 delete failed for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Extract the R2 key from a full public URL.
     * e.g. "https://images.tappy.vn/products/abc/42.jpg" → "products/abc/42.jpg"
     * Returns null if the URL is blank or does not start with the configured public URL.
     */
    public String keyFromUrl(String url) {
        if (url == null || url.isBlank() || publicUrl.isBlank()) return null;
        // Strip query string (cache-busting ?v=... param) before comparing / extracting key
        String cleanUrl = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        if (cleanUrl.startsWith(publicUrl + "/")) {
            return cleanUrl.substring(publicUrl.length() + 1);
        }
        return null;
    }
}
