package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An occupancy session + its folio. {@code items}/{@code itemsTotal}/{@code balanceDue} are
 * populated on the stay-detail response.
 */
@Data
@Builder
public class RoomStayDTO {
    private Long id;
    private String stayNumber;
    private Long roomId;
    private String roomNumber;
    private String guestName;
    private String guestPhone;
    private String guestIdNumber;
    private Long customerId;
    private Integer adults;
    private String billingMode;     // NIGHTLY | HOURLY | OVERNIGHT
    private BigDecimal rate;
    private LocalDateTime checkinAt;
    private LocalDateTime expectedCheckout;
    private LocalDateTime checkoutAt;
    private Integer units;
    private BigDecimal roomCharge;
    private BigDecimal deposit;
    private String status;          // IN_HOUSE | CHECKED_OUT | CANCELLED
    private Long linkedOrderId;
    private String note;

    private List<RoomStayItemDTO> items;
    private BigDecimal itemsTotal;
    private BigDecimal grandTotal;  // roomCharge + itemsTotal
    private BigDecimal balanceDue;  // grandTotal - deposit
}
