package com.tappy.pos.model.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Create a booking. Two modes via {@link #bookingType}:
 *  - WALK_IN: starts the timer immediately (scheduled* fields ignored).
 *  - RESERVATION: requires scheduledDate + scheduledStartTime (and optional end).
 */
@Data
public class CreateBookingRequest {

    @NotNull
    private Long resourceId;

    /** RESERVATION | WALK_IN (default WALK_IN). */
    private String bookingType = "WALK_IN";

    private Long customerId;
    private String customerName;
    private String customerPhone;

    // Reservation slot
    private LocalDate scheduledDate;
    private LocalTime scheduledStartTime;
    private LocalTime scheduledEndTime;

    // Đặt cọc giữ sân — deposit to hold the reservation (netted at checkout).
    private java.math.BigDecimal depositAmount;
    private Boolean depositPaid;

    // Sân cố định — repeat this reservation weekly. recurrenceCount = total occurrences
    // (including the first); materializes N weekly rows sharing a recurrence group id.
    private Boolean recurrenceWeekly;
    private Integer recurrenceCount;

    private String note;
}
