package com.tappy.pos.service.tenant;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.SaveZaloMessageTemplateRequest;
import com.tappy.pos.model.dto.tenant.ZaloMessageTemplateDTO;
import com.tappy.pos.model.entity.tenant.ZaloMessageTemplate;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.ZaloMessageTemplateRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZaloMessageTemplateService {

    /** Template type constant for appointment reminders. */
    public static final String APPOINTMENT_REMINDER = "APPOINTMENT_REMINDER";

    /** Template type constant for pawn-contract due-date reminders sent to the borrower. */
    public static final String PAWN_DUE_REMINDER = "PAWN_DUE_REMINDER";

    @Value("${zalo.zns.appointment-reminder-template-id:}")
    private String globalAppointmentReminderTemplateId;

    @Value("${zalo.zns.pawn-due-reminder-template-id:}")
    private String globalPawnDueReminderTemplateId;

    private final ZaloMessageTemplateRepository repo;
    private final TenantContext tenantContext;
    private final MessageService messageService;

    // ── queries ────────────────────────────────────────────────────────────

    public List<ZaloMessageTemplateDTO> list(String type) {
        return repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(type)
                .stream().map(this::toDTO).toList();
    }

    public ZaloMessageTemplateDTO getById(Long id) {
        return toDTO(findActive(id));
    }

    /**
     * Returns the Zalo ZNS template ID to use for the given type.
     * Priority: tenant's default template → global application.properties value.
     * Returns {@code null} if neither is configured (caller should skip sending).
     */
    public String getDefaultTemplateId(String type) {
        return repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(type)
                .map(ZaloMessageTemplate::getTemplateId)
                .filter(id -> !id.isBlank())
                .orElseGet(() -> {
                    if (APPOINTMENT_REMINDER.equals(type) && !globalAppointmentReminderTemplateId.isBlank()) {
                        return globalAppointmentReminderTemplateId;
                    }
                    if (PAWN_DUE_REMINDER.equals(type) && !globalPawnDueReminderTemplateId.isBlank()) {
                        return globalPawnDueReminderTemplateId;
                    }
                    return null;
                });
    }

    // ── mutations ──────────────────────────────────────────────────────────

    @Transactional
    public ZaloMessageTemplateDTO create(String type, SaveZaloMessageTemplateRequest req) {
        boolean firstOfType = !repo.existsByTemplateTypeAndDeletedFalse(type);
        ZaloMessageTemplate tpl = ZaloMessageTemplate.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name(req.getName().trim())
                .templateType(type)
                .templateId(req.getTemplateId().trim())
                .isDefault(firstOfType)
                .build();
        return toDTO(repo.save(tpl));
    }

    @Transactional
    public ZaloMessageTemplateDTO update(Long id, SaveZaloMessageTemplateRequest req) {
        ZaloMessageTemplate tpl = findActive(id);
        tpl.setName(req.getName().trim());
        tpl.setTemplateId(req.getTemplateId().trim());
        return toDTO(repo.save(tpl));
    }

    @Transactional
    public ZaloMessageTemplateDTO setDefault(String type, Long id) {
        ZaloMessageTemplate tpl = findActive(id);
        if (!tpl.getTemplateType().equals(type)) {
            throw new BadRequestException(
                    messageService.getMessage("error.zaloTemplate.wrongType", type));
        }
        repo.clearDefaultForType(type);
        tpl.setDefault(true);
        return toDTO(repo.save(tpl));
    }

    @Transactional
    public void delete(Long id) {
        ZaloMessageTemplate tpl = findActive(id);
        tpl.softDelete();

        if (tpl.isDefault()) {
            // Promote the next non-deleted template of the same type as default.
            repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(tpl.getTemplateType())
                    .stream().findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        repo.save(next);
                    });
        }
        repo.save(tpl);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private ZaloMessageTemplate findActive(Long id) {
        return repo.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.zaloTemplate.notFound", id)));
    }

    private ZaloMessageTemplateDTO toDTO(ZaloMessageTemplate t) {
        return ZaloMessageTemplateDTO.builder()
                .id(t.getId())
                .name(t.getName())
                .templateType(t.getTemplateType())
                .templateId(t.getTemplateId())
                .isDefault(t.isDefault())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
