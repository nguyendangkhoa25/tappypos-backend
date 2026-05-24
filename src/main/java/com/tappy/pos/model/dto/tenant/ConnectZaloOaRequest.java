package com.tappy.pos.model.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /shop-config/zalo-oa.
 * The shop owner pastes their App ID and App Secret from the Zalo OA Developer portal,
 * along with the display name and ID of their Official Account.
 */
@Data
public class ConnectZaloOaRequest {
    @NotBlank
    private String appId;
    @NotBlank
    private String appSecret;
    /** Display name of the Zalo Official Account, e.g. "Tiệm Cà Phê ABC". */
    @NotBlank
    private String oaName;
    /** Zalo OA ID (page identifier). Optional — stored for display purposes only. */
    private String oaId;
}
