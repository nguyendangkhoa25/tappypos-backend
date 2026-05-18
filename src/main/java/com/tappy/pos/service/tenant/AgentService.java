package com.tappy.pos.service.tenant;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.SaveVendorRequest;
import com.tappy.pos.model.dto.tenant.VendorDTO;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.tenant.Agent;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.AgentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public List<VendorDTO> getAll(String search) {
        return agentRepository.findAllActive(search).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public VendorDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    public VendorDTO create(SaveVendorRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException(messageService.getMessage("error.vendor.admin.name.required"));
        }
        Agent agent = Agent.builder()
                .name(request.getName().trim())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .notes(request.getNotes())
                .active(true)
                .build();
        Agent saved = agentRepository.save(agent);

        String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        String actorFullName = userRepository.findByUsernameTenantScoped(actorUsername)
                .map(u -> u.getFullName()).orElse(actorUsername);
        activityLogService.logAsync("master", actorUsername, actorFullName,
                ActivityAction.AGENT_CREATED, "AGENT", String.valueOf(saved.getId()),
                "Tạo đại lý: " + saved.getName(), null);
        String title = messageService.getMessage("notification.master.vendor.created.title");
        String msg = messageService.getMessage("notification.master.vendor.created.message",
                saved.getName(), actorUsername);
        notificationService.pushToRolesAsync(Notification.NotificationType.SYSTEM, title, msg,
                "VENDOR", saved.getId(), List.of("MASTER_TENANT"), null);

        return toDTO(saved);
    }

    public VendorDTO update(Long id, SaveVendorRequest request) {
        Agent agent = findOrThrow(id);
        if (request.getName() != null && !request.getName().isBlank()) {
            agent.setName(request.getName().trim());
        }
        agent.setContactEmail(request.getContactEmail());
        agent.setContactPhone(request.getContactPhone());
        agent.setNotes(request.getNotes());
        Agent updated = agentRepository.save(agent);

        String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync("master", actorUsername, null,
                ActivityAction.AGENT_UPDATED, "AGENT", String.valueOf(updated.getId()),
                "Cập nhật đại lý: " + updated.getName(), null);

        return toDTO(updated);
    }

    public void delete(Long id) {
        Agent agent = findOrThrow(id);
        agent.softDelete();
        agentRepository.save(agent);
    }

    private Agent findOrThrow(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.vendor.admin.not.found", id)));
    }

    private VendorDTO toDTO(Agent a) {
        return VendorDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .contactEmail(a.getContactEmail())
                .contactPhone(a.getContactPhone())
                .notes(a.getNotes())
                .active(a.getActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
