package com.tappy.pos.model.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Week-level appointment count summary keyed by ISO date string (yyyy-MM-dd).
 * Days with zero appointments are omitted from the map — callers should treat
 * missing keys as 0.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentWeekSummaryDTO {
    /** ISO date string (yyyy-MM-dd) → appointment count */
    private Map<String, Long> countByDate;
}
