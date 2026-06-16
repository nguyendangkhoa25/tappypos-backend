package com.tappy.pos.service.integration;

import com.tappy.pos.model.dto.integration.IntegrationStatusDTO;

public interface IntegrationService {

    /**
     * Returns the OAuth authorization URL for the given integration type.
     * {@code origin} is the frontend origin (e.g. https://shop.pos.tappy.vn) the
     * callback should redirect back to; null/blank falls back to the configured URL.
     */
    String getAuthUrl(String integrationType, String tenantId, String origin);

    /** Processes the OAuth callback, persists credentials, and returns status. */
    IntegrationStatusDTO handleOAuthCallback(String code, String state, String integrationType);

    /** The frontend origin recorded for an OAuth state nonce (peek, no consume). */
    String peekOAuthOrigin(String integrationType, String state);

    /** Returns connection status for one integration type. */
    IntegrationStatusDTO getStatus(String integrationType, String tenantId);

    /** Disconnects an integration (clears tokens, keeps linked data). */
    void disconnect(String integrationType, String tenantId);
}
