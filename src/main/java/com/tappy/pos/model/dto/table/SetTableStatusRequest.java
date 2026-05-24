package com.tappy.pos.model.dto.table;

import com.tappy.pos.model.enums.TableStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PATCH /tables/{id}/status.
 *
 * Used by staff to manually set a table to RESERVED, CLEANING, or AVAILABLE.
 * For OCCUPIED transitions use the order checkout / occupy endpoints instead.
 */
@Data
@NoArgsConstructor
public class SetTableStatusRequest {

    @NotNull(message = "Trạng thái bàn không được để trống")
    private TableStatus status;

    /** Required when status = RESERVED. Name of the party. */
    private String reservedFor;

    /** Optional when status = RESERVED. Human-readable time, e.g. "19:00". */
    private String reservedTime;
}
