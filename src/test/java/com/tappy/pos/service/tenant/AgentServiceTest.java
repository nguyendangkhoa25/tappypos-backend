package com.tappy.pos.service.tenant;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.SaveVendorRequest;
import com.tappy.pos.model.dto.tenant.VendorDTO;
import com.tappy.pos.model.entity.tenant.Agent;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.AgentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentService Unit Tests")
class AgentServiceTest {

    @Mock private AgentRepository agentRepository;
    @Mock private MessageService messageService;
    @Mock private UserRepository userRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AgentService agentService;

    private Agent agent;
    private SaveVendorRequest saveRequest;

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("masteradmin");
        SecurityContextHolder.setContext(securityContext);

        agent = Agent.builder()
                .name("Đại lý Hà Nội")
                .contactEmail("hanoi@example.com")
                .contactPhone("0901234567")
                .notes("Đại lý khu vực Hà Nội")
                .active(true)
                .build();
        agent.setId(1L);
        agent.setDeleted(false);

        saveRequest = new SaveVendorRequest();
        saveRequest.setName("Đại lý Hà Nội");
        saveRequest.setContactEmail("hanoi@example.com");
        saveRequest.setContactPhone("0901234567");
        saveRequest.setNotes("Đại lý khu vực Hà Nội");

        lenient().when(messageService.getMessage(anyString())).thenReturn("title or message");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("error");

        User user = User.builder().fullName("Master Admin").build();
        lenient().when(userRepository.findByUsernameTenantScoped("masteradmin"))
                .thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll returns all active agents")
    void testGetAll() {
        when(agentRepository.findAllActive(null)).thenReturn(List.of(agent));

        List<VendorDTO> result = agentService.getAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Đại lý Hà Nội");
    }

    @Test
    @DisplayName("getAll with search forwards search term to repository")
    void testGetAll_WithSearch() {
        when(agentRepository.findAllActive("Hà Nội")).thenReturn(List.of(agent));

        List<VendorDTO> result = agentService.getAll("Hà Nội");

        assertThat(result).hasSize(1);
        verify(agentRepository).findAllActive("Hà Nội");
    }

    @Test
    @DisplayName("getAll returns empty list when no agents found")
    void testGetAll_Empty() {
        when(agentRepository.findAllActive(null)).thenReturn(List.of());

        List<VendorDTO> result = agentService.getAll(null);

        assertThat(result).isEmpty();
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns DTO for existing agent")
    void testGetById_Success() {
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));

        VendorDTO result = agentService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Đại lý Hà Nội");
        assertThat(result.getContactEmail()).isEqualTo("hanoi@example.com");
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for missing agent")
    void testGetById_NotFound() {
        when(agentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create saves agent and fires async side-effects")
    void testCreate_Success() {
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        VendorDTO result = agentService.create(saveRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Đại lý Hà Nội");
        verify(agentRepository).save(any(Agent.class));
        verify(activityLogService).logAsync(anyString(), anyString(), anyString(),
                any(), anyString(), anyString(), anyString(), any());
        verify(notificationService).pushToRolesAsync(any(), anyString(), anyString(),
                anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("create trims agent name before saving")
    void testCreate_TrimsName() {
        saveRequest.setName("  Đại lý HCM  ");
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        agentService.create(saveRequest);

        verify(agentRepository).save(argThat(a -> "Đại lý HCM".equals(a.getName())));
    }

    @Test
    @DisplayName("create throws BadRequestException when name is null")
    void testCreate_NullName() {
        saveRequest.setName(null);

        assertThatThrownBy(() -> agentService.create(saveRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create throws BadRequestException when name is blank")
    void testCreate_BlankName() {
        saveRequest.setName("   ");

        assertThatThrownBy(() -> agentService.create(saveRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create uses fallback username when user not found in repo")
    void testCreate_UserNotFoundFallback() {
        when(userRepository.findByUsernameTenantScoped("masteradmin")).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        agentService.create(saveRequest);

        verify(activityLogService).logAsync(anyString(), eq("masteradmin"), eq("masteradmin"),
                any(), anyString(), anyString(), anyString(), any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update saves agent with new fields")
    void testUpdate_Success() {
        saveRequest.setName("Đại lý HCM");
        saveRequest.setContactEmail("hcm@example.com");

        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        VendorDTO result = agentService.update(1L, saveRequest);

        assertThat(result).isNotNull();
        verify(agentRepository).save(argThat(a -> "Đại lý HCM".equals(a.getName())));
    }

    @Test
    @DisplayName("update does not change name when new name is blank")
    void testUpdate_BlankName_KeepsOldName() {
        saveRequest.setName("   ");
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        agentService.update(1L, saveRequest);

        verify(agentRepository).save(argThat(a -> "Đại lý Hà Nội".equals(a.getName())));
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for missing agent")
    void testUpdate_NotFound() {
        when(agentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.update(99L, saveRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete soft-deletes an agent")
    void testDelete_Success() {
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        agentService.delete(1L);

        assertThat(agent.isDeleted()).isTrue();
        verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException for missing agent")
    void testDelete_NotFound() {
        when(agentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── DTO mapping ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("toDTO maps all agent fields correctly")
    void testToDTO_AllFields() {
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));

        VendorDTO dto = agentService.getById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Đại lý Hà Nội");
        assertThat(dto.getContactEmail()).isEqualTo("hanoi@example.com");
        assertThat(dto.getContactPhone()).isEqualTo("0901234567");
        assertThat(dto.getNotes()).isEqualTo("Đại lý khu vực Hà Nội");
        assertThat(dto.getActive()).isTrue();
    }
}
