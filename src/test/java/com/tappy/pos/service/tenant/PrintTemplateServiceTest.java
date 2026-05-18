package com.tappy.pos.service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.pawn.PawnStampTemplateConfig;
import com.tappy.pos.model.dto.tenant.PrintTemplateDTO;
import com.tappy.pos.model.dto.tenant.ReceiptTemplateConfig;
import com.tappy.pos.model.dto.tenant.SavePrintTemplateRequest;
import com.tappy.pos.model.dto.tenant.StampTemplateConfig;
import com.tappy.pos.model.entity.tenant.PrintTemplate;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.PrintTemplateRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrintTemplateService Unit Tests")
class PrintTemplateServiceTest {

    @Mock private PrintTemplateRepository repo;
    @Mock private MessageService messageService;
    @Mock private TenantContext tenantContext;

    private PrintTemplateService service;

    @BeforeEach
    void setUp() {
        service = new PrintTemplateService(repo, new ObjectMapper(), messageService, tenantContext);
        // Stub messages whose content is asserted in exception tests
        lenient().when(messageService.getMessage(eq("error.printTemplate.wrongType"), any(Object[].class)))
                .thenReturn("Template does not belong to type RECEIPT");
        lenient().when(messageService.getMessage(eq("error.printTemplate.invalidConfigJson"), any(Object[].class)))
                .thenReturn("Invalid config JSON for type RECEIPT: error");
    }

    private PrintTemplate buildTemplate(String type, String name, boolean isDefault) {
        return PrintTemplate.builder()
                .templateType(type)
                .name(name)
                .configJson("{}")
                .isDefault(isDefault)
                .build();
    }

    // ── getTemplates ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTemplates: returns mapped DTOs for all templates of type")
    void getTemplates_returnsList() {
        PrintTemplate t1 = buildTemplate(PrintTemplateService.POS_RECEIPT, "Default", true);
        PrintTemplate t2 = buildTemplate(PrintTemplateService.POS_RECEIPT, "Custom", false);
        when(repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(PrintTemplateService.POS_RECEIPT))
                .thenReturn(List.of(t1, t2));

        List<PrintTemplateDTO> result = service.getTemplates(PrintTemplateService.POS_RECEIPT);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Default");
    }

    @Test
    @DisplayName("getTemplate: returns DTO for existing template")
    void getTemplate_found() {
        PrintTemplate t = buildTemplate(PrintTemplateService.POS_RECEIPT, "T1", true);
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        PrintTemplateDTO dto = service.getTemplate(1L);

        assertThat(dto.getName()).isEqualTo("T1");
    }

