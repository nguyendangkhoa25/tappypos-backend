package com.tappy.pos.model.dto.payment;

import com.tappy.pos.model.enums.BillingCycle;
import lombok.Data;

/**
 * Master-admin request to record an offline (cash / direct transfer) subscription payment for a
 * tenant. The amount is derived server-side from the plan — never trusted from the client.
 */
@Data
public class RecordOfflinePaymentRequest {
    private String tenantId;
    private String planCode;
    private BillingCycle billingCycle;
    private String note;
}
