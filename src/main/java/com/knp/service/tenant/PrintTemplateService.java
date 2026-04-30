package com.knp.service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.pawn.PawnStampTemplateConfig;
import com.knp.model.dto.tenant.PrintTemplateDTO;
import com.knp.model.dto.tenant.ReceiptTemplateConfig;
import com.knp.model.dto.tenant.SavePrintTemplateRequest;
import com.knp.model.dto.tenant.StampTemplateConfig;
import com.knp.model.entity.tenant.PrintTemplate;
import com.knp.repository.tenant.PrintTemplateRepository;
import com.knp.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrintTemplateService {

    public static final String POS_RECEIPT     = "POS_RECEIPT";
    public static final String PRODUCT_STAMP   = "PRODUCT_STAMP";
    public static final String INVENTORY_STAMP = "INVENTORY_STAMP";
    public static final String PAWN_STAMP      = "PAWN_STAMP";

    private final PrintTemplateRepository repo;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    // ── queries ────────────────────────────────────────────────────────────

    public List<PrintTemplateDTO> getTemplates(String type) {
        return repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(type)
                .stream().map(this::toDTO).toList();
    }

    public PrintTemplateDTO getTemplate(Long id) {
        return toDTO(findActive(id));
    }

    public ReceiptTemplateConfig getReceiptConfig() {
        return repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(POS_RECEIPT)
                .map(t -> deserialize(t.getConfigJson(), ReceiptTemplateConfig.class, ReceiptTemplateConfig.defaults()))
                .orElseGet(ReceiptTemplateConfig::defaults);
    }

    public StampTemplateConfig getStampConfig(String type) {
        StampTemplateConfig fallback = INVENTORY_STAMP.equals(type)
                ? StampTemplateConfig.inventoryDefaults() : StampTemplateConfig.defaults();
        return repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(type)
                .map(t -> deserialize(t.getConfigJson(), StampTemplateConfig.class, fallback))
                .orElse(fallback);
    }

    public PawnStampTemplateConfig getPawnStampConfig() {
        return repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PAWN_STAMP)
                .map(t -> deserialize(t.getConfigJson(), PawnStampTemplateConfig.class, PawnStampTemplateConfig.defaults()))
                .orElseGet(PawnStampTemplateConfig::defaults);
    }

    // ── mutations ──────────────────────────────────────────────────────────

    @Transactional
    public PrintTemplateDTO create(String type, SavePrintTemplateRequest req) {
        validateConfigJson(type, req.getConfigJson());
        boolean firstOfType = !repo.existsByTemplateTypeAndDeletedFalse(type);
        PrintTemplate tpl = PrintTemplate.builder()
                .templateType(type)
                .name(req.getName().trim())
                .configJson(req.getConfigJson())
                .isDefault(firstOfType)
                .build();
        return toDTO(repo.save(tpl));
    }

    @Transactional
    public PrintTemplateDTO update(Long id, SavePrintTemplateRequest req) {
        validateConfigJson(findActive(id).getTemplateType(), req.getConfigJson());
        PrintTemplate tpl = findActive(id);
        tpl.setName(req.getName().trim());
        tpl.setConfigJson(req.getConfigJson());
        return toDTO(repo.save(tpl));
    }

    @Transactional
    public PrintTemplateDTO setDefault(String type, Long id) {
        PrintTemplate tpl = findActive(id);
        if (!tpl.getTemplateType().equals(type)) {
            throw new BadRequestException(messageService.getMessage("error.printTemplate.wrongType", new Object[]{type}));
        }
        repo.clearDefaultForType(type);
        tpl.setDefault(true);
        return toDTO(repo.save(tpl));
    }

    @Transactional
    public void delete(Long id) {
        PrintTemplate tpl = findActive(id);
        tpl.softDelete();

        if (tpl.isDefault()) {
            repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(tpl.getTemplateType())
                    .stream().findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        repo.save(next);
                    });
        }
        repo.save(tpl);
    }

    // ── legacy single-template access (used by preview endpoint) ──────────

    public String getDefaultConfigJson(String type) {
        return repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(type)
                .map(PrintTemplate::getConfigJson)
                .orElseGet(() -> defaultJsonFor(type));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private PrintTemplate findActive(Long id) {
        return repo.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Print template not found: " + id));
    }

    private PrintTemplateDTO toDTO(PrintTemplate t) {
        return PrintTemplateDTO.builder()
                .id(t.getId())
                .templateType(t.getTemplateType())
                .name(t.getName())
                .configJson(t.getConfigJson())
                .isDefault(t.isDefault())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private <T> T deserialize(String json, Class<T> cls, T fallback) {
        try { return objectMapper.readValue(json, cls); }
        catch (Exception e) {
            log.warn("Failed to deserialize {}: {}", cls.getSimpleName(), e.getMessage());
            return fallback;
        }
    }

    private String defaultJsonFor(String type) {
        try {
            Object config = switch (type) {
                case POS_RECEIPT     -> ReceiptTemplateConfig.defaults();
                case INVENTORY_STAMP -> StampTemplateConfig.inventoryDefaults();
                case PAWN_STAMP      -> PawnStampTemplateConfig.defaults();
                default              -> StampTemplateConfig.defaults();
            };
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void validateConfigJson(String type, String json) {
        try {
            switch (type) {
                case POS_RECEIPT     -> objectMapper.readValue(json, ReceiptTemplateConfig.class);
                case PRODUCT_STAMP,
                     INVENTORY_STAMP -> objectMapper.readValue(json, StampTemplateConfig.class);
                case PAWN_STAMP      -> objectMapper.readValue(json, PawnStampTemplateConfig.class);
                default              -> objectMapper.readTree(json);
            }
        } catch (Exception e) {
            throw new BadRequestException(messageService.getMessage("error.printTemplate.invalidConfigJson", new Object[]{type, e.getMessage()}));
        }
    }
}
