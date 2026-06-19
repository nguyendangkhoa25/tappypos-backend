package com.tappy.pos.service.repair;

import com.tappy.pos.model.dto.repair.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface RepairTicketService {

    Page<RepairTicketDTO> search(String status, String keyword, Pageable pageable);

    RepairTicketDTO getById(Long id);

    RepairTicketDTO create(CreateRepairTicketRequest request);

    RepairTicketDTO update(Long id, UpdateRepairTicketRequest request);

    RepairTicketDTO updateStatus(Long id, UpdateRepairStatusRequest request);

    RepairTicketDTO assignTechnician(Long id, AssignTechnicianRequest request);

    void delete(Long id);

    /** Ticket counts grouped by status (for the status board), scoped to visibility. */
    Map<String, Long> statusCounts();
}
