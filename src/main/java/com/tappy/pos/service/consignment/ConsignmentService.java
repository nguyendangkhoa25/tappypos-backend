package com.tappy.pos.service.consignment;

import com.tappy.pos.model.dto.consignment.ConsignmentDTO;
import com.tappy.pos.model.dto.consignment.ConsignmentRequest;
import com.tappy.pos.model.dto.consignment.ConsignmentSettlementDTO;
import com.tappy.pos.model.enums.ConsignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ConsignmentService {

    ConsignmentDTO create(ConsignmentRequest request);

    ConsignmentDTO update(Long id, ConsignmentRequest request);

    ConsignmentDTO getById(Long id);

    Page<ConsignmentDTO> search(ConsignmentStatus status, Pageable pageable);

    void delete(Long id);

    /** Settle-by-sales preview for a consignment over a date range (no state change). */
    ConsignmentSettlementDTO getSettlement(Long id, LocalDate from, LocalDate to);

    /** Marks the consignment SETTLED, stamping the period and total owed to the publisher. */
    ConsignmentDTO settle(Long id, LocalDate from, LocalDate to);
}
