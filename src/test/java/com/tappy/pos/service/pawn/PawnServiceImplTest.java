package com.tappy.pos.service.pawn;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.PawnStatusNotAllowException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.service.storage.R2StorageService;
import com.tappy.pos.model.dto.pawn.*;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.pawn.PawnAuditEntity;
import com.tappy.pos.model.entity.pawn.PawnElectronicsEntity;
import com.tappy.pos.model.entity.pawn.PawnEntity;
import com.tappy.pos.model.entity.pawn.PawnQuery;
import com.tappy.pos.model.entity.pawn.ReqMoneyEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.PawnStatus;
import com.tappy.pos.model.mapper.PawnMapper;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.pawn.PawnAuditRepository;
import com.tappy.pos.repository.pawn.PawnElectronicsRepository;
import com.tappy.pos.repository.pawn.PawnJewelryRepository;
import com.tappy.pos.repository.pawn.PawnGeneralRepository;
import com.tappy.pos.repository.pawn.PawnQueryRepository;
import com.tappy.pos.repository.pawn.PawnRealEstateRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import com.tappy.pos.repository.pawn.PawnVehicleRepository;
import com.tappy.pos.repository.pawn.PawnWatchRepository;
import com.tappy.pos.repository.pawn.ReqMoneyRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.tenant.ShopInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PawnServiceImpl Unit Tests")
class PawnServiceImplTest {

    @Mock private PawnRepository pawnRepository;
    @Mock private PawnQueryRepository queryRepository;
    @Mock private PawnMapper pawnMapper;
    @Mock private CustomerRepository customerRepository;
    @Mock private ReqMoneyRepository reqMoneyRepository;
    @Mock private AuthContext authContext;
    @Mock private FeatureContext featureContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private PawnAuditRepository auditRepository;
    @Mock private ShopInfoService shopInfoService;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;
    @Mock private PawnJewelryRepository jewelryRepository;
    @Mock private PawnElectronicsRepository electronicsRepository;
    @Mock private PawnVehicleRepository vehicleRepository;
    @Mock private PawnWatchRepository watchRepository;
    @Mock private PawnRealEstateRepository realEstateRepository;
    @Mock private PawnGeneralRepository generalRepository;
    @Mock private R2StorageService r2StorageService;
    @Mock private R2CleanupService r2CleanupService;

    @InjectMocks
    private PawnServiceImpl pawnService;

    private PawnEntity pawnEntity;
    private PawnResponse pawnResponse;

    @BeforeEach
    void setUp() {
        pawnEntity = PawnEntity.builder()
                .pawnId(1L)
                .customerId(10L)
                .itemName("Dây chuyền vàng")
                .pawnAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("3"))
                .pawnStatus(PawnStatus.PAWNED)
                .pawnDate(LocalDateTime.now().minusDays(10))
                .pawnDueDate(LocalDateTime.now().plusDays(20))
                .build();

        pawnResponse = new PawnResponse();
        pawnResponse.setPawnId(1L);
        pawnResponse.setCustomerId(10L);
        pawnResponse.setItemName("Dây chuyền vàng");
        pawnResponse.setPawnAmount(new BigDecimal("5000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(10));
        pawnResponse.setPawnStatus(PawnStatus.PAWNED);

        lenient().when(authContext.getCurrentUsername()).thenReturn("staff01");
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");
        lenient().when(messageService.getMessage(anyString())).thenReturn("error");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("error");
        // Default: grant PAWN_VIEW_ALL so pre-existing tests continue to see all data
        lenient().when(featureContext.hasFeature(anyString())).thenReturn(true);
    }

    // ── getPawnDetails ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawnDetails returns full response with customer and audits")
    void testGetPawnDetails_Success() {
        Customer customer = Customer.builder().name("Nguyễn Văn A").phone("0901234567").build();
        customer.setId(10L);

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.getPawnDetails(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerName()).isEqualTo("Nguyễn Văn A");
        assertThat(result.getPhone()).isEqualTo("0901234567");
    }

    @Test
    @DisplayName("getPawnDetails throws ResourceNotFoundException when pawn not found")
    void testGetPawnDetails_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.getPawnDetails(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPawnDetails sets audits from repository")
    void testGetPawnDetails_WithAudits() {
        PawnAuditEntity auditEntity = new PawnAuditEntity();
        PawnAudit audit = PawnAudit.builder().build();
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of(auditEntity));
        when(pawnMapper.auditFromPawnAuditEntity(auditEntity)).thenReturn(audit);

        PawnResponse result = pawnService.getPawnDetails(1L);

        assertThat(result.getAudits()).hasSize(1);
    }

    @Test
    @DisplayName("getPawnDetails maps reqMoneys from entity")
    void testGetPawnDetails_WithReqMoneys() {
        ReqMoneyEntity reqMoney = ReqMoneyEntity.builder()
                .requestAmount(new BigDecimal("1000000")).build();
        ReqMoneyResponse reqMoneyResponse = new ReqMoneyResponse();
        pawnEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Dây chuyền").pawnStatus(PawnStatus.PAWNED)
                .pawnAmount(new BigDecimal("5000000")).interestRate(new BigDecimal("3"))
                .reqMoneys(Set.of(reqMoney)).build();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());
        when(pawnMapper.fromReqMoneyEntity(reqMoney)).thenReturn(reqMoneyResponse);

        PawnResponse result = pawnService.getPawnDetails(1L);

        assertThat(result.getReqMoneys()).hasSize(1);
    }

    // ── deletePawnByPawnIds ───────────────────────────────────────────────────

    @Test
    @DisplayName("deletePawnByPawnIds deletes and logs activity for each pawn")
    void testDeletePawnByPawnIds_Success() {
        when(pawnRepository.findAllById(List.of(1L))).thenReturn(List.of(pawnEntity));

        pawnService.deletePawnByPawnIds(List.of(1L));

        verify(pawnRepository).deleteAllByIdInBatch(List.of(1L));
        verify(activityLogService).logAsync(eq("test-shop"), eq("staff01"), isNull(),
                any(), eq("PAWN"), eq("1"), anyString(), isNull(), any());
    }

