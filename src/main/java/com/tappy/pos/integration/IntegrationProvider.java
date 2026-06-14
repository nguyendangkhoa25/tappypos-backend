package com.tappy.pos.integration;

import com.tappy.pos.model.dto.integration.IntegrationStatusDTO;

/**
 * Strategy interface for third-party integrations.
 * One implementation per integration type (Google Drive, Zalo, …).
 */
public interface IntegrationProvider {

    /** Unique identifier matching the integration_type column (e.g. "GOOGLE_DRIVE"). */
    String getType();

    /**
     * Returns the OAuth2 / authorization URL the user must visit to grant access.
     * The {@code tenantId} is embedded in the OAuth state parameter so the callback
     * can resolve which tenant completed the flow.
     */
    String buildAuthUrl(String tenantId);

    /**
     * Same as {@link #buildAuthUrl(String)} but also records the originating
     * frontend {@code origin} (e.g. https://kim-ngan-phat.pos.tappy.vn) so the OAuth
     * callback can redirect back to the right host. Non-OAuth providers ignore it.
     */
    default String buildAuthUrl(String tenantId, String origin) {
        return buildAuthUrl(tenantId);
    }

    /**
     * Returns the originating frontend origin recorded for an OAuth state nonce,
     * without consuming it. Used by the callback to pick its redirect target.
     * Null when not applicable / unknown.
     */
    default String peekOAuthOrigin(String state) {
        return null;
    }

    /**
     * Exchanges the authorization code received from the provider's callback,
     * creates any required remote resources (e.g. Drive folders), and persists
     * the credentials for the given tenant.
     */
    IntegrationStatusDTO handleCallback(String code, String state);

    /** Returns the current connection status for the tenant. */
    IntegrationStatusDTO getStatus(String tenantId);

    /**
     * Revokes the stored credentials and marks the integration as DISCONNECTED.
     * Existing linked data (entity_images rows) are NOT deleted.
     */
    void disconnect(String tenantId);
}
