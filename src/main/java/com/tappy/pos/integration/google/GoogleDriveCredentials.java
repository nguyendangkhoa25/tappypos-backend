package com.tappy.pos.integration.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * JSON shape stored (encrypted) in shop_integrations.config_json for GOOGLE_DRIVE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleDriveCredentials {

    private String refreshToken;
    private String accessToken;
    private Long   tokenExpiresAt;   // epoch millis; 0 = unknown/expired
    private String email;            // connected Google account email

    /** Drive folder IDs created at connection time. Keys = entity type names. */
    private String rootFolderId;
    private Map<String, String> folderIds; // "PAWN" → id, "EMPLOYEE" → id, …
}
