package com.tappy.pos.service.auth;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.auth.RoleDTO;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.repository.auth.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final MessageService messageService;

    /**
     * Get role by code
     */
    public RoleDTO getRoleByCode(String code) {
        log.info("Fetching role by code: {}", code);

        // Validate role code
        if (!RoleEnum.isValidRole(code)) {
            String errorMessage = messageService.getMessage("error.role.invalid", code);
            throw new BadRequestException(errorMessage);
        }

        Role role = roleRepository.findByName(code)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.role.not.found", code);
                    return new ResourceNotFoundException(errorMessage);
                });
        return mapToDTO(role);
    }

    /**
     * Get role by ID
     */
    public RoleDTO getRoleById(Long id) {
        log.info("Fetching role by id: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.role.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        return mapToDTO(role);
    }

    /**
     * Get all predefined roles
     */
    public List<RoleDTO> getAllRoles() {
        log.info("Fetching all predefined roles");
        return roleRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Initialize default predefined roles (called on application startup)
     * Only these 5 roles are available in the system
     */
    public void initializeDefaultRoles() {
        log.info("Initializing predefined roles");

        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (!roleRepository.existsByName(roleEnum.getCode())) {
                Role role = Role.builder()
                        .name(roleEnum.getCode())
                        .description(roleEnum.getDescription())
                        .build();
                roleRepository.save(role);
                log.info("Role created: {}", roleEnum.getCode());
            }
        }

        log.info("All predefined roles initialized successfully");
    }

    /**
     * Validate role code
     */
    public boolean isValidRole(String code) {
        return RoleEnum.isValidRole(code);
    }

    /**
     * Map Role entity to RoleDTO
     */
    private RoleDTO mapToDTO(Role role) {
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}

