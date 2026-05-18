package com.tappy.pos.service.integration;

import com.tappy.pos.model.dto.integration.IntegrationStatusDTO;

public interface IntegrationService {

    /** Returns the OAuth authorization URL for the given integration type. */
    String getAuthUrl(String integrationType, String tenantId);

    /** Processes the OAuth callback, persists credentials, and returns status. */
    IntegrationStatusDTO handleOAuthCallback(String code, String state, String integrationType);

    /** Returns connection status for one integration type. */
    IntegrationStatusDTO getStatus(String integrationType, String tenantId);

    /** Disconnects an integration (clears tokens, keeps linked data). */
    void disconnect(String integrationType, String tenantId);
}
