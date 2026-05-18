package com.tappy.pos.service.contact;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.contact.ContactLeadDTO;
import com.tappy.pos.model.dto.contact.ContactLeadRequest;
import com.tappy.pos.model.dto.contact.UpdateLeadStatusRequest;
import com.tappy.pos.model.entity.contact.ContactLead;
import com.tappy.pos.model.enums.LeadStatus;
import com.tappy.pos.repository.contact.ContactLeadRepository;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactLeadServiceImpl implements ContactLeadService {

    private final ContactLeadRepository contactLeadRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void submit(ContactLeadRequest request) {
        ContactLead lead = ContactLead.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .shopType(request.getShopType())
                .note(request.getNote())
                .source("LANDING_PAGE")
                .build();

        contactLeadRepository.save(lead);

        String title = "[Khách hàng mới] " + request.getName();
        String message = request.getName() + " (" + request.getPhone() + ")"
                + (request.getShopType() != null ? " — " + request.getShopType() : "")
                + " vừa đăng ký dùng thử từ trang chủ.";

        try {
            notificationService.pushToMasterUsers(title, message, "CONTACT_LEAD", lead.getId());
        } catch (Exception e) {
            log.warn("Failed to notify master users for contact lead {}: {}", lead.getId(), e.getMessage());
        }
    }

    @Override
    public Page<ContactLeadDTO> getAll(LeadStatus status, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return contactLeadRepository
                .findAll(status != null ? status.name() : null, unsorted)
                .map(this::toDTO);
    }

    @Override
    @Transactional
    public ContactLeadDTO updateStatus(Long id, UpdateLeadStatusRequest request) {
        ContactLead lead = contactLeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact lead not found: " + id));
        lead.setStatus(request.getStatus());
        lead.setAdminNote(request.getAdminNote());
        return toDTO(contactLeadRepository.save(lead));
    }

    private ContactLeadDTO toDTO(ContactLead l) {
        return ContactLeadDTO.builder()
                .id(l.getId())
                .name(l.getName())
                .phone(l.getPhone())
                .shopType(l.getShopType())
                .note(l.getNote())
                .source(l.getSource())
                .status(l.getStatus())
                .statusDisplayName(l.getStatus().getDisplayName())
                .adminNote(l.getAdminNote())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
