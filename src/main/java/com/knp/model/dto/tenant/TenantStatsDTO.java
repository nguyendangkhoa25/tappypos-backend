package com.knp.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TenantStatsDTO {
    private long total;
    private long active;
    private long inactive;
    private long expiringSoon;
    private long expired;
}
