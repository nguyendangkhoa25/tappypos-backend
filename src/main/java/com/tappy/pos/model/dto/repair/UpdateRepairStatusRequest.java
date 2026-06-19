package com.tappy.pos.model.dto.repair;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRepairStatusRequest {
    /** Target status: RECEIVED | DIAGNOSING | QUOTED | REPAIRING | COMPLETED | DELIVERED | CANCELLED */
    @NotBlank
    private String status;

    /** Optional note recorded with the transition. */
    private String note;
}
