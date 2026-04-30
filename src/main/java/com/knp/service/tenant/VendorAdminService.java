package com.knp.service.tenant;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.tenant.SaveVendorRequest;
import com.knp.model.dto.tenant.VendorDTO;
import com.knp.model.entity.tenant.VendorAdmin;
import com.knp.model.enums.ActivityAction;
import com.knp.repository.auth.UserRepository;
import com.knp.repository.tenant.VendorAdminRepository;
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
public class VendorAdminService {

    private final VendorAdminRepository vendorAdminRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public List<VendorDTO> getAll(String search) {
        return vendorAdminRepository.findAllActive(search).stream()
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
        VendorAdmin vendor = VendorAdmin.builder()
                .name(request.getName().trim())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .notes(request.getNotes())
                .active(true)
                .build();
        VendorAdmin saved = vendorAdminRepository.save(vendor);

        String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        String actorFullName = userRepository.findByUsername(actorUsername)
                .map(u -> u.getFullName()).orElse(actorUsername);
        activityLogService.logAsync("master", actorUsername, actorFullName,
                ActivityAction.VENDOR_CREATED, "VENDOR", String.valueOf(saved.getId()),
                "Created vendor: " + saved.getName(), null);
        String title = messageService.getMessage("notification.master.vendor.created.title");
        String msg = messageService.getMessage("notification.master.vendor.created.message",
                saved.getName(), actorUsername);
        notificationService.pushToMasterUsers(title, msg, "VENDOR", saved.getId());

        return toDTO(saved);
    }

    public VendorDTO update(Long id, SaveVendorRequest request) {
        VendorAdmin vendor = findOrThrow(id);
        if (request.getName() != null && !request.getName().isBlank()) {
            vendor.setName(request.getName().trim());
        }
        vendor.setContactEmail(request.getContactEmail());
        vendor.setContactPhone(request.getContactPhone());
        vendor.setNotes(request.getNotes());
        return toDTO(vendorAdminRepository.save(vendor));
    }

    public void delete(Long id) {
        VendorAdmin vendor = findOrThrow(id);
        vendor.softDelete();
        vendorAdminRepository.save(vendor);
    }

    private VendorAdmin findOrThrow(Long id) {
        return vendorAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.vendor.admin.not.found", id)));
    }

    private VendorDTO toDTO(VendorAdmin v) {
        return VendorDTO.builder()
                .id(v.getId())
                .name(v.getName())
                .contactEmail(v.getContactEmail())
                .contactPhone(v.getContactPhone())
                .notes(v.getNotes())
                .active(v.getActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
