package com.knp.service.contact;

import com.knp.model.dto.contact.ContactLeadDTO;
import com.knp.model.dto.contact.ContactLeadRequest;
import com.knp.model.dto.contact.UpdateLeadStatusRequest;
import com.knp.model.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactLeadService {
    void submit(ContactLeadRequest request);
    Page<ContactLeadDTO> getAll(LeadStatus status, Pageable pageable);
    ContactLeadDTO updateStatus(Long id, UpdateLeadStatusRequest request);
}
