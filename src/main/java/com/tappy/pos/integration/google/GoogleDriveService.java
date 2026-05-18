package com.tappy.pos.integration.google;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level Google API client.
 * Handles token exchange/refresh, folder management, and (in Phase 2) file upload.
 * All methods are package-private or called from GoogleDriveIntegrationProvider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String DRIVE_FILES   = "https://www.googleapis.com/drive/v3/files";
    private static final String FOLDER_MIME   = "application/vnd.google-apps.folder";
    private static final String APP_ROOT_NAME = "TappyPOS";

    @Value("${integration.google-drive.client-id:}")
    private String clientId;

    @Value("${integration.google-drive.client-secret:}")
    private String clientSecret;

    @Value("${integration.google-drive.redirect-uri:http://localhost:6868/api/integrations/oauth/callback}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ── Token operations ──────────────────────────────────────────────────────

    /** Exchanges an authorization code for access + refresh tokens. */
    public TokenResult exchangeCode(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code",          code);
        params.add("client_id",     clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri",  redirectUri);
        params.add("grant_type",    "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(params, headers), Map.class);

        Map<?, ?> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("Token exchange failed — no access_token in response");
        }

        String accessToken  = (String) body.get("access_token");
        String refreshToken = (String) body.get("refresh_token");
        Object expiresRaw   = body.get("expires_in");
        long   expiresIn    = expiresRaw instanceof Number n ? n.longValue() : 3600L;
        long   expiresAt    = Instant.now().toEpochMilli() + expiresIn * 1000L;

        return new TokenResult(accessToken, refreshToken, expiresAt);
    }

    /** Uses the stored refresh token to obtain a new access token. Updates the credentials in-place. */
    public void refreshAccessToken(GoogleDriveCredentials creds) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id",     clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", creds.getRefreshToken());
        params.add("grant_type",    "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST,
                    new HttpEntity<>(params, headers), Map.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("invalid_grant")) {
                throw new DriveTokenRevokedException(
                        "Google Drive refresh token revoked or expired (invalid_grant)");
            }
            throw new IllegalStateException("Token refresh failed: " + e.getMessage());
        }

        Map<?, ?> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("Token refresh failed — no access_token in response");
        }

        String accessToken  = (String) body.get("access_token");
        Object expiresRaw   = body.get("expires_in");
        long   expiresIn    = expiresRaw instanceof Number n ? n.longValue() : 3600L;
        creds.setAccessToken(accessToken);
        creds.setTokenExpiresAt(Instant.now().toEpochMilli() + expiresIn * 1000L);
    }

    /** Returns a valid access token, refreshing if necessary. */
    public String getValidAccessToken(GoogleDriveCredentials creds) {
        boolean expired = creds.getTokenExpiresAt() == null ||
                          creds.getTokenExpiresAt() < Instant.now().toEpochMilli() + 60_000L;
        if (expired) {
            refreshAccessToken(creds);
        }
        return creds.getAccessToken();
    }

    // ── User info ─────────────────────────────────────────────────────────────

    public String getConnectedEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    USERINFO_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = resp.getBody();
            return body != null ? (String) body.get("email") : null;
        } catch (Exception e) {
            log.warn("Could not fetch Google user email: {}", e.getMessage());
            return null;
        }
    }

    // ── Folder operations ─────────────────────────────────────────────────────

    /**
     * Creates the full folder hierarchy for a shop at connection time.
     * Returns a map of entityType → folderId (e.g. "PAWN" → "1abc…").
     *
     * Structure in Drive:
     *   TappyPOS/
     *     └─ {shopName}/
     *          ├─ Pawns/
     *          ├─ Employees/
     *          ├─ Customers/
     *          └─ Products/
     */
    public FolderStructure createShopFolders(String accessToken, String shopName) {
        String appRoot  = findOrCreateFolder(accessToken, APP_ROOT_NAME, null);
        String shopRoot = findOrCreateFolder(accessToken, sanitize(shopName), appRoot);

        Map<String, String> folderIds = new LinkedHashMap<>();
        folderIds.put("PAWN",     findOrCreateFolder(accessToken, "Pawns",     shopRoot));
        folderIds.put("EMPLOYEE", findOrCreateFolder(accessToken, "Employees", shopRoot));
        folderIds.put("CUSTOMER", findOrCreateFolder(accessToken, "Customers", shopRoot));
        folderIds.put("PRODUCT",  findOrCreateFolder(accessToken, "Products",  shopRoot));

        return new FolderStructure(shopRoot, folderIds);
    }

    /**
     * Creates (or returns existing) year/month sub-folder inside a parent folder.
     * Used by the upload service (Phase 2) for high-volume entity types like PAWN.
     */
    public String getOrCreateMonthFolder(String accessToken, String parentId,
                                         int year, int month) {
        String yearFolder  = findOrCreateFolder(accessToken, String.valueOf(year), parentId);
        String monthStr    = String.format("%02d", month);
        return findOrCreateFolder(accessToken, monthStr, yearFolder);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns the Drive folder ID for {@code name} under {@code parentId},
     * creating it first if it doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public String findOrCreateFolder(String accessToken, String name, String parentId) {
        // Search for existing
        String query = "name='" + name.replace("'", "\\'") +
                       "' and mimeType='" + FOLDER_MIME + "' and trashed=false" +
                       (parentId != null ? " and '" + parentId + "' in parents" : "");

        String searchUrl = DRIVE_FILES + "?q=" + encodeQuery(query) + "&fields=files(id,name)&spaces=drive";
        HttpHeaders headers = authHeaders(accessToken);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    searchUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = resp.getBody();
            if (body != null) {
                List<Map<?, ?>> files = (List<Map<?, ?>>) body.get("files");
                if (files != null && !files.isEmpty()) {
                    return (String) files.get(0).get("id");
                }
            }
        } catch (Exception e) {
            log.debug("Drive folder search failed, will create: {}", e.getMessage());
        }

        // Create new folder
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name",     name);
        metadata.put("mimeType", FOLDER_MIME);
        if (parentId != null) {
            metadata.put("parents", List.of(parentId));
        }

        HttpHeaders createHeaders = authHeaders(accessToken);
        createHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> createResp = restTemplate.exchange(
                DRIVE_FILES, HttpMethod.POST,
                new HttpEntity<>(metadata, createHeaders), Map.class);

        Map<?, ?> created = createResp.getBody();
        if (created == null || !created.containsKey("id")) {
            throw new IllegalStateException("Failed to create Drive folder: " + name);
        }
        return (String) created.get("id");
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        return h;
    }

    private String encodeQuery(String query) {
        try {
            return java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return query;
        }
    }

    private String sanitize(String name) {
        return name == null ? "Shop" : name.trim().replaceAll("[/\\\\?%*:|\"<>]", "_");
    }

    // ── File upload ───────────────────────────────────────────────────────────

    /**
     * Uploads a file to the specified Drive folder using the multipart/related
     * upload type (metadata + binary in one request, suitable for files ≤ 5 MB).
     *
     * @param accessToken valid Drive access token
     * @param folderId    Drive folder ID to place the file in
     * @param fileName    desired file name in Drive
     * @param fileBytes   raw file bytes
     * @param mimeType    MIME type of the file (e.g. "image/jpeg")
     * @return UploadResult with fileId and webViewLink
     */
    public UploadResult uploadFile(String accessToken, String folderId,
                                   String fileName, byte[] fileBytes, String mimeType) {
        String boundary = java.util.UUID.randomUUID().toString().replace("-", "");
        String metadataJson = "{\"name\":\"" + escapeJson(fileName) +
                              "\",\"parents\":[\"" + folderId + "\"]}";

        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

            String metadataPart = "--" + boundary + "\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    metadataJson + "\r\n";
            bos.write(metadataPart.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            String filePart = "--" + boundary + "\r\n" +
                    "Content-Type: " + mimeType + "\r\n\r\n";
            bos.write(filePart.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            bos.write(fileBytes);
            bos.write(("\r\n--" + boundary + "--").getBytes(java.nio.charset.StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    org.springframework.http.MediaType.parseMediaType(
                            "multipart/related; boundary=" + boundary));
            headers.setBearerAuth(accessToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    DRIVE_FILES + "?uploadType=multipart&fields=id,webViewLink",
                    HttpMethod.POST,
                    new HttpEntity<>(bos.toByteArray(), headers),
                    Map.class);

            Map<?, ?> body = response.getBody();
            if (body == null || !body.containsKey("id")) {
                throw new IllegalStateException("Drive upload returned no file ID");
            }
            return new UploadResult((String) body.get("id"), (String) body.get("webViewLink"));

        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to build Drive upload payload", e);
        }
    }

    /**
     * Grants public read access to a Drive file so thumbnail URLs can be
     * embedded in the app without authentication.
     */
    public void makePublic(String accessToken, String fileId) {
        String url = DRIVE_FILES + "/" + fileId + "/permissions";
        Map<String, String> body = Map.of("type", "anyone", "role", "reader");

        HttpHeaders headers = authHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("Could not make Drive file public (fileId={}): {}", fileId, e.getMessage());
        }
    }

    // ── Value objects ─────────────────────────────────────────────────────────

    public record TokenResult(String accessToken, String refreshToken, long expiresAt) {}
    public record FolderStructure(String rootFolderId, Map<String, String> folderIds) {}
    public record UploadResult(String fileId, String webViewLink) {}

    // ── Private helpers ───────────────────────────────────────────────────────

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
