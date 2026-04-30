package com.knp.service.product;

import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.variant.SaveVariantTypeRequest;
import com.knp.model.dto.variant.VariantTypeDTO;
import com.knp.model.dto.variant.VariantTypeOptionDTO;
import com.knp.model.entity.product.ProductType;
import com.knp.model.entity.product.VariantType;
import com.knp.model.entity.product.VariantTypeOption;
import com.knp.repository.product.ProductTypeRepository;
import com.knp.repository.product.VariantTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VariantTypeService {

    private final VariantTypeRepository variantTypeRepository;
    private final ProductTypeRepository productTypeRepository;
    private final MessageService messageService;

    public List<VariantTypeDTO> getAll() {
        return variantTypeRepository.findAllActive().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<VariantTypeDTO> getForProductType(Long productTypeId) {
        return variantTypeRepository.findForProductType(productTypeId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public VariantTypeDTO getById(Long id) {
        return variantTypeRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.variantType.not.found", id)));
    }

    @Transactional
    public VariantTypeDTO create(SaveVariantTypeRequest req) {
        if (variantTypeRepository.existsByNameAndDeletedFalse(req.getName().trim())) {
            throw new BadRequestException(
                    messageService.getMessage("error.variantType.name.exists", req.getName()));
        }

        VariantType vt = VariantType.builder()
                .name(req.getName().trim())
                .description(req.getDescription())
                .productTypeId(req.getProductTypeId())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .options(new ArrayList<>())
                .build();

        buildOptions(vt, req.getOptions());
        VariantType saved = variantTypeRepository.save(vt);
        log.info("Variant type created: id={}, name={}", saved.getId(), saved.getName());
        return mapToDTO(saved);
    }

    @Transactional
    public VariantTypeDTO update(Long id, SaveVariantTypeRequest req) {
        VariantType vt = variantTypeRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.variantType.not.found", id)));

        vt.setName(req.getName().trim());
        vt.setDescription(req.getDescription());
        vt.setProductTypeId(req.getProductTypeId());
        if (req.getSortOrder() != null) vt.setSortOrder(req.getSortOrder());

        // Replace options
        vt.getOptions().clear();
        buildOptions(vt, req.getOptions());

        VariantType saved = variantTypeRepository.save(vt);
        log.info("Variant type updated: id={}, name={}", saved.getId(), saved.getName());
        return mapToDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        VariantType vt = variantTypeRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.variantType.not.found", id)));
        vt.softDelete();
        vt.getOptions().forEach(o -> o.softDelete());
        variantTypeRepository.save(vt);
        log.info("Variant type deleted: id={}, name={}", id, vt.getName());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void buildOptions(VariantType vt, List<String> values) {
        if (values == null || values.isEmpty()) return;
        List<String> distinct = values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        IntStream.range(0, distinct.size()).forEach(i -> {
            VariantTypeOption opt = VariantTypeOption.builder()
                    .variantType(vt)
                    .value(distinct.get(i))
                    .sortOrder(i)
                    .build();
            vt.getOptions().add(opt);
        });
    }

    private VariantTypeDTO mapToDTO(VariantType vt) {
        String productTypeName = null;
        if (vt.getProductTypeId() != null) {
            productTypeName = productTypeRepository.findById(vt.getProductTypeId())
                    .map(ProductType::getName).orElse(null);
        }
        List<VariantTypeOptionDTO> options = vt.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .map(o -> VariantTypeOptionDTO.builder()
                        .id(o.getId())
                        .value(o.getValue())
                        .sortOrder(o.getSortOrder())
                        .build())
                .collect(Collectors.toList());
        return VariantTypeDTO.builder()
                .id(vt.getId())
                .name(vt.getName())
                .description(vt.getDescription())
                .productTypeId(vt.getProductTypeId())
                .productTypeName(productTypeName)
                .sortOrder(vt.getSortOrder())
                .options(options)
                .build();
    }
}
