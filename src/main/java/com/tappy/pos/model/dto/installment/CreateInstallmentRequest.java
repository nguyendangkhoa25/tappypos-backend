package com.tappy.pos.model.dto.installment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Set up a trả-góp contract: total → trừ trả trước → chia N kỳ từ ngày bắt đầu. Interest-free. */
@Data
public class CreateInstallmentRequest {

    @NotNull(message = "{error.installment.customerRequired}")
    private Long customerId;

    private Long orderId;
    private String orderNumber;

    @NotNull(message = "{error.installment.totalRequired}")
    @Positive(message = "{error.installment.totalRequired}")
    private BigDecimal totalAmount;     // tổng giá trị hợp đồng

    private BigDecimal downPayment;     // trả trước (optional)

    @NotNull(message = "{error.installment.periodsRequired}")
    @Min(value = 1, message = "{error.installment.periodsRequired}")
    private Integer numberOfPeriods;    // số kỳ

    @NotNull(message = "{error.installment.firstDueRequired}")
    private LocalDate firstDueDate;     // ngày đến hạn kỳ đầu

    /** Khoảng cách giữa các kỳ tính theo tháng; mặc định 1 (hàng tháng). */
    private Integer intervalMonths;

    private String note;
}
