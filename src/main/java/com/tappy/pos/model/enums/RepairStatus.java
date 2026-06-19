package com.tappy.pos.model.enums;

import java.util.List;

/**
 * Repair-ticket workflow states. The happy path is ordered:
 * RECEIVED → DIAGNOSING → QUOTED → REPAIRING → COMPLETED → DELIVERED.
 * CANCELLED is a terminal off-path state reachable before delivery.
 */
public enum RepairStatus {
    RECEIVED,
    DIAGNOSING,
    QUOTED,
    REPAIRING,
    COMPLETED,
    DELIVERED,
    CANCELLED;

    /** Terminal states that cannot transition further. */
    public static final List<String> TERMINAL = List.of(DELIVERED.name(), CANCELLED.name());

    public static boolean exists(String value) {
        if (value == null) return false;
        for (RepairStatus s : values()) {
            if (s.name().equals(value)) return true;
        }
        return false;
    }
}
