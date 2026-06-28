package com.tappy.pos.model.enums;

/**
 * Vòng đời của một tờ khai thuế.
 * DRAFT (nháp, sửa được) → FINALIZED (đã chốt số, khóa) → SUBMITTED (đã nộp lên cơ quan thuế).
 * CANCELLED = hủy (soft).
 */
public enum TaxDeclarationStatus {
    DRAFT,
    FINALIZED,
    SUBMITTED,
    CANCELLED
}
