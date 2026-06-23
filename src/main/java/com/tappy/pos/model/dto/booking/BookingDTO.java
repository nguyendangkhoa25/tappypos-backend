package com.tappy.pos.model.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private String bookingNumber;
    private Long resourceId;
    private String resourceName;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String bookingType;
    private LocalDate scheduledDate;
    private LocalTime scheduledStartTime;
    private LocalTime scheduledEndTime;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private BigDecimal hourlyRate;
    private BigDecimal timeAmount;
    private BigDecimal depositAmount;
    private Boolean depositPaid;
    private String recurrenceGroupId;
    private String status;
    private String note;
    private Long linkedOrderId;
    private String createdBy;
    private LocalDateTime createdAt;

    /** Minutes elapsed so far for a running session (computed, not persisted). */
    private Long elapsedMinutes;
}
