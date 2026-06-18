package com.tappy.pos.service.tenant;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.SaveZaloMessageTemplateRequest;
import com.tappy.pos.model.dto.tenant.ZaloMessageTemplateDTO;
import com.tappy.pos.model.entity.tenant.ZaloMessageTemplate;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.ZaloMessageTemplateRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ZaloMessageTemplateService Unit Tests")
class ZaloMessageTemplateServiceTest {

    @Mock private ZaloMessageTemplateRepository repo;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @InjectMocks
    private ZaloMessageTemplateService service;

    private static final String TYPE = ZaloMessageTemplateService.APPOINTMENT_REMINDER;

    @BeforeEach
    void setUp() {
        when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        ReflectionTestUtils.setField(service, "globalAppointmentReminderTemplateId", "GLOBAL-TPL");
    }

    private ZaloMessageTemplate tpl(long id, boolean isDefault) {
        ZaloMessageTemplate t = ZaloMessageTemplate.builder()
                .name("Nhắc lịch").templateType(TYPE).templateId("ZNS-" + id).isDefault(isDefault).build();
        t.setId(id);
        return t;
    }

    @Test
    @DisplayName("list maps active templates to DTOs")
    void list() {
        when(repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(TYPE))
                .thenReturn(List.of(tpl(1, true)));
        List<ZaloMessageTemplateDTO> result = service.list(TYPE);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTemplateId()).isEqualTo("ZNS-1");
        assertThat(result.get(0).isDefault()).isTrue();
    }

    @Test
    @DisplayName("getById returns active template")
    void getById() {
        when(repo.findById(1L)).thenReturn(Optional.of(tpl(1, false)));
        assertThat(service.getById(1L).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById throws for missing/deleted template")
    void getById_notFound() {
        when(repo.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById treats a soft-deleted template as missing")
    void getById_deleted() {
        ZaloMessageTemplate t = tpl(3, false);
        t.softDelete();
        when(repo.findById(3L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.getById(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getDefaultTemplateId prefers the tenant default")
    void getDefaultTemplateId_tenant() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(TYPE))
                .thenReturn(Optional.of(tpl(1, true)));
        assertThat(service.getDefaultTemplateId(TYPE)).isEqualTo("ZNS-1");
    }

    @Test
    @DisplayName("getDefaultTemplateId falls back to the global id for reminders")
    void getDefaultTemplateId_globalFallback() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(TYPE)).thenReturn(Optional.empty());
        assertThat(service.getDefaultTemplateId(TYPE)).isEqualTo("GLOBAL-TPL");
    }

    @Test
    @DisplayName("getDefaultTemplateId returns null for a non-reminder type with no tenant default")
    void getDefaultTemplateId_noneForOtherType() {
        when(repo.findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse("OTHER")).thenReturn(Optional.empty());
        assertThat(service.getDefaultTemplateId("OTHER")).isNull();
    }

    @Test
    @DisplayName("create marks the first template of a type as default")
    void create_first() {
        when(repo.existsByTemplateTypeAndDeletedFalse(TYPE)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        SaveZaloMessageTemplateRequest req = new SaveZaloMessageTemplateRequest();
        req.setName("  Nhắc lịch  ");
        req.setTemplateId("  ZNS-9  ");

        ZaloMessageTemplateDTO dto = service.create(TYPE, req);

        assertThat(dto.getName()).isEqualTo("Nhắc lịch");
        assertThat(dto.getTemplateId()).isEqualTo("ZNS-9");
        assertThat(dto.isDefault()).isTrue();
    }

    @Test
    @DisplayName("create does not mark a subsequent template as default")
    void create_subsequent() {
        when(repo.existsByTemplateTypeAndDeletedFalse(TYPE)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        SaveZaloMessageTemplateRequest req = new SaveZaloMessageTemplateRequest();
        req.setName("X");
        req.setTemplateId("Y");

        assertThat(service.create(TYPE, req).isDefault()).isFalse();
    }

    @Test
    @DisplayName("update changes name and templateId")
    void update() {
        when(repo.findById(1L)).thenReturn(Optional.of(tpl(1, false)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        SaveZaloMessageTemplateRequest req = new SaveZaloMessageTemplateRequest();
        req.setName(" New ");
        req.setTemplateId(" NEW-ID ");

        ZaloMessageTemplateDTO dto = service.update(1L, req);

        assertThat(dto.getName()).isEqualTo("New");
        assertThat(dto.getTemplateId()).isEqualTo("NEW-ID");
    }

    @Test
    @DisplayName("setDefault clears the existing default and promotes the chosen one")
    void setDefault() {
        ZaloMessageTemplate t = tpl(1, false);
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ZaloMessageTemplateDTO dto = service.setDefault(TYPE, 1L);

        verify(repo).clearDefaultForType(TYPE);
        assertThat(t.isDefault()).isTrue();
        assertThat(dto.isDefault()).isTrue();
    }

    @Test
    @DisplayName("setDefault rejects a template of a different type")
    void setDefault_wrongType() {
        ZaloMessageTemplate t = tpl(1, false);
        t.setTemplateType("OTHER");
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.setDefault(TYPE, 1L)).isInstanceOf(BadRequestException.class);
        verify(repo, never()).clearDefaultForType(anyString());
    }

    @Test
    @DisplayName("delete of the default promotes the next template")
    void delete_defaultPromotesNext() {
        ZaloMessageTemplate def = tpl(1, true);
        ZaloMessageTemplate next = tpl(2, false);
        when(repo.findById(1L)).thenReturn(Optional.of(def));
        when(repo.findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(TYPE))
                .thenReturn(List.of(next));

        service.delete(1L);

        assertThat(def.isDeleted()).isTrue();
        assertThat(next.isDefault()).isTrue();
        verify(repo).save(next);
        verify(repo).save(def);
    }

    @Test
    @DisplayName("delete of a non-default template does not promote others")
    void delete_nonDefault() {
        ZaloMessageTemplate t = tpl(1, false);
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        service.delete(1L);

        assertThat(t.isDeleted()).isTrue();
        verify(repo, never()).findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(anyString());
    }
}