    @Test
    @DisplayName("getTemplate: throws when template not found")
    void getTemplate_notFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTemplate(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getReceiptConfig ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getReceiptConfig: returns defaults when no template exists")
    void getReceiptConfig_noTemplate() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.POS_RECEIPT))
                .thenReturn(Optional.empty());

        ReceiptTemplateConfig cfg = service.getReceiptConfig();

        assertThat(cfg).isNotNull();
        assertThat(cfg).isEqualTo(ReceiptTemplateConfig.defaults());
    }

    @Test
    @DisplayName("getReceiptConfig: deserializes config from stored template")
    void getReceiptConfig_fromTemplate() throws Exception {
        String json = new ObjectMapper().writeValueAsString(ReceiptTemplateConfig.defaults());
        PrintTemplate t = PrintTemplate.builder().templateType(PrintTemplateService.POS_RECEIPT)
                .name("R").configJson(json).isDefault(true).build();
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.POS_RECEIPT))
                .thenReturn(Optional.of(t));

        ReceiptTemplateConfig cfg = service.getReceiptConfig();

        assertThat(cfg).isNotNull();
    }

    // ── getStampConfig ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStampConfig: returns inventory defaults when no template and type is INVENTORY_STAMP")
    void getStampConfig_inventoryDefault() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.INVENTORY_STAMP))
                .thenReturn(Optional.empty());

        StampTemplateConfig cfg = service.getStampConfig(PrintTemplateService.INVENTORY_STAMP);

        assertThat(cfg).isNotNull();
        assertThat(cfg).isEqualTo(StampTemplateConfig.inventoryDefaults());
    }

    @Test
    @DisplayName("getStampConfig: returns product defaults when type is PRODUCT_STAMP")
    void getStampConfig_productDefault() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.PRODUCT_STAMP))
                .thenReturn(Optional.empty());

        StampTemplateConfig cfg = service.getStampConfig(PrintTemplateService.PRODUCT_STAMP);

        assertThat(cfg).isEqualTo(StampTemplateConfig.defaults());
    }

    // ── getPawnStampConfig ────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawnStampConfig: returns defaults when no template exists")
    void getPawnStampConfig_default() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.PAWN_STAMP))
                .thenReturn(Optional.empty());

        PawnStampTemplateConfig cfg = service.getPawnStampConfig();

        assertThat(cfg).isNotNull();
        assertThat(cfg).isEqualTo(PawnStampTemplateConfig.defaults());
    }

    // ── getDefaultConfigJson ──────────────────────────────────────────────────

    @Test
    @DisplayName("getDefaultConfigJson: returns stored JSON when default template exists")
    void getDefaultConfigJson_found() {
        PrintTemplate t = buildTemplate(PrintTemplateService.POS_RECEIPT, "R", true);
        t.setConfigJson("{\"paperWidth\":\"80mm\"}");
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.POS_RECEIPT))
                .thenReturn(Optional.of(t));

        String json = service.getDefaultConfigJson(PrintTemplateService.POS_RECEIPT);

        assertThat(json).contains("80mm");
    }

    @Test
    @DisplayName("getDefaultConfigJson: returns generated default when no template exists")
    void getDefaultConfigJson_notFound() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.PAWN_STAMP))
                .thenReturn(Optional.empty());

        String json = service.getDefaultConfigJson(PrintTemplateService.PAWN_STAMP);

        assertThat(json).isNotEmpty();
        assertThat(json).isNotEqualTo("{}");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: first template of type becomes default automatically")
    void create_firstTemplateIsDefault() {
        when(repo.existsByTemplateTypeAndDeletedFalse(PrintTemplateService.POS_RECEIPT)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Receipt");
        req.setConfigJson("{\"paperWidth\":\"80mm\"}");

        PrintTemplateDTO dto = service.create(PrintTemplateService.POS_RECEIPT, req);

        ArgumentCaptor<PrintTemplate> cap = ArgumentCaptor.forClass(PrintTemplate.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isDefault()).isTrue();
    }

    @Test
    @DisplayName("create: subsequent template of same type is NOT default")
    void create_subsequentTemplateNotDefault() {
        when(repo.existsByTemplateTypeAndDeletedFalse(PrintTemplateService.POS_RECEIPT)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Custom Receipt");
        req.setConfigJson("{\"paperWidth\":\"58mm\"}");

        service.create(PrintTemplateService.POS_RECEIPT, req);

        ArgumentCaptor<PrintTemplate> cap = ArgumentCaptor.forClass(PrintTemplate.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isDefault()).isFalse();
    }

    @Test
    @DisplayName("create: throws BadRequestException for invalid config JSON")
    void create_invalidJson() {
        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Bad");
        req.setConfigJson("not-json");

        assertThatThrownBy(() -> service.create(PrintTemplateService.POS_RECEIPT, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid config JSON");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: saves updated template")
    void update_success() {
        PrintTemplate existing = buildTemplate(PrintTemplateService.POS_RECEIPT, "Old Name", true);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("New Name");
        req.setConfigJson("{\"paperWidth\":\"80mm\"}");

        PrintTemplateDTO dto = service.update(1L, req);

        assertThat(dto.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("update: throws when template not found")
    void update_notFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("X");
        req.setConfigJson("{}");

        assertThatThrownBy(() -> service.update(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── setDefault ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setDefault: clears other defaults and marks this one")
    void setDefault_success() {
        PrintTemplate tpl = buildTemplate(PrintTemplateService.POS_RECEIPT, "Custom", false);
        when(repo.findById(1L)).thenReturn(Optional.of(tpl));
        doNothing().when(repo).clearDefaultForType(PrintTemplateService.POS_RECEIPT);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        PrintTemplateDTO dto = service.setDefault(PrintTemplateService.POS_RECEIPT, 1L);

        verify(repo).clearDefaultForType(PrintTemplateService.POS_RECEIPT);
        assertThat(tpl.isDefault()).isTrue();
    }

    @Test
    @DisplayName("setDefault: throws when template type doesn't match")
    void setDefault_typeMismatch() {
        PrintTemplate tpl = buildTemplate(PrintTemplateService.PRODUCT_STAMP, "Stamp", false);
        when(repo.findById(1L)).thenReturn(Optional.of(tpl));

        assertThatThrownBy(() -> service.setDefault(PrintTemplateService.POS_RECEIPT, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to type");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes and promotes next template as default")
    void delete_promotesNext() {
        PrintTemplate toDelete = buildTemplate(PrintTemplateService.POS_RECEIPT, "Default", true);
        PrintTemplate next = buildTemplate(PrintTemplateService.POS_RECEIPT, "Next", false);

        when(repo.findById(1L)).thenReturn(Optional.of(toDelete));
        when(repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(PrintTemplateService.POS_RECEIPT))
                .thenReturn(List.of(next));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.delete(1L);

        assertThat(toDelete.isDeleted()).isTrue();
        assertThat(next.isDefault()).isTrue();
    }

    @Test
    @DisplayName("delete: soft-deletes non-default without promoting")
    void delete_nonDefault() {
        PrintTemplate tpl = buildTemplate(PrintTemplateService.POS_RECEIPT, "Custom", false);
        when(repo.findById(1L)).thenReturn(Optional.of(tpl));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.delete(1L);

        assertThat(tpl.isDeleted()).isTrue();
        verify(repo, never()).findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(anyString());
    }

    // ── validateConfigJson: PRODUCT_STAMP / INVENTORY_STAMP / PAWN_STAMP / default ──

    @Test
    @DisplayName("create: accepts valid JSON for PRODUCT_STAMP type")
    void create_productStamp_validJson() {
        when(repo.existsByTemplateTypeAndDeletedFalse(PrintTemplateService.PRODUCT_STAMP)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Product Stamp");
        req.setConfigJson("{}");

        service.create(PrintTemplateService.PRODUCT_STAMP, req);

        verify(repo).save(any());
    }

    @Test
    @DisplayName("create: accepts valid JSON for INVENTORY_STAMP type")
    void create_inventoryStamp_validJson() {
        when(repo.existsByTemplateTypeAndDeletedFalse(PrintTemplateService.INVENTORY_STAMP)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Inventory Stamp");
        req.setConfigJson("{}");

        service.create(PrintTemplateService.INVENTORY_STAMP, req);

        verify(repo).save(any());
    }

    @Test
    @DisplayName("create: accepts valid JSON for PAWN_STAMP type")
    void create_pawnStamp_validJson() {
        when(repo.existsByTemplateTypeAndDeletedFalse(PrintTemplateService.PAWN_STAMP)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Pawn Stamp");
        req.setConfigJson("{}");

        service.create(PrintTemplateService.PAWN_STAMP, req);

        verify(repo).save(any());
    }

    @Test
    @DisplayName("create: accepts any valid JSON tree for unknown template types")
    void create_unknownType_acceptsValidJson() {
        when(repo.existsByTemplateTypeAndDeletedFalse("CUSTOM_TYPE")).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Custom");
        req.setConfigJson("{\"key\":\"value\"}");

        service.create("CUSTOM_TYPE", req);

        verify(repo).save(any());
    }

    // ── defaultJsonFor: POS_RECEIPT / INVENTORY_STAMP / default ──────────────

    @Test
    @DisplayName("getDefaultConfigJson: generates default JSON for POS_RECEIPT when no template")
    void getDefaultConfigJson_posReceipt_notFound() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.POS_RECEIPT))
                .thenReturn(Optional.empty());

        String json = service.getDefaultConfigJson(PrintTemplateService.POS_RECEIPT);

        assertThat(json).isNotEmpty();
        assertThat(json).isNotEqualTo("{}");
    }

    @Test
    @DisplayName("getDefaultConfigJson: generates default JSON for INVENTORY_STAMP when no template")
    void getDefaultConfigJson_inventoryStamp_notFound() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.INVENTORY_STAMP))
                .thenReturn(Optional.empty());

        String json = service.getDefaultConfigJson(PrintTemplateService.INVENTORY_STAMP);

        assertThat(json).isNotEmpty();
    }

    @Test
    @DisplayName("getDefaultConfigJson: uses StampTemplateConfig defaults for unknown type")
    void getDefaultConfigJson_unknownType_usesProductStampDefaults() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse("UNKNOWN_TYPE"))
                .thenReturn(Optional.empty());

        String json = service.getDefaultConfigJson("UNKNOWN_TYPE");

        assertThat(json).isNotEmpty();
    }

    // ── deserialize: exception path returns fallback ──────────────────────────

    @Test
    @DisplayName("getReceiptConfig: returns fallback defaults when stored JSON is unparseable")
    void getReceiptConfig_invalidJson_returnsFallback() {
        PrintTemplate t = PrintTemplate.builder()
                .templateType(PrintTemplateService.POS_RECEIPT)
                .name("R").configJson("not-valid-json").isDefault(true).build();
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.POS_RECEIPT))
                .thenReturn(Optional.of(t));

        ReceiptTemplateConfig cfg = service.getReceiptConfig();

        assertThat(cfg).isNotNull();
        assertThat(cfg).isEqualTo(ReceiptTemplateConfig.defaults());
    }

    @Test
    @DisplayName("getPawnStampConfig: returns fallback when stored JSON is unparseable")
    void getPawnStampConfig_invalidJson_returnsFallback() {
        PrintTemplate t = PrintTemplate.builder()
                .templateType(PrintTemplateService.PAWN_STAMP)
                .name("P").configJson("{invalid}").isDefault(true).build();
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(PrintTemplateService.PAWN_STAMP))
                .thenReturn(Optional.of(t));

        PawnStampTemplateConfig cfg = service.getPawnStampConfig();

        assertThat(cfg).isNotNull();
        assertThat(cfg).isEqualTo(PawnStampTemplateConfig.defaults());
    }
}