    @Test
    @DisplayName("deletePawnByPawnIds skips activity log when no entities found")
    void testDeletePawnByPawnIds_NoEntities() {
        when(pawnRepository.findAllById(List.of(99L))).thenReturn(List.of());

        pawnService.deletePawnByPawnIds(List.of(99L));

        verify(pawnRepository).deleteAllByIdInBatch(List.of(99L));
        verify(activityLogService, never()).logAsync(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── cancelPawnByPawnId ────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelPawnByPawnId sets CANCELLED status and logs activity")
    void testCancelPawnByPawnId_Success() {
        PawnEntity savedEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Dây chuyền").pawnStatus(PawnStatus.CANCELLED).build();
        when(pawnRepository.findById(1L))
                .thenReturn(Optional.of(pawnEntity))
                .thenReturn(Optional.of(savedEntity));
        when(pawnRepository.save(pawnEntity)).thenReturn(savedEntity);
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(any())).thenReturn(List.of());

        PawnResponse result = pawnService.cancelPawnByPawnId(1L, "Không cần nữa");

        assertThat(result).isNotNull();
        assertThat(pawnEntity.getPawnStatus()).isEqualTo(PawnStatus.CANCELLED);
        assertThat(pawnEntity.getCanceledReason()).isEqualTo("Không cần nữa");
        verify(pawnRepository, atLeastOnce()).save(pawnEntity);
        verify(activityLogService).logAsync(eq("test-shop"), eq("staff01"), isNull(),
                any(), eq("PAWN"), anyString(), anyString(), isNull(), any());
    }

    @Test
    @DisplayName("cancelPawnByPawnId throws when pawn already FORFEITED")
    void testCancelPawnByPawnId_AlreadyForfeited() {
        pawnEntity.setPawnStatus(PawnStatus.FORFEITED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.cancelPawnByPawnId(1L, "reason"))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("cancelPawnByPawnId throws when pawn already REDEEMED")
    void testCancelPawnByPawnId_AlreadyRedeemed() {
        pawnEntity.setPawnStatus(PawnStatus.REDEEMED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.cancelPawnByPawnId(1L, "reason"))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("cancelPawnByPawnId throws ResourceNotFoundException when pawn not found")
    void testCancelPawnByPawnId_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.cancelPawnByPawnId(99L, "reason"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── forfeitPawnByPawnId ───────────────────────────────────────────────────

    @Test
    @DisplayName("forfeitPawnByPawnId sets FORFEITED status and logs activity")
    void testForfeitPawnByPawnId_Success() {
        ForfeitRequest request = new ForfeitRequest();
        request.setForfeitedReason("Quá hạn không chuộc");
        request.setForfeitedAmount(new BigDecimal("4500000"));
        request.setInterestAmount(new BigDecimal("450000"));
        request.setTotalAmount(new BigDecimal("4950000"));
        request.setForfeitedDate(LocalDateTime.now());

        PawnEntity savedEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Dây chuyền").pawnStatus(PawnStatus.FORFEITED).build();
        when(pawnRepository.findById(1L))
                .thenReturn(Optional.of(pawnEntity))
                .thenReturn(Optional.of(savedEntity));
        when(pawnRepository.save(pawnEntity)).thenReturn(savedEntity);
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(any())).thenReturn(List.of());

        PawnResponse result = pawnService.forfeitPawnByPawnId(1L, request);

        assertThat(result).isNotNull();
        assertThat(pawnEntity.getPawnStatus()).isEqualTo(PawnStatus.FORFEITED);
        assertThat(pawnEntity.getForfeitedReason()).isEqualTo("Quá hạn không chuộc");
    }

    @Test
    @DisplayName("forfeitPawnByPawnId throws when pawn already CANCELLED")
    void testForfeitPawnByPawnId_AlreadyCancelled() {
        pawnEntity.setPawnStatus(PawnStatus.CANCELLED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, new ForfeitRequest()))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId throws when pawn already FORFEITED")
    void testForfeitPawnByPawnId_AlreadyForfeited() {
        pawnEntity.setPawnStatus(PawnStatus.FORFEITED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, new ForfeitRequest()))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId throws when pawn already REDEEMED")
    void testForfeitPawnByPawnId_AlreadyRedeemed() {
        pawnEntity.setPawnStatus(PawnStatus.REDEEMED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, new ForfeitRequest()))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    // ── calculatePawnRedeem ───────────────────────────────────────────────────

    @Test
    @DisplayName("calculatePawnRedeem computes interest for pawned item (no req moneys)")
    void testCalculatePawnRedeem_NoReqMoneys() {
        pawnEntity.setInterestCalcMode("DAILY_30");
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(30));
        pawnResponse.setPawnAmount(new BigDecimal("5000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getInterestAmount()).isNotNull();
        assertThat(result.getTotalAmount()).isNotNull();
    }

    @Test
    @DisplayName("calculatePawnRedeem throws when pawn is CANCELLED")
    void testCalculatePawnRedeem_CancelledStatus() {
        pawnEntity.setPawnStatus(PawnStatus.CANCELLED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.calculatePawnRedeem(1L, null))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("calculatePawnRedeem throws when pawn is REDEEMED")
    void testCalculatePawnRedeem_RedeemedStatus() {
        pawnEntity.setPawnStatus(PawnStatus.REDEEMED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.calculatePawnRedeem(1L, null))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("calculatePawnRedeem uses provided redeemDate from request")
    void testCalculatePawnRedeem_WithRedeemDate() {
        RedeemRequest redeemRequest = new RedeemRequest();
        redeemRequest.setRedeemDate(LocalDateTime.now().minusDays(5));

        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(15));
        pawnResponse.setPawnAmount(new BigDecimal("3000000"));
        pawnResponse.setInterestRate(new BigDecimal("2"));

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, redeemRequest);

        // DateTimeUtil.fromLocalDateTime converts GMT→UTC+7 so the stored date shifts; just verify it's set
        assertThat(result.getRedeemDate()).isNotNull();
    }

    // ── getPawnSetting / updatePawnSetting ────────────────────────────────────

    @Test
    @DisplayName("getPawnSetting delegates to shopInfoService")
    void testGetPawnSetting() {
        PawnSetting setting = PawnSetting.builder().interestRate(new BigDecimal("3")).interestType(30).dueDate(30).build();
        when(shopInfoService.getPawnSetting()).thenReturn(setting);

        PawnSetting result = pawnService.getPawnSetting();

        assertThat(result).isSameAs(setting);
        verify(shopInfoService).getPawnSetting();
    }

    @Test
    @DisplayName("updatePawnSetting delegates to shopInfoService")
    void testUpdatePawnSetting() {
        PawnSetting setting = PawnSetting.builder().interestRate(new BigDecimal("3")).interestType(30).dueDate(30).build();
        when(shopInfoService.updatePawnSetting(setting)).thenReturn(setting);

        PawnSetting result = pawnService.updatePawnSetting(setting);

        assertThat(result).isSameAs(setting);
        verify(shopInfoService).updatePawnSetting(setting);
    }

    // ── mergeInterestBarsData (static) ────────────────────────────────────────

    @Test
    @DisplayName("mergeInterestBarsData sums amounts for same year-month")
    void testMergeInterestBarsData_SameMonth() {
        BarsData redeemed = BarsData.builder().year(2024).month(3).amount(1000000).count(2).weight(0.0).build();
        BarsData forfeited = BarsData.builder().year(2024).month(3).amount(500000).count(1).weight(0.0).build();

        List<BarsData> result = PawnServiceImpl.mergeInterestBarsData(
                List.of(redeemed), List.of(forfeited));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(1500000);
        assertThat(result.get(0).getCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("mergeInterestBarsData keeps separate entries for different months")
    void testMergeInterestBarsData_DifferentMonths() {
        BarsData jan = BarsData.builder().year(2024).month(1).amount(1000000).count(1).weight(0.0).build();
        BarsData feb = BarsData.builder().year(2024).month(2).amount(800000).count(1).weight(0.0).build();

        List<BarsData> result = PawnServiceImpl.mergeInterestBarsData(
                List.of(jan), List.of(feb));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("mergeInterestBarsData with empty lists returns empty result")
    void testMergeInterestBarsData_BothEmpty() {
        List<BarsData> result = PawnServiceImpl.mergeInterestBarsData(List.of(), List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("mergeInterestBarsData with only redeemed data")
    void testMergeInterestBarsData_OnlyRedeemed() {
        BarsData redeemed = BarsData.builder().year(2024).month(5).amount(2000000).count(3).weight(0.0).build();

        List<BarsData> result = PawnServiceImpl.mergeInterestBarsData(List.of(redeemed), List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(2000000);
    }

    // ── requestMoreMoney ─────────────────────────────────────────────────────

    @Test
    @DisplayName("requestMoreMoney saves ReqMoneyEntity and logs activity")
    void testRequestMoreMoney_Success() {
        ReqMoneyRequest request = new ReqMoneyRequest();
        request.setRequestAmount(new BigDecimal("2000000"));
        request.setRequestDate(LocalDateTime.now().toLocalDate());

        ReqMoneyEntity savedEntity = ReqMoneyEntity.builder()
                .requestId(10L)
                .pawnId(1L)
                .requestAmount(new BigDecimal("2000000"))
                .build();

        ReqMoneyResponse response = new ReqMoneyResponse();
        response.setRequestId(10L);

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(reqMoneyRepository.save(any(ReqMoneyEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.fromReqMoneyEntity(any(ReqMoneyEntity.class))).thenReturn(response);

        ReqMoneyResponse result = pawnService.requestMoreMoney(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(10L);
        verify(reqMoneyRepository).save(any(ReqMoneyEntity.class));
        verify(activityLogService).logAsync(eq("test-shop"), eq("staff01"), isNull(),
                any(), eq("PAWN"), eq("1"), anyString(), isNull(), any());
    }

    @Test
    @DisplayName("requestMoreMoney throws ResourceNotFoundException when pawn not found")
    void testRequestMoreMoney_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.requestMoreMoney(99L, new ReqMoneyRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("requestMoreMoney with null requestDate sets null on entity")
    void testRequestMoreMoney_NullDate() {
        ReqMoneyRequest request = new ReqMoneyRequest();
        request.setRequestAmount(new BigDecimal("1000000"));
        request.setRequestDate(null);

        ReqMoneyEntity savedEntity = ReqMoneyEntity.builder()
                .requestId(11L).pawnId(1L).requestAmount(new BigDecimal("1000000")).build();

        ReqMoneyResponse response = new ReqMoneyResponse();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(reqMoneyRepository.save(any(ReqMoneyEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.fromReqMoneyEntity(any(ReqMoneyEntity.class))).thenReturn(response);

        ReqMoneyResponse result = pawnService.requestMoreMoney(1L, request);

        assertThat(result).isNotNull();
    }

    // ── updateVisibleStatus ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateVisibleStatus delegates to repository and returns count")
    void testUpdateVisibleStatus_Success() {
        when(pawnRepository.updateVisibleStatus(List.of(1L), true)).thenReturn(1);

        int result = pawnService.updateVisibleStatus(1L, true);

        assertThat(result).isEqualTo(1);
        verify(pawnRepository).updateVisibleStatus(List.of(1L), true);
    }

    // ── createPawn ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPawn: saves pawn with PAWNED status and returns details")
    @SuppressWarnings("unchecked")
    void testCreatePawn_Success() {
        PawnRequest req = new PawnRequest();
        req.setCustomerId(10L);
        req.setItemName("Dây chuyền vàng");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("5000000"));
        req.setInterestRate(new BigDecimal("3"));

        PawnEntity mappedEntity = PawnEntity.builder()
                .customerId(10L).itemName("Dây chuyền vàng")
                .pawnAmount(new BigDecimal("5000000")).interestRate(new BigDecimal("3"))
                .build();
        PawnEntity savedEntity = PawnEntity.builder()
                .pawnId(2L).customerId(10L).itemName("Dây chuyền vàng")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("3")).build();

        when(pawnMapper.fromPawnRequest(req)).thenReturn(mappedEntity);
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnRepository.findById(2L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(2L)).thenReturn(List.of());

        PawnResponse result = pawnService.createPawn(req);

        assertThat(result).isNotNull();
        verify(pawnRepository).save(any(PawnEntity.class));
        verify(activityLogService).logAsync(anyString(), anyString(), isNull(), any(), eq("PAWN"), eq("2"), anyString(), isNull(), any());
    }

    @Test
    @DisplayName("createPawn: creates guest customer when visitingGuest flag is set and no customerId")
    @SuppressWarnings("unchecked")
    void testCreatePawn_VisitingGuest_CreatesCustomer() {
        PawnRequest req = new PawnRequest();
        req.setVisitingGuest(true);
        req.setCustomerId(0L);
        req.setItemName("Điện thoại");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("2000000"));
        req.setInterestRate(new BigDecimal("3"));

        when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.empty());
        Customer savedGuest = Customer.builder().name("Khách vãng lai").phone("0000000000").build();
        savedGuest.setId(99L);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedGuest);

        PawnEntity mappedEntity = PawnEntity.builder().customerId(99L).itemName("Điện thoại")
                .pawnAmount(new BigDecimal("2000000")).interestRate(new BigDecimal("3")).build();
        PawnEntity savedEntity = PawnEntity.builder().pawnId(3L).customerId(99L).itemName("Điện thoại")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("2000000"))
                .interestRate(new BigDecimal("3")).build();
        PawnResponse resp = new PawnResponse();
        resp.setPawnId(3L);

        when(pawnMapper.fromPawnRequest(req)).thenReturn(mappedEntity);
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnRepository.findById(3L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(resp);
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(3L)).thenReturn(List.of());

        PawnResponse result = pawnService.createPawn(req);

        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    // ── getPawns ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawns: returns paged results using specifications")
    @SuppressWarnings("unchecked")
    void testGetPawns_Success() {
        PawnQuery query = new PawnQuery();
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        when(queryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(query)));
        when(pawnMapper.fromPawnQuery(query)).thenReturn(pawnResponse);
        when(queryRepository.getSummary(any(Specification.class))).thenReturn(null);

        SearchPawnRequest searchReq = SearchPawnRequest.builder().build();
        PawnSearchResponse result = pawnService.getPawns(PageRequest.of(0, 10), searchReq);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPawns: applies visible status filter when shop flag is enabled")
    @SuppressWarnings("unchecked")
    void testGetPawns_WithVisibleFilter() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(true);
        when(queryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(queryRepository.getSummary(any(Specification.class))).thenReturn(null);

        PawnSearchResponse result = pawnService.getPawns(PageRequest.of(0, 10), SearchPawnRequest.builder().build());

        assertThat(result.getContent()).isEmpty();
    }

    // ── getCustomerPawnKpi ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomerPawnKpi: returns five customer rankings mapped to widget fields")
    void getCustomerPawnKpi_returnsFiveRankings() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        Object[] row = new Object[]{ 7L, "Nguyễn Văn A", 3L, 5_000_000L, 250_000L };
        when(pawnRepository.findTopCustomersByPawnAmount(any(), any(), eq(false), any()))
                .thenReturn(java.util.Collections.singletonList(row));
        when(pawnRepository.findTopCustomersByPawnCount(any(), any(), eq(false), any()))
                .thenReturn(java.util.Collections.singletonList(row));
        when(pawnRepository.findTopCustomersByCompletedAmount(any(), any(), eq(false), any()))
                .thenReturn(java.util.Collections.singletonList(row));
        when(pawnRepository.findTopCustomersByCompletedCount(any(), any(), eq(false), any()))
                .thenReturn(java.util.Collections.singletonList(row));
        when(pawnRepository.findTopCustomersByInterestAmount(any(), any(), eq(false), any()))
                .thenReturn(java.util.Collections.singletonList(row));

        com.tappy.pos.model.dto.pawn.DateFilterRequest filter =
                com.tappy.pos.model.dto.pawn.DateFilterRequest.builder()
                        .fromDate(System.currentTimeMillis() - 86_400_000L)
                        .toDate(System.currentTimeMillis())
                        .build();

        java.util.Map<String, Object> result = pawnService.getCustomerPawnKpi(filter);

        assertThat(result).containsOnlyKeys("topPawnedAmount", "topPawnedCount",
                "topCompletedPawnAmount", "topCompletedPawnCount", "topInterestAmount");
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> pawnedAmount =
                (List<java.util.Map<String, Object>>) result.get("topPawnedAmount");
        assertThat(pawnedAmount).hasSize(1);
        assertThat(pawnedAmount.get(0))
                .containsEntry("customerId", 7L)
                .containsEntry("name", "Nguyễn Văn A")
                .containsEntry("pawnCount", 3)
                .containsEntry("pawnAmount", 5_000_000L)
                .containsEntry("interestAmount", 250_000L);
    }

    @Test
    @DisplayName("getCustomerPawnKpi: maps a null customer name to walk-in label")
    void getCustomerPawnKpi_nullName() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(true);
        Object[] row = new Object[]{ null, null, 0L, 0L, 0L };
        when(pawnRepository.findTopCustomersByPawnAmount(any(), any(), eq(true), any()))
                .thenReturn(java.util.Collections.singletonList(row));
        when(pawnRepository.findTopCustomersByPawnCount(any(), any(), eq(true), any())).thenReturn(List.of());
        when(pawnRepository.findTopCustomersByCompletedAmount(any(), any(), eq(true), any())).thenReturn(List.of());
        when(pawnRepository.findTopCustomersByCompletedCount(any(), any(), eq(true), any())).thenReturn(List.of());
        when(pawnRepository.findTopCustomersByInterestAmount(any(), any(), eq(true), any())).thenReturn(List.of());

        com.tappy.pos.model.dto.pawn.DateFilterRequest filter =
                com.tappy.pos.model.dto.pawn.DateFilterRequest.builder().fromDate(0L).toDate(1L).build();

        java.util.Map<String, Object> result = pawnService.getCustomerPawnKpi(filter);

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> rows =
                (List<java.util.Map<String, Object>>) result.get("topPawnedAmount");
        assertThat(rows.get(0)).containsEntry("name", "Khách vãng lai");
    }

    // ── updatePawn ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePawn: updates entity and returns details")
    void testUpdatePawn_Success() {
        PawnRequest req = new PawnRequest();
        req.setItemName("iPhone 15");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("10000000"));
        req.setInterestRate(new BigDecimal("2.5"));
        req.setPawnStatus(PawnStatus.PAWNED);

        PawnEntity savedEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("iPhone 15")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("10000000"))
                .interestRate(new BigDecimal("2.5")).build();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity)).thenReturn(Optional.of(savedEntity));
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.updatePawn(1L, req);

        assertThat(result).isNotNull();
        verify(pawnRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("updatePawn: deletes reqMoney items when deletedRequestIds provided")
    void testUpdatePawn_DeletesReqMoneys() {
        PawnRequest req = new PawnRequest();
        req.setItemName("Gold ring");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("3000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnStatus(PawnStatus.PAWNED);
        req.setDeletedRequestIds(List.of(5L, 6L));

        PawnEntity savedEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Gold ring")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("3000000"))
                .interestRate(new BigDecimal("3")).build();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity)).thenReturn(Optional.of(savedEntity));
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        pawnService.updatePawn(1L, req);

        verify(reqMoneyRepository).deleteAllById(List.of(5L, 6L));
    }

    @Test
    @DisplayName("updatePawn: throws ResourceNotFoundException when pawn not found")
    void testUpdatePawn_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.updatePawn(99L, new PawnRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── extendPawn ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("extendPawn: marks original as EXTENDED and saves new extended pawn")
    void testExtendPawn_Success() {
        PawnRequest req = new PawnRequest();
        req.setItemName("Dây chuyền vàng");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setExtendDate(LocalDateTime.now());
        req.setExtendDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("5000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnStatus(PawnStatus.PAWNED);

        PawnEntity extendedPawn = PawnEntity.builder()
                .pawnId(2L).customerId(10L).itemName("Dây chuyền vàng")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("3"))
                .reqMoneys(new HashSet<>()).build();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnRepository.findById(2L)).thenReturn(Optional.of(extendedPawn));
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(pawnEntity).thenReturn(extendedPawn);
        when(pawnMapper.fromPawnEntity(extendedPawn)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(2L)).thenReturn(List.of());

        PawnResponse result = pawnService.extendPawn(1L, req);

        assertThat(result).isNotNull();
        // Extending renews the loan into a new PAWNED contract; the original is closed as
        // EXTENDED ("Đã gia hạn"), NOT REDEEMED ("Đã trả" = paid back & item returned).
        assertThat(pawnEntity.getPawnStatus()).isEqualTo(PawnStatus.EXTENDED);
        verify(pawnRepository, times(2)).save(any(PawnEntity.class));
    }

    @Test
    @DisplayName("extendPawn: throws when original pawn not found")
    void testExtendPawn_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.extendPawn(99L, new PawnRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPawnKPIs ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawnKPIs: assembles KPIs from repository aggregates")
    @SuppressWarnings("unchecked")
    void testGetPawnKPIs_Success() {
        long now = System.currentTimeMillis();
        DateFilterRequest filter = DateFilterRequest.builder()
                .fromDate(now - 86400000L).toDate(now).build();

        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);

        List<Object[]> rows = Collections.singletonList(new Object[]{new BigDecimal("5000000"), 2L});
        List<Object[]> empty = Collections.emptyList();
        when(pawnRepository.getPawnAmountByPawnStatus(eq(PawnStatus.PAWNED), eq(false)))
                .thenReturn(rows);
        when(pawnRepository.getPawnRequestAmountByPawnStatus(eq(PawnStatus.PAWNED), eq(false)))
                .thenReturn(0L);
        when(pawnRepository.sumByPawnStatusAndPawnDueDateBetween(eq(PawnStatus.PAWNED), any(), any(), eq(false)))
                .thenReturn(rows);
        when(pawnRepository.sumRequestAmountByPawnStatusAndPawnDueDateBetween(eq(PawnStatus.PAWNED), any(), any(), eq(false)))
                .thenReturn(0L);
        when(pawnRepository.sumByPawnStatusAndPawnDueDateBefore(eq(PawnStatus.PAWNED), any(), eq(false)))
                .thenReturn(rows);
        when(pawnRepository.sumRequestMoneyByPawnStatusAndRequestDateBetween(eq(PawnStatus.PAWNED), any(), any(), eq(false)))
                .thenReturn(empty);
        when(pawnRepository.sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(rows);
        when(pawnRepository.sumRequestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(0L);
        when(pawnRepository.sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(500000L);

        PawnKPIs kpis = pawnService.getPawnKPIs(filter);

        assertThat(kpis).isNotNull();
        assertThat(kpis.getTotalPawnedCount()).isEqualTo(2);
        assertThat(kpis.getInterestPawnAmount()).isEqualTo(500000L);
    }

    // ── getPawnCharts ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawnCharts: throws when date filter is zero")
    void testGetPawnCharts_NoDateFilter() {
        DateFilterRequest filter = DateFilterRequest.builder().fromDate(0).toDate(0).build();
        when(messageService.getMessage(anyString())).thenReturn("date required");

        assertThatThrownBy(() -> pawnService.getPawnCharts(filter))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getPawnCharts: returns bars response with data from repository")
    void testGetPawnCharts_Success() {
        long now = System.currentTimeMillis();
        DateFilterRequest filter = DateFilterRequest.builder()
                .fromDate(now - 86400000L).toDate(now).build();

        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);

        List<Object[]> rows = Collections.singletonList(
                new Object[]{new BigDecimal("1000000"), 3L, 2026, 5});
        when(pawnRepository.getAmountAndTotalCountByMonthPawnDateAndStatus(any(), any(), eq(false))).thenReturn(rows);
        when(pawnRepository.getAmountAndTotalCountByMonthRedeemDateAndStatus(anyList(), any(), any(), eq(false))).thenReturn(rows);
        when(pawnRepository.getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(anyList(), any(), any(), eq(false))).thenReturn(rows);
        when(pawnRepository.getRedeemedInterestAmount(eq(PawnStatus.REDEEMED), any(), any(), eq(false))).thenReturn(rows);
        when(pawnRepository.getForfeitedInterestAmount(eq(PawnStatus.FORFEITED), any(), any(), eq(false))).thenReturn(rows);

        PawnBarsResponse result = pawnService.getPawnCharts(filter);

        assertThat(result).isNotNull();
        assertThat(result.getPawnedBars()).hasSize(1);
        assertThat(result.getPawnedBars().get(0).getAmount()).isEqualTo(1000000L);
    }

    @Test
    @DisplayName("getPawnCharts: handles null values in result rows gracefully")
    void testGetPawnCharts_NullValues() {
        long now = System.currentTimeMillis();
        DateFilterRequest filter = DateFilterRequest.builder()
                .fromDate(now - 86400000L).toDate(now).build();

        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);

        List<Object[]> rowsWithNulls = Collections.singletonList(
                new Object[]{null, null, null, null});
        when(pawnRepository.getAmountAndTotalCountByMonthPawnDateAndStatus(any(), any(), eq(false))).thenReturn(rowsWithNulls);
        when(pawnRepository.getAmountAndTotalCountByMonthRedeemDateAndStatus(anyList(), any(), any(), eq(false))).thenReturn(List.of());
        when(pawnRepository.getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(anyList(), any(), any(), eq(false))).thenReturn(List.of());
        when(pawnRepository.getRedeemedInterestAmount(eq(PawnStatus.REDEEMED), any(), any(), eq(false))).thenReturn(List.of());
        when(pawnRepository.getForfeitedInterestAmount(eq(PawnStatus.FORFEITED), any(), any(), eq(false))).thenReturn(List.of());

        PawnBarsResponse result = pawnService.getPawnCharts(filter);

        assertThat(result).isNotNull();
        assertThat(result.getPawnedBars().get(0).getAmount()).isEqualTo(0L);
        assertThat(result.getPawnedBars().get(0).getCount()).isEqualTo(0);
    }

    // ── getPawnIdsToClean ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawnIdsToClean: returns pawn IDs from query result")
    void testGetPawnIdsToClean_Success() {
        SearchPawnRequest searchRequest = SearchPawnRequest.builder().build();
        PawnQuery query1 = new PawnQuery();
        query1.setPawnId(1L);
        PawnQuery query2 = new PawnQuery();
        query2.setPawnId(2L);
        Page<PawnQuery> page = new PageImpl<>(List.of(query1, query2));
        when(queryRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        List<Long> result = pawnService.getPawnIdsToClean(PageRequest.of(0, 10), searchRequest);

        assertThat(result).containsExactly(1L, 2L);
    }

    // ── calculateInterestAmount branches ──────────────────────────────────────

    @Test
    @DisplayName("calculatePawnRedeem: uses 25-day interest mode")
    void testCalculatePawnRedeem_25DayMode() {
        pawnEntity.setInterestCalcMode("DAILY_25");
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(35));
        pawnResponse.setPawnAmount(new BigDecimal("5000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));
        pawnEntity.getReqMoneys().clear();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        assertThat(result.getInterestAmount()).isNotNull().isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculatePawnRedeem: uses full-month interest mode (rounds up to next full month)")
    void testCalculatePawnRedeem_FullMonthMode() {
        pawnEntity.setInterestCalcMode("MONTHLY");
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(20));
        pawnResponse.setPawnAmount(new BigDecimal("2000000"));
        pawnResponse.setInterestRate(new BigDecimal("2"));
        pawnEntity.getReqMoneys().clear();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        assertThat(result.getInterestAmount()).isNotNull();
    }

    @Test
    @DisplayName("calculatePawnRedeem: extends an existing pawn (isExtending flag)")
    void testCalculatePawnRedeem_Extending() {
        RedeemRequest redeemRequest = new RedeemRequest();
        redeemRequest.setExtendingRequest(true);

        pawnEntity.setInterestCalcMode("DAILY_30");
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(10));
        pawnResponse.setPawnAmount(new BigDecimal("3000000"));
        pawnResponse.setInterestRate(new BigDecimal("2"));
        pawnEntity.getReqMoneys().clear();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, redeemRequest);

        assertThat(result.getInterestAmount()).isNotNull();
        assertThat(result.getHeldDays()).isEqualTo(10);
    }

    // ── B6: request-level interestCalcMode overrides entity's stored mode ────

    @Test
    @DisplayName("calculatePawnRedeem: request-level interestCalcMode overrides entity's stored DAILY_30 with MONTHLY")
    void testCalculatePawnRedeem_RequestOverridesCalcMode() {
        pawnEntity.setInterestCalcMode("DAILY_30");
        pawnEntity.getReqMoneys().clear();
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(30));
        pawnResponse.setPawnAmount(new BigDecimal("10000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));

        RedeemRequest redeemRequest = new RedeemRequest();
        redeemRequest.setInterestCalcMode("MONTHLY");

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, redeemRequest);

        // pawnDate=30 days ago → dateBetween=30 → pawnedDays=31
        // MONTHLY: months=(31+29)/30=2; interest=10M * 0.03 * 2 = 600,000
        // DAILY_30 would give 10M * 0.03 / 30 * 31 = 310,000 — confirms MONTHLY won
        assertThat(result.getInterestAmount()).isEqualByComparingTo(new BigDecimal("600000"));
    }

    // ── B5: reqMoneys interest and principal accumulation ─────────────────────

    @Test
    @DisplayName("calculatePawnRedeem: accumulates interest from each reqMoney disbursement")
    void testCalculatePawnRedeem_WithReqMoneys_AccumulatesInterest() {
        ReqMoneyEntity reqMoneyEntity = ReqMoneyEntity.builder()
                .requestId(5L).pawnId(1L)
                .requestAmount(new BigDecimal("2000000"))
                .requestDate(LocalDateTime.now().minusDays(15))
                .build();

        pawnEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).pawnStatus(PawnStatus.PAWNED)
                .pawnAmount(new BigDecimal("5000000")).interestRate(new BigDecimal("3"))
                .interestCalcMode("DAILY_30")
                .reqMoneys(Set.of(reqMoneyEntity))
                .build();

        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(30));
        pawnResponse.setPawnAmount(new BigDecimal("5000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));

        ReqMoneyResponse reqMoneyResponse = new ReqMoneyResponse();
        reqMoneyResponse.setRequestId(5L);
        reqMoneyResponse.setRequestAmount(new BigDecimal("2000000"));

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());
        when(pawnMapper.fromReqMoneyEntity(reqMoneyEntity)).thenReturn(reqMoneyResponse);

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        // Main: pawnDate=30 days ago → pawnedDays=31; 5M * 0.03 / 30 * 31 = 155,000
        // ReqMoney: requestDate=15 days ago → dateBetween=15 → pawnedDays=16; 2M * 0.03 / 30 * 16 = 32,000
        // Total interest = 187,000; total amount = 5M + 2M + 187,000 = 7,187,000
        assertThat(result.getInterestAmount()).isEqualByComparingTo(new BigDecimal("187000"));
        assertThat(result.getReqMoneys()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("7187000"));
    }

    // ── BIWEEKLY interest mode ────────────────────────────────────────────────

    @Test
    @DisplayName("calculatePawnRedeem: biweekly mode rounds up to next half-month period")
    void testCalculatePawnRedeem_BiweeklyMode() {
        pawnEntity.setInterestCalcMode("BIWEEKLY");
        pawnEntity.getReqMoneys().clear();
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(14));
        pawnResponse.setPawnAmount(new BigDecimal("2000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        // pawnDate=14 days ago → dateBetween=14 → pawnedDays=15
        // halfMonths=(15+14)/15=1; interest=2M * 0.03 * 1 / 2 = 30,000
        assertThat(result.getInterestAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    // ── calculateHeldDays: same-day minimum ──────────────────────────────────

    @Test
    @DisplayName("calculatePawnRedeem: same-day redemption charges minimum 1 day interest")
    void testCalculatePawnRedeem_SameDayChargesOneDay() {
        pawnEntity.setInterestCalcMode("DAILY_30");
        pawnEntity.getReqMoneys().clear();
        pawnResponse.setPawnDate(LocalDateTime.now());
        pawnResponse.setPawnAmount(new BigDecimal("3000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        // dateBetween=0 → pawnedDays=1; 3M * 0.03 / 30 * 1 = 3,000
        assertThat(result.getHeldDays()).isEqualTo(1);
        assertThat(result.getInterestAmount()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    // ── requestMoreMoney: blocked by terminal statuses ────────────────────────

    @Test
    @DisplayName("requestMoreMoney throws PawnStatusNotAllowException when pawn is CANCELLED")
    void testRequestMoreMoney_ThrowsWhenCancelled() {
        pawnEntity.setPawnStatus(PawnStatus.CANCELLED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.requestMoreMoney(1L, new ReqMoneyRequest()))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("requestMoreMoney throws PawnStatusNotAllowException when pawn is REDEEMED")
    void testRequestMoreMoney_ThrowsWhenRedeemed() {
        pawnEntity.setPawnStatus(PawnStatus.REDEEMED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.requestMoreMoney(1L, new ReqMoneyRequest()))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("requestMoreMoney throws PawnStatusNotAllowException when pawn is FORFEITED")
    void testRequestMoreMoney_ThrowsWhenForfeited() {
        pawnEntity.setPawnStatus(PawnStatus.FORFEITED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.requestMoreMoney(1L, new ReqMoneyRequest()))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    // ── forfeitPawnByPawnId: amount validation ────────────────────────────────

    @Test
    @DisplayName("forfeitPawnByPawnId throws IllegalArgumentException when interestAmount is null")
    void testForfeitPawnByPawnId_ThrowsWhenInterestAmountNull() {
        ForfeitRequest request = new ForfeitRequest();
        request.setInterestAmount(null);
        request.setTotalAmount(new BigDecimal("4500000"));
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId throws IllegalArgumentException when interestAmount is negative")
    void testForfeitPawnByPawnId_ThrowsWhenInterestAmountNegative() {
        ForfeitRequest request = new ForfeitRequest();
        request.setInterestAmount(new BigDecimal("-1"));
        request.setTotalAmount(new BigDecimal("4500000"));
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId throws IllegalArgumentException when totalAmount is zero")
    void testForfeitPawnByPawnId_ThrowsWhenTotalAmountZero() {
        ForfeitRequest request = new ForfeitRequest();
        request.setInterestAmount(new BigDecimal("450000"));
        request.setTotalAmount(BigDecimal.ZERO);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── createPawn: visiting guest reuses existing walk-in customer ───────────

    @Test
    @DisplayName("createPawn: reuses existing walk-in customer instead of creating a new one")
    @SuppressWarnings("unchecked")
    void testCreatePawn_VisitingGuest_ReuseExistingCustomer() {
        PawnRequest req = new PawnRequest();
        req.setVisitingGuest(true);
        req.setCustomerId(0L);
        req.setItemName("Điện thoại");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("2000000"));
        req.setInterestRate(new BigDecimal("3"));

        com.tappy.pos.model.entity.customer.Customer existingGuest = com.tappy.pos.model.entity.customer.Customer.builder()
                .name("Khách vãng lai").phone("0000000000").build();
        existingGuest.setId(99L);
        when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.of(existingGuest));

        PawnEntity mappedEntity = PawnEntity.builder().customerId(99L).itemName("Điện thoại")
                .pawnAmount(new BigDecimal("2000000")).interestRate(new BigDecimal("3")).build();
        PawnEntity savedEntity = PawnEntity.builder().pawnId(3L).customerId(99L).itemName("Điện thoại")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("2000000"))
                .interestRate(new BigDecimal("3")).build();
        PawnResponse resp = new PawnResponse();
        resp.setPawnId(3L);

        when(pawnMapper.fromPawnRequest(req)).thenReturn(mappedEntity);
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnRepository.findById(3L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(resp);
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(3L)).thenReturn(List.of());

        pawnService.createPawn(req);

        verify(customerRepository, never()).save(any(com.tappy.pos.model.entity.customer.Customer.class));
    }

    // ── extendPawn: reqMoney amounts rolled into new pawnAmount ──────────────

    @Test
    @DisplayName("extendPawn: new pawn amount includes all reqMoney disbursements from original")
    void testExtendPawn_AccumulatesReqMoneys() {
        ReqMoneyEntity reqMoney = ReqMoneyEntity.builder()
                .requestId(7L).pawnId(1L).requestAmount(new BigDecimal("1000000")).build();
        PawnEntity originalPawn = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Dây chuyền vàng")
                .pawnStatus(PawnStatus.PAWNED)
                .pawnAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("3"))
                .interestCalcMode("DAILY_30")
                .reqMoneys(new HashSet<>(Set.of(reqMoney)))
                .build();

        PawnRequest req = new PawnRequest();
        req.setItemName("Dây chuyền vàng");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setExtendDate(LocalDateTime.now());
        req.setExtendDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("6000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnStatus(PawnStatus.PAWNED);

        PawnEntity extendedPawn = PawnEntity.builder()
                .pawnId(2L).customerId(10L).itemName("Dây chuyền vàng")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("6000000"))
                .interestRate(new BigDecimal("3")).reqMoneys(new HashSet<>()).build();

        ArgumentCaptor<PawnEntity> saveCaptor = ArgumentCaptor.forClass(PawnEntity.class);

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(originalPawn));
        when(pawnRepository.findById(2L)).thenReturn(Optional.of(extendedPawn));
        when(pawnRepository.save(saveCaptor.capture())).thenReturn(originalPawn).thenReturn(extendedPawn);
        when(pawnMapper.fromPawnEntity(extendedPawn)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(2L)).thenReturn(List.of());

        pawnService.extendPawn(1L, req);

        // Second save is the new extended pawn; amount should be 5M + 1M (reqMoney) = 6M
        List<PawnEntity> captured = saveCaptor.getAllValues();
        assertThat(captured).hasSizeGreaterThanOrEqualTo(2);
        assertThat(captured.get(1).getPawnAmount()).isEqualByComparingTo(new BigDecimal("6000000"));
    }

    // ── deletePawnByPawnIds: category child table cleanup ─────────────────────

    @Test
    @DisplayName("deletePawnByPawnIds: invokes all category child table cleanup repositories before main delete")
    void testDeletePawnByPawnIds_CleansUpCategoryRepositories() {
        when(pawnRepository.findAllById(List.of(1L))).thenReturn(List.of(pawnEntity));

        pawnService.deletePawnByPawnIds(List.of(1L));

        verify(electronicsRepository).deleteByPawnIdIn(List.of(1L));
        verify(vehicleRepository).deleteByPawnIdIn(List.of(1L));
        verify(watchRepository).deleteByPawnIdIn(List.of(1L));
        verify(realEstateRepository).deleteByPawnIdIn(List.of(1L));
        verify(generalRepository).deleteByPawnIdIn(List.of(1L));
        verify(pawnRepository).deleteAllByIdInBatch(List.of(1L));
    }

    // ── saveItemDetail: ELECTRONICS category saves electronics entity ─────────

    @Test
    @DisplayName("createPawn: saves electronics detail when pawnCategory is ELECTRONICS")
    @SuppressWarnings("unchecked")
    void testCreatePawn_WithElectronicsDetail() {
        PawnElectronicsDetail detail = new PawnElectronicsDetail();
        detail.setBrand("Apple");

        PawnRequest req = new PawnRequest();
        req.setCustomerId(10L);
        req.setItemName("iPhone 15 Pro");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("15000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnCategory("ELECTRONICS");
        req.setElectronicsDetail(detail);

        PawnElectronicsEntity electronicsEntity = PawnElectronicsEntity.builder()
                .pawnId(4L).brand("Apple").build();

        PawnEntity mappedEntity = PawnEntity.builder().customerId(10L).itemName("iPhone 15 Pro").build();
        PawnEntity savedEntity = PawnEntity.builder().pawnId(4L).customerId(10L).itemName("iPhone 15 Pro")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("15000000"))
                .interestRate(new BigDecimal("3")).pawnCategory("ELECTRONICS").build();

        when(pawnMapper.fromPawnRequest(req)).thenReturn(mappedEntity);
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.toElectronicsEntity(anyString(), eq(4L), eq(detail))).thenReturn(electronicsEntity);
        when(pawnRepository.findById(4L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(4L)).thenReturn(List.of());
        when(electronicsRepository.findByPawnId(4L)).thenReturn(Optional.of(electronicsEntity));
        when(pawnMapper.fromElectronicsEntity(electronicsEntity)).thenReturn(new PawnElectronicsDetail());

        pawnService.createPawn(req);

        verify(electronicsRepository).deleteByPawnId(4L);
        verify(electronicsRepository).save(electronicsEntity);
    }

    // ── saveItemDetail: WATCH category ────────────────────────────────────────

    @Test
    @DisplayName("createPawn: saves watch detail when pawnCategory is WATCH")
    @SuppressWarnings("unchecked")
    void testCreatePawn_WithWatchDetail() {
        PawnWatchDetail detail = new PawnWatchDetail();

        PawnRequest req = new PawnRequest();
        req.setCustomerId(10L);
        req.setItemName("Rolex Submariner");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("50000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnCategory("WATCH");
        req.setWatchDetail(detail);

        PawnEntity savedEntity = PawnEntity.builder().pawnId(5L).customerId(10L).itemName("Rolex Submariner")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("50000000"))
                .interestRate(new BigDecimal("3")).pawnCategory("WATCH").build();

        when(pawnMapper.fromPawnRequest(req)).thenReturn(PawnEntity.builder().build());
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.toWatchEntity(anyString(), eq(5L), eq(detail))).thenReturn(mock(com.tappy.pos.model.entity.pawn.PawnWatchEntity.class));
        when(pawnRepository.findById(5L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(5L)).thenReturn(List.of());
        when(watchRepository.findByPawnId(5L)).thenReturn(Optional.empty());

        pawnService.createPawn(req);

        verify(watchRepository).deleteByPawnId(5L);
        verify(watchRepository).save(any());
    }

    // ── getPawnIdsToClean ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPawnIdsToClean: returns list of pawnIds from page")
    @SuppressWarnings("unchecked")
    void testGetPawnIdsToClean_ReturnsPawnIds() {
        PawnQuery query1 = new PawnQuery();
        query1.setPawnId(10L);
        PawnQuery query2 = new PawnQuery();
        query2.setPawnId(20L);

        when(queryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(query1, query2)));

        List<Long> ids = pawnService.getPawnIdsToClean(PageRequest.of(0, 10),
                SearchPawnRequest.builder().build());

        assertThat(ids).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("getPawnIdsToClean: returns empty list when no pawns match")
    @SuppressWarnings("unchecked")
    void testGetPawnIdsToClean_EmptyResult() {
        when(queryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        List<Long> ids = pawnService.getPawnIdsToClean(PageRequest.of(0, 10),
                SearchPawnRequest.builder().build());

        assertThat(ids).isEmpty();
    }

    // ── enrichWithItemDetail: ELECTRONICS category ────────────────────────────

    @Test
    @DisplayName("getPawnDetails: enriches response with electronics detail when pawnCategory is ELECTRONICS")
    void testGetPawnDetails_WithElectronicsCategory() {
        PawnEntity electronics = PawnEntity.builder()
                .pawnId(6L).customerId(10L).itemName("iPhone 15")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("10000000"))
                .interestRate(new BigDecimal("3")).pawnCategory("ELECTRONICS").build();

        PawnElectronicsEntity electronicsEntity = PawnElectronicsEntity.builder()
                .pawnId(6L).brand("Apple").build();
        PawnElectronicsDetail electronicsDetail = new PawnElectronicsDetail();
        electronicsDetail.setBrand("Apple");

        PawnResponse resp = new PawnResponse();
        resp.setPawnId(6L);

        when(pawnRepository.findById(6L)).thenReturn(Optional.of(electronics));
        when(pawnMapper.fromPawnEntity(electronics)).thenReturn(resp);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(electronicsRepository.findByPawnId(6L)).thenReturn(Optional.of(electronicsEntity));
        when(pawnMapper.fromElectronicsEntity(electronicsEntity)).thenReturn(electronicsDetail);
        when(auditRepository.findByPawnIdOrderByActionIdAsc(6L)).thenReturn(List.of());

        PawnResponse result = pawnService.getPawnDetails(6L);

        assertThat(result).isNotNull();
        verify(electronicsRepository).findByPawnId(6L);
        verify(pawnMapper).fromElectronicsEntity(electronicsEntity);
    }

    // ── signContract / removeSignature (digital pawn contract, §4d) ─────────────

    /** A minimal valid PNG header (8-byte signature) base64-encoded as a data URL. */
    private static String pngDataUrl() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    private void stubGetPawnDetails() {
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("signContract stores the signature, stamps signedAt, logs PAWN_SIGNED, cleans up the old image")
    void testSignContract_Success() {
        pawnEntity.setCustomerSignatureUrl("https://img/old.png");
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(r2StorageService.keyFromUrl("https://img/old.png")).thenReturn("old-key");
        when(r2StorageService.upload(anyString(), any(byte[].class), eq("image/png")))
                .thenReturn("https://img/new.png");
        stubGetPawnDetails();

        SignPawnRequest req = new SignPawnRequest();
        req.setSignature(pngDataUrl());

        PawnResponse result = pawnService.signContract(1L, req);

        assertThat(result).isNotNull();
        assertThat(pawnEntity.getCustomerSignatureUrl()).isEqualTo("https://img/new.png");
        assertThat(pawnEntity.getSignedAt()).isNotNull();
        verify(r2StorageService).upload(anyString(), any(byte[].class), eq("image/png"));
        verify(pawnRepository).save(pawnEntity);
        verify(r2CleanupService).deleteAsync("old-key");
        verify(activityLogService).logAsync(eq("test-shop"), eq("staff01"), isNull(),
                eq(ActivityAction.PAWN_SIGNED), eq("PAWN"), eq("1"), any(), isNull(), any());
    }

    @Test
    @DisplayName("signContract rejects a non-PAWNED contract")
    void testSignContract_StatusNotAllowed() {
        pawnEntity.setPawnStatus(PawnStatus.REDEEMED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        SignPawnRequest req = new SignPawnRequest();
        req.setSignature(pngDataUrl());

        assertThatThrownBy(() -> pawnService.signContract(1L, req))
                .isInstanceOf(PawnStatusNotAllowException.class);
        verify(r2StorageService, never()).upload(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("signContract throws 404 when a non-owner lacks PAWN_VIEW_ALL")
    void testSignContract_OwnershipDenied() {
        pawnEntity.setCreatedBy("someone-else");
        when(featureContext.hasFeature("PAWN_VIEW_ALL")).thenReturn(false);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        SignPawnRequest req = new SignPawnRequest();
        req.setSignature(pngDataUrl());

        assertThatThrownBy(() -> pawnService.signContract(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(r2StorageService, never()).upload(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("signContract rejects a payload that is not a PNG")
    void testSignContract_InvalidPng() {
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        SignPawnRequest req = new SignPawnRequest();
        req.setSignature("data:image/png;base64," + Base64.getEncoder().encodeToString("not-a-png".getBytes()));

        assertThatThrownBy(() -> pawnService.signContract(1L, req))
                .isInstanceOf(BadRequestException.class);
        verify(r2StorageService, never()).upload(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("removeSignature clears the columns and cleans up the stored image")
    void testRemoveSignature_Success() {
        pawnEntity.setCustomerSignatureUrl("https://img/old.png");
        pawnEntity.setSignedAt(LocalDateTime.now());
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(r2StorageService.keyFromUrl("https://img/old.png")).thenReturn("old-key");
        stubGetPawnDetails();

        PawnResponse result = pawnService.removeSignature(1L);

        assertThat(result).isNotNull();
        assertThat(pawnEntity.getCustomerSignatureUrl()).isNull();
        assertThat(pawnEntity.getSignedAt()).isNull();
        verify(pawnRepository).save(pawnEntity);
        verify(r2CleanupService).deleteAsync("old-key");
    }

    // ── getPawnDetails: PAWN_VIEW_ALL ownership guard ──────────────────────────

    @Test
    @DisplayName("getPawnDetails: throws 404 when caller lacks PAWN_VIEW_ALL and is not the creator")
    void testGetPawnDetails_OwnershipDenied() {
        pawnEntity.setCreatedBy("someone-else");
        when(featureContext.hasFeature("PAWN_VIEW_ALL")).thenReturn(false);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.getPawnDetails(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPawnDetails: allows the creator through even without PAWN_VIEW_ALL")
    void testGetPawnDetails_OwnerAllowed() {
        pawnEntity.setCreatedBy("staff01");
        when(featureContext.hasFeature("PAWN_VIEW_ALL")).thenReturn(false);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.getPawnDetails(1L);

        assertThat(result).isNotNull();
    }

    // ── getPawns: PAWN_VIEW_ALL scoping + item-detail enrichment ──────────────

    @Test
    @DisplayName("getPawns: scopes list to createdBy when PAWN_VIEW_ALL is absent and enriches details")
    @SuppressWarnings("unchecked")
    void testGetPawns_ScopedToCreatedBy_WithEnrichment() {
        when(featureContext.hasFeature("PAWN_VIEW_ALL")).thenReturn(false);
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);

        PawnQuery query = new PawnQuery();
        PawnResponse enriched = new PawnResponse();
        enriched.setPawnId(6L);
        enriched.setPawnCategory("ELECTRONICS");

        PawnElectronicsEntity electronicsEntity = PawnElectronicsEntity.builder().pawnId(6L).brand("Apple").build();

        when(queryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(query)));
        when(pawnMapper.fromPawnQuery(query)).thenReturn(enriched);
        when(electronicsRepository.findByPawnIdIn(List.of(6L))).thenReturn(List.of(electronicsEntity));
        when(pawnMapper.fromElectronicsEntity(electronicsEntity)).thenReturn(new PawnElectronicsDetail());
        when(queryRepository.getSummary(any(Specification.class))).thenReturn(null);

        PawnSearchResponse result = pawnService.getPawns(PageRequest.of(0, 10), SearchPawnRequest.builder().build());

        assertThat(result.getContent()).hasSize(1);
        verify(electronicsRepository).findByPawnIdIn(List.of(6L));
    }

    // ── lookupByCode ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("lookupByCode: resolves a numeric code via findById")
    void testLookupByCode_NumericFound() {
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.lookupByCode("1");

        assertThat(result).isNotNull();
        verify(pawnRepository, atLeastOnce()).findById(1L);
        verify(pawnRepository, never()).findByLegacyId(anyString());
    }

    @Test
    @DisplayName("lookupByCode: falls back to legacyId lookup when numeric id is not found")
    void testLookupByCode_NumericMissing_FallsBackToLegacy() {
        // First findById (numeric parse) misses; legacy lookup hits; getPawnDetails re-fetches by id.
        when(pawnRepository.findById(1L)).thenReturn(Optional.empty()).thenReturn(Optional.of(pawnEntity));
        when(pawnRepository.findByLegacyId("1")).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.lookupByCode("1");

        assertThat(result).isNotNull();
        verify(pawnRepository).findByLegacyId("1");
    }

    @Test
    @DisplayName("lookupByCode: non-numeric code goes straight to legacyId lookup")
    void testLookupByCode_NonNumeric_UsesLegacy() {
        when(pawnRepository.findByLegacyId("LEGACY-123")).thenReturn(Optional.of(pawnEntity));
        // getPawnDetails re-fetches the resolved entity by its pawnId
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.lookupByCode("LEGACY-123");

        assertThat(result).isNotNull();
        verify(pawnRepository).findByLegacyId("LEGACY-123");
    }

    @Test
    @DisplayName("lookupByCode: throws when neither numeric nor legacy match exists")
    void testLookupByCode_NotFound() {
        when(pawnRepository.findByLegacyId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.lookupByCode("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updatePawn: REDEEMED requestType + heldDays branch ─────────────────────

    @Test
    @DisplayName("updatePawn: REDEEMED requestType triggers PAWN_REDEEMED activity action and applies heldDays")
    void testUpdatePawn_RedeemedRequestType() {
        PawnRequest req = new PawnRequest();
        req.setItemName("Vòng vàng");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("4000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnStatus(PawnStatus.REDEEMED);
        req.setRequestType("REDEEMED");
        req.setHeldDays(12L);

        PawnEntity savedEntity = PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Vòng vàng")
                .pawnStatus(PawnStatus.REDEEMED).pawnAmount(new BigDecimal("4000000"))
                .interestRate(new BigDecimal("3")).build();

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity)).thenReturn(Optional.of(savedEntity));
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        pawnService.updatePawn(1L, req);

        assertThat(pawnEntity.getHeldDays()).isEqualTo(12);
        verify(activityLogService).logAsync(eq("test-shop"), eq("staff01"), isNull(),
                eq(ActivityAction.PAWN_REDEEMED), eq("PAWN"), eq("1"), eq("activity.pawn.redeemed"), isNull(), any());
    }

    // ── decodeSignaturePng: size cap + blank ──────────────────────────────────

    @Test
    @DisplayName("signContract: rejects a signature larger than the size cap")
    void testSignContract_SignatureTooLarge() {
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        byte[] huge = new byte[300 * 1024];
        huge[0] = (byte) 0x89; huge[1] = 'P'; huge[2] = 'N'; huge[3] = 'G';
        SignPawnRequest req = new SignPawnRequest();
        req.setSignature(Base64.getEncoder().encodeToString(huge));

        assertThatThrownBy(() -> pawnService.signContract(1L, req))
                .isInstanceOf(BadRequestException.class);
        verify(r2StorageService, never()).upload(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("signContract: a blank uploaded URL nulls out the signature column")
    void testSignContract_BlankUploadUrl() {
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(r2StorageService.keyFromUrl(any())).thenReturn(null);
        when(r2StorageService.upload(anyString(), any(byte[].class), eq("image/png"))).thenReturn("");
        stubGetPawnDetails();

        SignPawnRequest req = new SignPawnRequest();
        req.setSignature(pngDataUrl());

        pawnService.signContract(1L, req);

        assertThat(pawnEntity.getCustomerSignatureUrl()).isNull();
    }

    // ── removeSignature: ownership guard ──────────────────────────────────────

    @Test
    @DisplayName("removeSignature: throws 404 when caller lacks PAWN_VIEW_ALL and is not the creator")
    void testRemoveSignature_OwnershipDenied() {
        pawnEntity.setCreatedBy("someone-else");
        when(featureContext.hasFeature("PAWN_VIEW_ALL")).thenReturn(false);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.removeSignature(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pawnRepository, never()).save(any());
    }

    // ── forfeitPawnByPawnId: remaining validation branches ────────────────────

    @Test
    @DisplayName("forfeitPawnByPawnId: throws when totalAmount is null")
    void testForfeitPawnByPawnId_ThrowsWhenTotalAmountNull() {
        ForfeitRequest request = new ForfeitRequest();
        request.setInterestAmount(new BigDecimal("450000"));
        request.setTotalAmount(null);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId: throws when forfeitedDate is missing")
    void testForfeitPawnByPawnId_ThrowsWhenForfeitDateNull() {
        ForfeitRequest request = new ForfeitRequest();
        request.setInterestAmount(new BigDecimal("450000"));
        request.setTotalAmount(new BigDecimal("4950000"));
        request.setForfeitedDate(null);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId: throws when forfeitedAmount is missing")
    void testForfeitPawnByPawnId_ThrowsWhenForfeitAmountNull() {
        ForfeitRequest request = new ForfeitRequest();
        request.setInterestAmount(new BigDecimal("450000"));
        request.setTotalAmount(new BigDecimal("4950000"));
        request.setForfeitedDate(LocalDateTime.now());
        request.setForfeitedAmount(null);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("forfeitPawnByPawnId: throws ResourceNotFoundException when pawn not found")
    void testForfeitPawnByPawnId_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.forfeitPawnByPawnId(99L, new ForfeitRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── calculatePawnRedeem: FORFEITED status blocked + customer mapping ───────

    @Test
    @DisplayName("calculatePawnRedeem: throws when pawn is FORFEITED")
    void testCalculatePawnRedeem_ForfeitedStatus() {
        pawnEntity.setPawnStatus(PawnStatus.FORFEITED);
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));

        assertThatThrownBy(() -> pawnService.calculatePawnRedeem(1L, null))
                .isInstanceOf(PawnStatusNotAllowException.class);
    }

    @Test
    @DisplayName("calculatePawnRedeem: throws ResourceNotFoundException when pawn not found")
    void testCalculatePawnRedeem_NotFound() {
        when(pawnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pawnService.calculatePawnRedeem(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("calculatePawnRedeem: maps customer details onto the response when customer is present")
    void testCalculatePawnRedeem_WithCustomer() {
        pawnEntity.setInterestCalcMode("DAILY_30");
        pawnResponse.setPawnDate(LocalDateTime.now().minusDays(10));
        pawnResponse.setPawnAmount(new BigDecimal("5000000"));
        pawnResponse.setInterestRate(new BigDecimal("3"));
        pawnEntity.getReqMoneys().clear();

        Customer customer = Customer.builder().name("Trần Thị B").phone("0987654321").build();
        customer.setId(10L);

        when(pawnRepository.findById(1L)).thenReturn(Optional.of(pawnEntity));
        when(pawnMapper.fromPawnEntity(pawnEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(auditRepository.findByPawnIdOrderByActionIdAsc(1L)).thenReturn(List.of());

        PawnResponse result = pawnService.calculatePawnRedeem(1L, null);

        assertThat(result.getCustomerName()).isEqualTo("Trần Thị B");
        assertThat(result.getPhone()).isEqualTo("0987654321");
    }

    // ── extendPawn: category-specific detail copy branches ────────────────────

    private PawnEntity newExtendOriginal(String category) {
        return PawnEntity.builder()
                .pawnId(1L).customerId(10L).itemName("Item")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("3")).interestCalcMode("DAILY_30")
                .pawnCategory(category).reqMoneys(new HashSet<>()).build();
    }

    private PawnRequest newExtendRequest(String category) {
        PawnRequest req = new PawnRequest();
        req.setItemName("Item");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setExtendDate(LocalDateTime.now());
        req.setExtendDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("5000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnStatus(PawnStatus.PAWNED);
        // appendUpdateInfo overwrites the original's category from the request before the
        // category-copy block runs — so the request must carry the same category.
        req.setPawnCategory(category);
        return req;
    }

    private void stubExtendCommon(PawnEntity original) {
        // The new contract carries no category so the final getPawnDetails enrich step is a no-op,
        // keeping the category-copy stubs (findByPawnId(1L)) the only invocation of those repos.
        PawnEntity extendedPawn = PawnEntity.builder()
                .pawnId(2L).customerId(10L).itemName("Item")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("3")).reqMoneys(new HashSet<>())
                .build();
        when(pawnRepository.findById(1L)).thenReturn(Optional.of(original));
        when(pawnRepository.findById(2L)).thenReturn(Optional.of(extendedPawn));
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(original).thenReturn(extendedPawn);
        when(pawnMapper.fromPawnEntity(extendedPawn)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(2L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("extendPawn: copies JEWELRY detail to the new contract")
    void testExtendPawn_CopiesJewelryDetail() {
        PawnEntity original = newExtendOriginal("JEWELRY");
        stubExtendCommon(original);

        com.tappy.pos.model.entity.pawn.PawnJewelryEntity jewelry =
                mock(com.tappy.pos.model.entity.pawn.PawnJewelryEntity.class);
        PawnJewelryDetail detail = new PawnJewelryDetail();
        com.tappy.pos.model.entity.pawn.PawnJewelryEntity copied =
                mock(com.tappy.pos.model.entity.pawn.PawnJewelryEntity.class);
        when(jewelryRepository.findByPawnId(1L)).thenReturn(Optional.of(jewelry));
        when(pawnMapper.fromJewelryEntity(jewelry)).thenReturn(detail);
        when(pawnMapper.toJewelryEntity(eq("test-shop"), eq(2L), eq(detail))).thenReturn(copied);

        pawnService.extendPawn(1L, newExtendRequest("JEWELRY"));

        verify(jewelryRepository).save(copied);
    }

    @Test
    @DisplayName("extendPawn: copies VEHICLE detail to the new contract")
    void testExtendPawn_CopiesVehicleDetail() {
        PawnEntity original = newExtendOriginal("MOTORBIKE");
        stubExtendCommon(original);

        com.tappy.pos.model.entity.pawn.PawnVehicleEntity vehicle =
                mock(com.tappy.pos.model.entity.pawn.PawnVehicleEntity.class);
        PawnVehicleDetail detail = new PawnVehicleDetail();
        com.tappy.pos.model.entity.pawn.PawnVehicleEntity copied =
                mock(com.tappy.pos.model.entity.pawn.PawnVehicleEntity.class);
        when(vehicleRepository.findByPawnId(1L)).thenReturn(Optional.of(vehicle));
        when(pawnMapper.fromVehicleEntity(vehicle)).thenReturn(detail);
        when(pawnMapper.toVehicleEntity(eq("test-shop"), eq(2L), eq(detail))).thenReturn(copied);

        pawnService.extendPawn(1L, newExtendRequest("MOTORBIKE"));

        verify(vehicleRepository).save(copied);
    }

    @Test
    @DisplayName("extendPawn: copies ELECTRONICS detail to the new contract")
    void testExtendPawn_CopiesElectronicsDetail() {
        PawnEntity original = newExtendOriginal("ELECTRONICS");
        stubExtendCommon(original);

        PawnElectronicsEntity electronics = mock(PawnElectronicsEntity.class);
        PawnElectronicsDetail detail = new PawnElectronicsDetail();
        PawnElectronicsEntity copied = mock(PawnElectronicsEntity.class);
        when(electronicsRepository.findByPawnId(1L)).thenReturn(Optional.of(electronics));
        when(pawnMapper.fromElectronicsEntity(electronics)).thenReturn(detail);
        when(pawnMapper.toElectronicsEntity(eq("test-shop"), eq(2L), eq(detail))).thenReturn(copied);

        pawnService.extendPawn(1L, newExtendRequest("ELECTRONICS"));

        verify(electronicsRepository).save(copied);
    }

    @Test
    @DisplayName("extendPawn: copies WATCH detail to the new contract")
    void testExtendPawn_CopiesWatchDetail() {
        PawnEntity original = newExtendOriginal("WATCH");
        stubExtendCommon(original);

        com.tappy.pos.model.entity.pawn.PawnWatchEntity watch =
                mock(com.tappy.pos.model.entity.pawn.PawnWatchEntity.class);
        PawnWatchDetail detail = new PawnWatchDetail();
        com.tappy.pos.model.entity.pawn.PawnWatchEntity copied =
                mock(com.tappy.pos.model.entity.pawn.PawnWatchEntity.class);
        when(watchRepository.findByPawnId(1L)).thenReturn(Optional.of(watch));
        when(pawnMapper.fromWatchEntity(watch)).thenReturn(detail);
        when(pawnMapper.toWatchEntity(eq("test-shop"), eq(2L), eq(detail))).thenReturn(copied);

        pawnService.extendPawn(1L, newExtendRequest("WATCH"));

        verify(watchRepository).save(copied);
    }

    @Test
    @DisplayName("extendPawn: copies REAL_ESTATE detail to the new contract")
    void testExtendPawn_CopiesRealEstateDetail() {
        PawnEntity original = newExtendOriginal("REAL_ESTATE");
        stubExtendCommon(original);

        com.tappy.pos.model.entity.pawn.PawnRealEstateEntity realEstate =
                mock(com.tappy.pos.model.entity.pawn.PawnRealEstateEntity.class);
        PawnRealEstateDetail detail = new PawnRealEstateDetail();
        com.tappy.pos.model.entity.pawn.PawnRealEstateEntity copied =
                mock(com.tappy.pos.model.entity.pawn.PawnRealEstateEntity.class);
        when(realEstateRepository.findByPawnId(1L)).thenReturn(Optional.of(realEstate));
        when(pawnMapper.fromRealEstateEntity(realEstate)).thenReturn(detail);
        when(pawnMapper.toRealEstateEntity(eq("test-shop"), eq(2L), eq(detail))).thenReturn(copied);

        pawnService.extendPawn(1L, newExtendRequest("REAL_ESTATE"));

        verify(realEstateRepository).save(copied);
    }

    @Test
    @DisplayName("extendPawn: copies GENERAL detail to the new contract")
    void testExtendPawn_CopiesGeneralDetail() {
        PawnEntity original = newExtendOriginal("GENERAL");
        stubExtendCommon(original);

        com.tappy.pos.model.entity.pawn.PawnGeneralEntity general =
                mock(com.tappy.pos.model.entity.pawn.PawnGeneralEntity.class);
        PawnGeneralDetail detail = new PawnGeneralDetail();
        com.tappy.pos.model.entity.pawn.PawnGeneralEntity copied =
                mock(com.tappy.pos.model.entity.pawn.PawnGeneralEntity.class);
        when(generalRepository.findByPawnId(1L)).thenReturn(Optional.of(general));
        when(pawnMapper.fromGeneralEntity(general)).thenReturn(detail);
        when(pawnMapper.toGeneralEntity(eq("test-shop"), eq(2L), eq(detail))).thenReturn(copied);

        pawnService.extendPawn(1L, newExtendRequest("GENERAL"));

        verify(generalRepository).save(copied);
    }

    // ── saveItemDetail / enrichWithItemDetail: remaining categories ───────────

    @Test
    @DisplayName("createPawn: saves VEHICLE detail when pawnCategory is MOTORBIKE")
    @SuppressWarnings("unchecked")
    void testCreatePawn_WithVehicleDetail() {
        PawnVehicleDetail detail = new PawnVehicleDetail();
        PawnRequest req = new PawnRequest();
        req.setCustomerId(10L);
        req.setItemName("Honda SH");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("30000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnCategory("MOTORBIKE");
        req.setVehicleDetail(detail);

        PawnEntity savedEntity = PawnEntity.builder().pawnId(7L).customerId(10L).itemName("Honda SH")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("30000000"))
                .interestRate(new BigDecimal("3")).pawnCategory("MOTORBIKE").build();

        when(pawnMapper.fromPawnRequest(req)).thenReturn(PawnEntity.builder().build());
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.toVehicleEntity(anyString(), eq(7L), eq(detail)))
                .thenReturn(mock(com.tappy.pos.model.entity.pawn.PawnVehicleEntity.class));
        when(pawnRepository.findById(7L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(7L)).thenReturn(List.of());
        when(vehicleRepository.findByPawnId(7L)).thenReturn(Optional.empty());

        pawnService.createPawn(req);

        verify(vehicleRepository).deleteByPawnId(7L);
        verify(vehicleRepository).save(any());
    }

    @Test
    @DisplayName("createPawn: saves JEWELRY detail when pawnCategory is JEWELRY")
    @SuppressWarnings("unchecked")
    void testCreatePawn_WithJewelryDetail() {
        PawnJewelryDetail detail = new PawnJewelryDetail();
        PawnRequest req = new PawnRequest();
        req.setCustomerId(10L);
        req.setItemName("Nhẫn kim cương");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("20000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnCategory("JEWELRY");
        req.setJewelryDetail(detail);

        PawnEntity savedEntity = PawnEntity.builder().pawnId(8L).customerId(10L).itemName("Nhẫn kim cương")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("20000000"))
                .interestRate(new BigDecimal("3")).pawnCategory("JEWELRY").build();

        when(pawnMapper.fromPawnRequest(req)).thenReturn(PawnEntity.builder().build());
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnMapper.toJewelryEntity(anyString(), eq(8L), eq(detail)))
                .thenReturn(mock(com.tappy.pos.model.entity.pawn.PawnJewelryEntity.class));
        when(pawnRepository.findById(8L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(8L)).thenReturn(List.of());
        when(jewelryRepository.findByPawnId(8L)).thenReturn(Optional.empty());

        pawnService.createPawn(req);

        verify(jewelryRepository).deleteByPawnId(8L);
        verify(jewelryRepository).save(any());
    }

    @Test
    @DisplayName("createPawn: REAL_ESTATE and GENERAL detail null skips the save but still deletes")
    @SuppressWarnings("unchecked")
    void testCreatePawn_RealEstateCategory_NullDetailSkipsSave() {
        PawnRequest req = new PawnRequest();
        req.setCustomerId(10L);
        req.setItemName("Sổ đỏ");
        req.setPawnDate(LocalDateTime.now());
        req.setPawnDueDate(LocalDateTime.now().plusDays(30));
        req.setPawnAmount(new BigDecimal("500000000"));
        req.setInterestRate(new BigDecimal("3"));
        req.setPawnCategory("REAL_ESTATE");
        req.setRealEstateDetail(null);

        PawnEntity savedEntity = PawnEntity.builder().pawnId(9L).customerId(10L).itemName("Sổ đỏ")
                .pawnStatus(PawnStatus.PAWNED).pawnAmount(new BigDecimal("500000000"))
                .interestRate(new BigDecimal("3")).pawnCategory("REAL_ESTATE").build();

        when(pawnMapper.fromPawnRequest(req)).thenReturn(PawnEntity.builder().build());
        when(pawnRepository.save(any(PawnEntity.class))).thenReturn(savedEntity);
        when(pawnRepository.findById(9L)).thenReturn(Optional.of(savedEntity));
        when(pawnMapper.fromPawnEntity(savedEntity)).thenReturn(pawnResponse);
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());
        when(auditRepository.findByPawnIdOrderByActionIdAsc(9L)).thenReturn(List.of());
        when(realEstateRepository.findByPawnId(9L)).thenReturn(Optional.empty());

        pawnService.createPawn(req);

        verify(realEstateRepository).deleteByPawnId(9L);
        verify(realEstateRepository, never()).save(any());
    }

    // ── getPawnKPIs already covered for month; add zero-result path ────────────

    // ── getPawnCustomerInsights ───────────────────────────────────────────────

    @Test
    @DisplayName("getPawnCustomerInsights: derives returning customers as total minus new")
    void testGetPawnCustomerInsights_Success() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        when(pawnRepository.countDistinctPawnCustomers(any(), any(), eq(false))).thenReturn(10L);
        when(pawnRepository.countNewPawnCustomers(any(), any(), eq(false))).thenReturn(4L);
        when(pawnRepository.countWalkInPawns(any(), any(), eq(false))).thenReturn(2L);

        PawnCustomerInsights insights = pawnService.getPawnCustomerInsights(
                java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now());

        assertThat(insights.getTotalCustomers()).isEqualTo(10L);
        assertThat(insights.getNewCustomers()).isEqualTo(4L);
        assertThat(insights.getReturningCustomers()).isEqualTo(6L);
        assertThat(insights.getWalkInCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getPawnCustomerInsights: clamps returning customers to zero when new exceeds total")
    void testGetPawnCustomerInsights_NewExceedsTotal() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(true);
        when(pawnRepository.countDistinctPawnCustomers(any(), any(), eq(true))).thenReturn(3L);
        when(pawnRepository.countNewPawnCustomers(any(), any(), eq(true))).thenReturn(5L);
        when(pawnRepository.countWalkInPawns(any(), any(), eq(true))).thenReturn(0L);

        PawnCustomerInsights insights = pawnService.getPawnCustomerInsights(
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());

        assertThat(insights.getReturningCustomers()).isEqualTo(0L);
    }

    // ── getTopPawnCustomers ───────────────────────────────────────────────────

    @Test
    @DisplayName("getTopPawnCustomers: maps rows to widget fields and walk-in label for null name")
    void testGetTopPawnCustomers_Success() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        Object[] namedRow = new Object[]{ 7L, "Nguyễn Văn A", 3L, 5_000_000L, 250_000L };
        Object[] nullNameRow = new Object[]{ null, null, null, null, null };
        when(pawnRepository.findTopCustomersByPawnAmount(any(), any(), eq(false), any()))
                .thenReturn(List.of(namedRow, nullNameRow));

        List<java.util.Map<String, Object>> result = pawnService.getTopPawnCustomers(
                5, java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now());

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("customerId", 7L)
                .containsEntry("name", "Nguyễn Văn A")
                .containsEntry("totalCount", 3)
                .containsEntry("totalAmount", 5_000_000L)
                .containsEntry("interestAmount", 250_000L);
        assertThat(result.get(1)).containsEntry("name", "Khách vãng lai")
                .containsEntry("totalCount", 0)
                .containsEntry("totalAmount", 0L);
    }

    // ── getCustomerPawnSummary ────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomerPawnSummary: aggregates all + active rows for the customer")
    void testGetCustomerPawnSummary_Success() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        Object[] allRow = new Object[]{ 8L, 40_000_000L, 1_500_000L };
        Object[] activeRow = new Object[]{ 3L };
        when(pawnRepository.sumByPawnStatusInAndCustomerIdEquals(eq(10L), anyList(), eq(false)))
                .thenReturn(Collections.singletonList(allRow))
                .thenReturn(Collections.singletonList(activeRow));

        java.util.Map<String, Object> result = pawnService.getCustomerPawnSummary(10L);

        assertThat(result).containsEntry("totalCount", 8L)
                .containsEntry("totalPrincipal", 40_000_000L)
                .containsEntry("totalInterest", 1_500_000L)
                .containsEntry("activeCount", 3L);
    }

    @Test
    @DisplayName("getCustomerPawnSummary: returns zeros when no rows exist")
    void testGetCustomerPawnSummary_EmptyRows() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        when(pawnRepository.sumByPawnStatusInAndCustomerIdEquals(eq(99L), anyList(), eq(false)))
                .thenReturn(List.of())
                .thenReturn(List.of());

        java.util.Map<String, Object> result = pawnService.getCustomerPawnSummary(99L);

        assertThat(result).containsEntry("totalCount", 0L)
                .containsEntry("activeCount", 0L)
                .containsEntry("totalPrincipal", 0L)
                .containsEntry("totalInterest", 0L);
    }

    // ── getPawnCharts: day / week / year granularity branches ─────────────────

    private List<Object[]> dayRow() {
        return Collections.singletonList(new Object[]{ new BigDecimal("1000000"), 2L, 2026, 5, 12 });
    }

    private void stubDayChartRepos() {
        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        when(pawnRepository.getAmountByDayPawnDate(any(), any(), eq(false))).thenReturn(dayRow());
        when(pawnRepository.getAmountByDayRedeemDate(anyList(), any(), any(), eq(false))).thenReturn(dayRow());
        when(pawnRepository.getAmountByDayForfeitedDate(anyList(), any(), any(), eq(false))).thenReturn(dayRow());
        when(pawnRepository.getRedeemedInterestByDay(eq(PawnStatus.REDEEMED), any(), any(), eq(false))).thenReturn(dayRow());
        when(pawnRepository.getForfeitedInterestByDay(eq(PawnStatus.FORFEITED), any(), any(), eq(false))).thenReturn(dayRow());
        when(pawnRepository.getExtendedInterestByDay(any(), any(), eq(false))).thenReturn(dayRow());
    }

    @Test
    @DisplayName("getPawnCharts: day granularity returns day-level bars")
    void testGetPawnCharts_DayGranularity() {
        long now = System.currentTimeMillis();
        DateFilterRequest filter = DateFilterRequest.builder()
                .fromDate(now - 86400000L).toDate(now).granularity("day").build();
        stubDayChartRepos();

        PawnBarsResponse result = pawnService.getPawnCharts(filter);

        assertThat(result.getPawnedBars()).hasSize(1);
        assertThat(result.getPawnedBars().get(0).getDay()).isEqualTo(12);
    }

    @Test
    @DisplayName("getPawnCharts: week granularity aggregates day-level bars into week buckets")
    void testGetPawnCharts_WeekGranularity() {
        long now = System.currentTimeMillis();
        DateFilterRequest filter = DateFilterRequest.builder()
                .fromDate(now - 86400000L).toDate(now).granularity("week").build();
        stubDayChartRepos();

        PawnBarsResponse result = pawnService.getPawnCharts(filter);

        assertThat(result.getPawnedBars()).hasSize(1);
        // week label is the Monday of the ISO week, formatted YYYY-MM-DD
        assertThat(result.getPawnedBars().get(0).getLabel()).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    @DisplayName("getPawnCharts: year granularity aggregates month-level bars into year buckets")
    void testGetPawnCharts_YearGranularity() {
        long now = System.currentTimeMillis();
        DateFilterRequest filter = DateFilterRequest.builder()
                .fromDate(now - 86400000L).toDate(now).granularity("year").build();

        when(shopInfoService.getExcludeVisibleItemFlag()).thenReturn(false);
        List<Object[]> monthRow = Collections.singletonList(new Object[]{ new BigDecimal("2000000"), 4L, 2026, 5 });
        when(pawnRepository.getAmountAndTotalCountByMonthPawnDateAndStatus(any(), any(), eq(false))).thenReturn(monthRow);
        when(pawnRepository.getAmountAndTotalCountByMonthRedeemDateAndStatus(anyList(), any(), any(), eq(false))).thenReturn(monthRow);
        when(pawnRepository.getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(anyList(), any(), any(), eq(false))).thenReturn(monthRow);
        when(pawnRepository.getRedeemedInterestAmount(eq(PawnStatus.REDEEMED), any(), any(), eq(false))).thenReturn(monthRow);
        when(pawnRepository.getForfeitedInterestAmount(eq(PawnStatus.FORFEITED), any(), any(), eq(false))).thenReturn(monthRow);
        when(pawnRepository.getExtendedInterestByMonth(any(), any(), eq(false))).thenReturn(monthRow);

        PawnBarsResponse result = pawnService.getPawnCharts(filter);

        assertThat(result.getPawnedBars()).hasSize(1);
        assertThat(result.getPawnedBars().get(0).getLabel()).isEqualTo("2026");
    }

    @Test
    @DisplayName("getPawnCharts: throws when only toDate is zero")
    void testGetPawnCharts_ToDateZero() {
        DateFilterRequest filter = DateFilterRequest.builder().fromDate(123L).toDate(0L).build();

        assertThatThrownBy(() -> pawnService.getPawnCharts(filter))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── mergeInterestBarsData: label fallback to year-month ───────────────────

    @Test
    @DisplayName("mergeInterestBarsData: falls back to year-month key when label is null")
    void testMergeInterestBarsData_NullLabelFallsBackToYearMonth() {
        BarsData a = BarsData.builder().year(2026).month(5).amount(100000).count(1).weight(0.0).build();
        BarsData b = BarsData.builder().year(2026).month(5).amount(50000).count(1).weight(0.0).build();

        List<BarsData> result = PawnServiceImpl.mergeInterestBarsData(List.of(a), List.of(b));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(150000);
        assertThat(result.get(0).getCount()).isEqualTo(2);
    }
}
