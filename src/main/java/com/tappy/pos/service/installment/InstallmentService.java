package com.tappy.pos.service.installment;

import com.tappy.pos.model.dto.installment.CreateInstallmentRequest;
import com.tappy.pos.model.dto.installment.InstallmentDTO;
import com.tappy.pos.model.dto.installment.PayInstallmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstallmentService {
    InstallmentDTO create(CreateInstallmentRequest request);
    InstallmentDTO getById(Long debtId);
    Page<InstallmentDTO> search(Pageable pageable);
    /** Record payment of one kỳ; returns the refreshed contract. */
    InstallmentDTO payPeriod(Long scheduleId, PayInstallmentRequest request);
    InstallmentDTO cancel(Long debtId, String reason);
    /** Scheduler entry: push an overdue summary to SHOP_OWNER/MANAGER for the current tenant. */
    void notifyOverdue();
}
