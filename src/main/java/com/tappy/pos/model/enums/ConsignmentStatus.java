package com.tappy.pos.model.enums;

import lombok.Getter;

/** Lifecycle of a consignment (ký gửi) placement. */
@Getter
public enum ConsignmentStatus {
    ACTIVE("Đang ký gửi"),
    SETTLED("Đã thanh toán"),
    CANCELLED("Đã hủy");

    private final String displayName;

    ConsignmentStatus(String displayName) {
        this.displayName = displayName;
    }
}
