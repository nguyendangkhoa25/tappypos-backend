package com.tappy.pos.service.vendor;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.vendor.SaveVendorRequest;
import com.tappy.pos.model.dto.vendor.VendorDTO;
import com.tappy.pos.model.entity.vendor.Vendor;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;

    public Page<VendorDTO> getAll(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return vendorRepository.search(keyword.trim(), pageable).map(this::mapToDTO);
        }
        return vendorRepository.findAllActive(pageable).map(this::mapToDTO);
    }

    public VendorDTO getById(Long id) {
        return vendorRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.vendor.not.found", id)));
    }

    public List<VendorDTO> getAllForSelect() {
        return vendorRepository.findAllActiveForSelect().stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public VendorDTO create(SaveVendorRequest req) {
        String code = req.getCode().toUpperCase().trim();
        if (vendorRepository.findByCode(code).isPresent()) {
            throw new BadRequestException(messageService.getMessage("error.vendor.code.exists", code));
        }
        Vendor vendor = Vendor.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name(req.getName().trim())
                .code(code)
                .contactName(req.getContactName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .taxId(req.getTaxId())
                .paymentTerms(req.getPaymentTerms() != null ? req.getPaymentTerms() : "NET_30")
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .notes(req.getNotes())
                .build();
        Vendor saved = vendorRepository.save(vendor);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.VENDOR_CREATED, "VENDOR", String.valueOf(saved.getId()),
                "Thêm nhà cung cấp: " + saved.getName(), null);
        return mapToDTO(saved);
    }

    @Transactional
    public VendorDTO update(Long id, SaveVendorRequest req) {
        Vendor vendor = vendorRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.vendor.not.found", id)));

        String newCode = req.getCode().toUpperCase().trim();
        if (!newCode.equals(vendor.getCode())) {
            vendorRepository.findByCode(newCode).ifPresent(v -> {
                throw new BadRequestException(messageService.getMessage("error.vendor.code.exists", newCode));
            });
            vendor.setCode(newCode);
        }

        vendor.setName(req.getName().trim());
        if (req.getContactName() != null) vendor.setContactName(req.getContactName());
        if (req.getEmail() != null) vendor.setEmail(req.getEmail());
        if (req.getPhone() != null) vendor.setPhone(req.getPhone());
        if (req.getAddress() != null) vendor.setAddress(req.getAddress());
        if (req.getTaxId() != null) vendor.setTaxId(req.getTaxId());
        if (req.getPaymentTerms() != null) vendor.setPaymentTerms(req.getPaymentTerms());
        if (req.getIsActive() != null) vendor.setIsActive(req.getIsActive());
        if (req.getNotes() != null) vendor.setNotes(req.getNotes());

        Vendor updated = vendorRepository.save(vendor);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.VENDOR_UPDATED, "VENDOR", String.valueOf(updated.getId()),
                "Cập nhật nhà cung cấp: " + updated.getName(), null);
        return mapToDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.vendor.not.found", id)));
        vendor.softDelete();
        vendorRepository.save(vendor);
    }

    public VendorDTO mapToDTO(Vendor v) {
        return VendorDTO.builder()
                .id(v.getId())
                .name(v.getName())
                .code(v.getCode())
                .contactName(v.getContactName())
                .email(v.getEmail())
                .phone(v.getPhone())
                .address(v.getAddress())
                .taxId(v.getTaxId())
                .paymentTerms(v.getPaymentTerms())
                .isActive(v.getIsActive())
                .notes(v.getNotes())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
