package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantStatusResponse {
    private String shopId;
    private String shopName;
    private String status; // ACTIVE | SUSPENDED | NOT_FOUND
}
