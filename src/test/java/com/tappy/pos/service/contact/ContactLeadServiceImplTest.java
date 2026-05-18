package com.tappy.pos.service.contact;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.contact.ContactLeadDTO;
import com.tappy.pos.model.dto.contact.ContactLeadRequest;
import com.tappy.pos.model.dto.contact.UpdateLeadStatusRequest;
import com.tappy.pos.model.entity.contact.ContactLead;
import com.tappy.pos.model.enums.LeadStatus;
import com.tappy.pos.repository.contact.ContactLeadRepository;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactLeadServiceImpl Unit Tests")
class ContactLeadServiceImplTest {

    @Mock private ContactLeadRepository contactLeadRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ContactLeadServiceImpl contactLeadService;

    private ContactLead lead;
    private ContactLeadRequest submitRequest;

    @BeforeEach
    void setUp() {
        lead = ContactLead.builder()
                .name("Nguyễn Văn A")
                .phone("0901234567")
                .shopType("Tiệm vàng")
                .note("Muốn dùng thử hệ thống")
                .source("LANDING_PAGE")
                .build();
        lead.setId(1L);

        submitRequest = new ContactLeadRequest();
        submitRequest.setName("Nguyễn Văn A");
        submitRequest.setPhone("0901234567");
        submitRequest.setShopType("Tiệm vàng");
        submitRequest.setNote("Muốn dùng thử hệ thống");
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit saves lead and notifies master users")
    void testSubmit_Success() {
        when(contactLeadRepository.save(any(ContactLead.class))).thenReturn(lead);

        contactLeadService.submit(submitRequest);

        verify(contactLeadRepository).save(argThat(l ->
                "Nguyễn Văn A".equals(l.getName()) &&
                "0901234567".equals(l.getPhone()) &&
                "LANDING_PAGE".equals(l.getSource())));
        verify(notificationService).pushToMasterUsers(anyString(), anyString(), eq("CONTACT_LEAD"), isNull());
    }

    @Test
    @DisplayName("submit sets shopType in notification message when provided")
    void testSubmit_WithShopType_IncludedInMessage() {
        when(contactLeadRepository.save(any(ContactLead.class))).thenReturn(lead);

        contactLeadService.submit(submitRequest);

        verify(notificationService).pushToMasterUsers(
                contains("Nguyễn Văn A"),
                contains("Tiệm vàng"),
                eq("CONTACT_LEAD"),
                isNull());
    }

    @Test
    @DisplayName("submit does not fail when shopType is null")
    void testSubmit_NullShopType() {
        submitRequest.setShopType(null);
        ContactLead leadNoType = ContactLead.builder()
                .name("Nguyễn Văn A").phone("0901234567").source("LANDING_PAGE").build();
        leadNoType.setId(2L);
        when(contactLeadRepository.save(any(ContactLead.class))).thenReturn(leadNoType);

        contactLeadService.submit(submitRequest);

        verify(contactLeadRepository).save(any(ContactLead.class));
    }

    @Test
    @DisplayName("submit continues silently when notification throws")
    void testSubmit_NotificationFailure_Swallowed() {
        when(contactLeadRepository.save(any(ContactLead.class))).thenReturn(lead);
        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).pushToMasterUsers(anyString(), anyString(), anyString(), anyLong());

        contactLeadService.submit(submitRequest);

        verify(contactLeadRepository).save(any(ContactLead.class));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll returns page of leads without status filter")
    void testGetAll_NoStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        when(contactLeadRepository.findAll((String) null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(lead)));

        Page<ContactLeadDTO> result = contactLeadService.getAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(contactLeadRepository).findAll((String) null, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("getAll passes status name to repository when status provided")
    void testGetAll_WithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(contactLeadRepository.findAll((String) "NEW", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(lead)));

        Page<ContactLeadDTO> result = contactLeadService.getAll(LeadStatus.NEW, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(contactLeadRepository).findAll((String) "NEW", PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("getAll returns empty page when no leads match")
    void testGetAll_Empty() {
        Pageable pageable = PageRequest.of(0, 20);
        when(contactLeadRepository.findAll((String) null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ContactLeadDTO> result = contactLeadService.getAll(null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus updates status and adminNote then saves")
    void testUpdateStatus_Success() {
        UpdateLeadStatusRequest request = mock(UpdateLeadStatusRequest.class);
        when(request.getStatus()).thenReturn(LeadStatus.CONTACTED);
        when(request.getAdminNote()).thenReturn("Đã gọi điện tư vấn");

        when(contactLeadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(contactLeadRepository.save(any(ContactLead.class))).thenReturn(lead);

        ContactLeadDTO result = contactLeadService.updateStatus(1L, request);

        assertThat(result).isNotNull();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONTACTED);
        assertThat(lead.getAdminNote()).isEqualTo("Đã gọi điện tư vấn");
        verify(contactLeadRepository).save(lead);
    }

    @Test
    @DisplayName("updateStatus throws ResourceNotFoundException for missing lead")
    void testUpdateStatus_NotFound() {
        UpdateLeadStatusRequest request = mock(UpdateLeadStatusRequest.class);
        lenient().when(request.getStatus()).thenReturn(LeadStatus.CONTACTED);

        when(contactLeadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactLeadService.updateStatus(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── DTO mapping ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("toDTO maps all lead fields to ContactLeadDTO")
    void testToDTO_AllFields() {
        Pageable pageable = PageRequest.of(0, 20);
        when(contactLeadRepository.findAll((String) null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(lead)));

        Page<ContactLeadDTO> result = contactLeadService.getAll(null, pageable);
        ContactLeadDTO dto = result.getContent().get(0);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Nguyễn Văn A");
        assertThat(dto.getPhone()).isEqualTo("0901234567");
        assertThat(dto.getShopType()).isEqualTo("Tiệm vàng");
        assertThat(dto.getSource()).isEqualTo("LANDING_PAGE");
        assertThat(dto.getStatus()).isEqualTo(LeadStatus.NEW);
    }
}
