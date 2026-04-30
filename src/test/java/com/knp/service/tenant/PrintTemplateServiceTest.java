package com.knp.service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.tenant.PrintTemplateDTO;
import com.knp.model.dto.tenant.SavePrintTemplateRequest;
import com.knp.model.entity.tenant.PrintTemplate;
import com.knp.repository.tenant.PrintTemplateRepository;
import com.knp.service.MessageService;
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

    private PrintTemplateService service;

    @BeforeEach
    void setUp() {
        service = new PrintTemplateService(repo, new ObjectMapper(), messageService);
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

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: first template of type becomes default automatically")
    void create_firstTemplateIsDefault() {
        when(repo.existsByTemplateTypeAndDeletedFalse(PrintTemplateService.POS_RECEIPT)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName("Receipt");
        req.setConfigJson("{\"paperSize\":\"80mm\"}");

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
        req.setConfigJson("{\"paperSize\":\"58mm\"}");

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
        req.setConfigJson("{\"paperSize\":\"80mm\"}");

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
}
