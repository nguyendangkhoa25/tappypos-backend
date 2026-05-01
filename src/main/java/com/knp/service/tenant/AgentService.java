package com.knp.service.tenant;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.tenant.SaveVendorRequest;
import com.knp.model.dto.tenant.VendorDTO;
import com.knp.model.entity.tenant.Agent;
import com.knp.model.enums.ActivityAction;
import com.knp.repository.auth.UserRepository;
import com.knp.repository.tenant.AgentRepository;
import com.knp.service.MessageService;
import com.knp.service.audit.ActivityLogService;
import com.knp.service.notification.NotificationService;
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
        String actorFullName = userRepository.findByUsername(actorUsername)
                .map(u -> u.getFullName()).orElse(actorUsername);
        activityLogService.logAsync("master", actorUsername, actorFullName,
                ActivityAction.VENDOR_CREATED, "VENDOR", String.valueOf(saved.getId()),
                "Created agent: " + saved.getName(), null);
        String title = messageService.getMessage("notification.master.vendor.created.title");
        String msg = messageService.getMessage("notification.master.vendor.created.message",
                saved.getName(), actorUsername);
        notificationService.pushToMasterUsers(title, msg, "VENDOR", saved.getId());

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
        return toDTO(agentRepository.save(agent));
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
