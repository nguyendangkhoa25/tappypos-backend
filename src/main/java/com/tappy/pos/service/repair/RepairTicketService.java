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

    /**
     * Warranty lookup (§4d): delivered repair tickets whose repair-warranty window is
     * still open, matched by serial/IMEI, phone, customer name, or ticket number.
     */
    java.util.List<RepairTicketDTO> warrantyLookup(String keyword);

    /**
     * Opens a warranty repair (§4d): a new free-labor ticket cloned from a still-in-warranty
     * delivered ticket. Throws if the original is no longer under warranty.
     */
    RepairTicketDTO createWarrantyClaim(Long originalTicketId);
}
