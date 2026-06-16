package com.tappy.pos.service.qrorder;

import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.qrorder.PublicOrderResponse;
import com.tappy.pos.model.dto.qrorder.PublicTableDTO;

/**
 * Unauthenticated QR table-ordering surface. Tenant context is set by TenantInterceptor from the
 * X-Tenant-ID header (the customer page sends it from the URL). Every method first checks the
 * tenant has the TABLE_SERVICE feature enabled.
 */
public interface PublicOrderService {

    /** Resolve a table by its QR token; validates the shop offers QR ordering. */
    PublicTableDTO resolveTable(String qrToken);

    /** Read-only menu (active products grouped by category) for the current tenant. */
    PublicMenuDTO getMenu();

    /** Create a SUBMITTED (awaiting-confirmation) order from the customer's cart. Re-prices server-side. */
    PublicOrderResponse submitOrder(String qrToken, PublicOrderRequest request);

    /** Order status for the customer's "received / confirmed / rejected" screen. */
    PublicOrderResponse getOrderStatus(Long orderId);
}
