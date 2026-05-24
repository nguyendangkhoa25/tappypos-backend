package com.tappy.pos.model.dto.tenant;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Connection status of the tenant's own Zalo Official Account.
 * Returned by GET /shop-config/zalo-oa.
 */
@Data
@Builder
public class ZaloOaStatusDTO {
    /** True when a valid access token exists for the tenant's own OA. */
    private boolean connected;
    /** The App ID stored for this tenant, if any. Never returns secret/token values. */
    private String appId;
    /** Display name of the Zalo Official Account, e.g. "Tiệm Cà Phê ABC". */
    private String oaName;
    /** Zalo OA ID (page identifier), if provided by the shop owner. */
    private String oaId;
    /** When the current access token expires. Null if not connected. */
    private LocalDateTime tokenExpiry;
}
