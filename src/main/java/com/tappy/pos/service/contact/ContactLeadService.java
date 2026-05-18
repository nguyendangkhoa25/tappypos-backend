package com.tappy.pos.service.contact;

import com.tappy.pos.model.dto.contact.ContactLeadDTO;
import com.tappy.pos.model.dto.contact.ContactLeadRequest;
import com.tappy.pos.model.dto.contact.UpdateLeadStatusRequest;
import com.tappy.pos.model.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactLeadService {
    void submit(ContactLeadRequest request);
    Page<ContactLeadDTO> getAll(LeadStatus status, Pageable pageable);
    ContactLeadDTO updateStatus(Long id, UpdateLeadStatusRequest request);
}
