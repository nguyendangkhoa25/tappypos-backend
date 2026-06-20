package com.tappy.pos.model.dto.installment;

import com.tappy.pos.model.enums.DebtStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A trả-góp contract = the CustomerDebt summary + its per-kỳ schedule. */
@Data
@Builder
public class InstallmentDTO {
    private Long debtId;
    private Long customerId;
    private String customerName;
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;        // originalAmount (financed)
    private BigDecimal downPayment;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private Integer installmentCount;
    private DebtStatus status;
    private LocalDate nextDueDate;         // earliest unpaid kỳ
    private boolean overdue;               // any unpaid kỳ past due
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<InstallmentScheduleDTO> schedule;
}
