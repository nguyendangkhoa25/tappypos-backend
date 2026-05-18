package com.tappy.pos.service.customer;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.loyalty.CreateLoyaltyTierRequest;
import com.tappy.pos.model.dto.loyalty.SaveLoyaltyProgramRequest;
import com.tappy.pos.model.dto.loyalty.LoyaltyTierDTO;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.customer.LoyaltyProgram;
import com.tappy.pos.model.entity.customer.LoyaltyTier;
import com.tappy.pos.model.entity.customer.LoyaltyTransaction;
import com.tappy.pos.model.enums.LoyaltyTransactionType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.customer.LoyaltyProgramRepository;
import com.tappy.pos.repository.customer.LoyaltyTierRepository;
import com.tappy.pos.repository.customer.LoyaltyTransactionRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.multitenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoyaltyService Unit Tests")
class LoyaltyServiceTest {

    @Mock private LoyaltyProgramRepository programRepository;
    @Mock private LoyaltyTierRepository tierRepository;
    @Mock private LoyaltyTransactionRepository transactionRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MessageService messageService;
    @Mock private TenantContext tenantContext;

    @InjectMocks
    private LoyaltyService loyaltyService;

    private LoyaltyProgram activeProgram;
    private Customer customer;

    @BeforeEach
    void setUp() {
        activeProgram = LoyaltyProgram.builder()
                .pointsPerAmount(1)
                .amountPerPoints(10000L)
                .redemptionPointsPerDiscount(100)
                .redemptionDiscountAmount(new BigDecimal("10000"))
                .minRedemptionPoints(100)
                .isActive(true)
                .build();

        customer = Customer.builder()
                .name("Test Customer")
                .phone("0901234567")
                .loyaltyPoints(200)
                .totalSpent(new BigDecimal("500000"))
                .build();

        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(eq("error.loyalty.minPointsRequired"), any(Object[].class)))
                .thenReturn("Minimum points required to redeem.");
        lenient().when(messageService.getMessage(eq("error.loyalty.insufficientPoints"), any(Object[].class)))
                .thenReturn("Insufficient points balance.");
    }

    // ── awardPointsForOrder ───────────────────────────────────────────────────

    @Test
    @DisplayName("awardPointsForOrder: awards correct points for 100,000 VND order")
    void awardPoints_basicOrder() {
        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(tierRepository.findTierForSpend(any())).thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenReturn(customer);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loyaltyService.awardPointsForOrder(10L, 1L, new BigDecimal("100000"));

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        // 100000 / 10000 = 10 units * 1 point = 10 points → 200 + 10 = 210
        assertThat(customerCaptor.getValue().getLoyaltyPoints()).isEqualTo(210);
    }

    @Test
    @DisplayName("awardPointsForOrder: applies tier multiplier")
    void awardPoints_withTierMultiplier() {
        LoyaltyTier goldTier = LoyaltyTier.builder()
                .name("Vàng")
                .pointsMultiplier(new BigDecimal("1.5"))
                .build();

        when(transactionRepository.findByOrderId(2L)).thenReturn(Optional.empty());
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(tierRepository.findTierForSpend(any())).thenReturn(Optional.of(goldTier));
        when(customerRepository.save(any())).thenReturn(customer);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loyaltyService.awardPointsForOrder(10L, 2L, new BigDecimal("100000"));

        ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(cap.capture());
        // 10 base points * 1.5 multiplier = 15 points → 200 + 15 = 215
        assertThat(cap.getValue().getLoyaltyPoints()).isEqualTo(215);
    }

    @Test
    @DisplayName("awardPointsForOrder: skips if order already has a transaction")
    void awardPoints_idempotent() {
        when(transactionRepository.findByOrderId(1L))
                .thenReturn(Optional.of(LoyaltyTransaction.builder().build()));

        loyaltyService.awardPointsForOrder(10L, 1L, new BigDecimal("100000"));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("awardPointsForOrder: skips if no active program")
    void awardPoints_noProgram() {
        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(programRepository.findActiveProgram()).thenReturn(Optional.empty());

        loyaltyService.awardPointsForOrder(10L, 1L, new BigDecimal("100000"));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("awardPointsForOrder: skips if amount is too small to earn a point")
    void awardPoints_amountTooSmall() {
        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(tierRepository.findTierForSpend(any())).thenReturn(Optional.empty());

        loyaltyService.awardPointsForOrder(10L, 1L, new BigDecimal("5000")); // < 10000 VND

        verify(customerRepository, never()).save(any());
    }

    // ── redeemPoints ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("redeemPoints: returns correct discount amount")
    void redeemPoints_success() {
        customer.setLoyaltyPoints(300);
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal discount = loyaltyService.redeemPoints(10L, 200, 5L);

        // 200 / 100 = 2 units * 10000 VND = 20000 VND
        assertThat(discount).isEqualByComparingTo(new BigDecimal("20000"));
        ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(cap.capture());
        assertThat(cap.getValue().getLoyaltyPoints()).isEqualTo(100); // 300 - 200
    }

    @Test
    @DisplayName("redeemPoints: throws when below minimum redemption threshold")
    void redeemPoints_belowMinimum() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> loyaltyService.redeemPoints(10L, 50, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Minimum");
    }

    @Test
    @DisplayName("redeemPoints: throws when insufficient points balance")
    void redeemPoints_insufficientBalance() {
        customer.setLoyaltyPoints(50);
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> loyaltyService.redeemPoints(10L, 100, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("redeemPoints: throws when no active program")
    void redeemPoints_noProgram() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.redeemPoints(10L, 100, 5L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── adjustPoints ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("adjustPoints: adds positive points correctly")
    void adjustPoints_positive() {
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loyaltyService.adjustPoints(10L, 50, "Bonus");

        ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(cap.capture());
        assertThat(cap.getValue().getLoyaltyPoints()).isEqualTo(250); // 200 + 50
    }

    @Test
    @DisplayName("adjustPoints: balance cannot go below zero")
    void adjustPoints_negativeClampedToZero() {
        customer.setLoyaltyPoints(30);
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loyaltyService.adjustPoints(10L, -100, "Deduct");

        ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(cap.capture());
        assertThat(cap.getValue().getLoyaltyPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("adjustPoints: throws when customer not found")
    void adjustPoints_customerNotFound() {
        when(customerRepository.findByIdActive(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.adjustPoints(99L, 50, "Test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getProgram ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProgram returns active program when one exists")
    void getProgram_returnsActiveProgram() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));

        var result = loyaltyService.getProgram();

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getProgram returns default when no program configured")
    void getProgram_returnsDefaultWhenNone() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.empty());

        var result = loyaltyService.getProgram();

        assertThat(result).isNotNull();
    }

    // ── saveProgram ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveProgram updates existing program with new values")
    void saveProgram_updatesExisting() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(programRepository.save(any(LoyaltyProgram.class))).thenReturn(activeProgram);

        SaveLoyaltyProgramRequest req = SaveLoyaltyProgramRequest.builder()
                .pointsPerAmount(2)
                .isActive(true)
                .build();

        var result = loyaltyService.saveProgram(req);

        assertThat(result).isNotNull();
        verify(programRepository).save(argThat(p -> p.getPointsPerAmount() == 2));
    }

    @Test
    @DisplayName("saveProgram creates new program when none exists")
    void saveProgram_createsNew() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.empty());
        when(programRepository.save(any(LoyaltyProgram.class))).thenReturn(activeProgram);

        SaveLoyaltyProgramRequest req = new SaveLoyaltyProgramRequest();

        loyaltyService.saveProgram(req);

        verify(programRepository).save(any(LoyaltyProgram.class));
    }

    // ── getTiers ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTiers returns all active tiers")
    void getTiers_returnsList() {
        LoyaltyTier tier = LoyaltyTier.builder()
                .name("Silver").minSpend(BigDecimal.ZERO).sortOrder(0).build();
        tier.setId(1L);
        tier.setDeleted(false);
        when(tierRepository.findAllActive()).thenReturn(List.of(tier));

        var result = loyaltyService.getTiers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Silver");
    }

    // ── createTier ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTier saves new tier with provided fields")
    void createTier_success() {
        LoyaltyTier tier = LoyaltyTier.builder()
                .name("Gold").minSpend(new BigDecimal("1000000")).sortOrder(1).build();
        tier.setId(2L);
        tier.setDeleted(false);
        when(tierRepository.save(any(LoyaltyTier.class))).thenReturn(tier);

        CreateLoyaltyTierRequest req = CreateLoyaltyTierRequest.builder()
                .name("Gold")
                .minSpend(new BigDecimal("1000000"))
                .sortOrder(1)
                .build();

        var result = loyaltyService.createTier(req);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Gold");
    }

    @Test
    @DisplayName("createTier applies defaults for null fields")
    void createTier_appliesDefaults() {
        LoyaltyTier tier = LoyaltyTier.builder().name("Basic").build();
        tier.setId(3L);
        tier.setDeleted(false);
        when(tierRepository.save(any(LoyaltyTier.class))).thenReturn(tier);

        CreateLoyaltyTierRequest req = new CreateLoyaltyTierRequest();
        req.setName("Basic");

        loyaltyService.createTier(req);

        verify(tierRepository).save(argThat(t ->
                t.getMinSpend().compareTo(BigDecimal.ZERO) == 0 &&
                t.getPointsMultiplier().compareTo(BigDecimal.ONE) == 0 &&
                "#9E9E9E".equals(t.getColor())));
    }

    // ── updateTier ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTier updates fields and saves")
    void updateTier_success() {
        LoyaltyTier tier = LoyaltyTier.builder().name("Silver").build();
        tier.setId(1L);
        tier.setDeleted(false);
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier));
        when(tierRepository.save(any(LoyaltyTier.class))).thenReturn(tier);

        CreateLoyaltyTierRequest req = new CreateLoyaltyTierRequest();
        req.setName("Gold");

        var result = loyaltyService.updateTier(1L, req);

        assertThat(result).isNotNull();
        verify(tierRepository).save(argThat(t -> "Gold".equals(t.getName())));
    }

    @Test
    @DisplayName("updateTier throws when tier not found")
    void updateTier_notFound() {
        when(tierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.updateTier(99L, new CreateLoyaltyTierRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteTier ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTier soft-deletes the tier")
    void deleteTier_success() {
        LoyaltyTier tier = LoyaltyTier.builder().name("Silver").build();
        tier.setId(1L);
        tier.setDeleted(false);
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier));
        when(tierRepository.save(any(LoyaltyTier.class))).thenReturn(tier);

        loyaltyService.deleteTier(1L);

        assertThat(tier.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteTier throws when tier not found")
    void deleteTier_notFound() {
        when(tierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.deleteTier(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getCustomerLoyalty ────────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomerLoyalty returns summary for customer")
    void getCustomerLoyalty_success() {
        customer.setId(1L);
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(tierRepository.findTierForSpend(any())).thenReturn(Optional.empty());
        when(tierRepository.findNextTierForSpend(any())).thenReturn(Optional.empty());

        var result = loyaltyService.getCustomerLoyalty(1L);

        assertThat(result).isNotNull();
        assertThat(result.getLoyaltyPoints()).isEqualTo(200);
    }

    @Test
    @DisplayName("getCustomerLoyalty throws when customer not found")
    void getCustomerLoyalty_notFound() {
        when(customerRepository.findByIdActive(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.getCustomerLoyalty(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getTransactionHistory ─────────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionHistory returns paged transactions")
    void getTransactionHistory_success() {
        var pageable = PageRequest.of(0, 10);
        var transaction = LoyaltyTransaction.builder()
                .customerId(1L).points(100).type(LoyaltyTransactionType.EARNED).build();
        when(transactionRepository.findByCustomerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        var result = loyaltyService.getTransactionHistory(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── backfillRedemptionOrderId ─────────────────────────────────────────────

    @Test
    @DisplayName("backfillRedemptionOrderId: sets orderId on most recent redemption without order")
    void backfillRedemptionOrderId_found_updatesOrderId() {
        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .customerId(1L).points(-100).type(LoyaltyTransactionType.REDEEMED).build();
        when(transactionRepository.findTopRedemptionWithoutOrder(1L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        loyaltyService.backfillRedemptionOrderId(1L, 55L);

        verify(transactionRepository).save(argThat(t -> Long.valueOf(55L).equals(t.getOrderId())));
    }

    @Test
    @DisplayName("backfillRedemptionOrderId: no-op when no unlinked redemption exists")
    void backfillRedemptionOrderId_notFound_noOp() {
        when(transactionRepository.findTopRedemptionWithoutOrder(1L)).thenReturn(Optional.empty());

        loyaltyService.backfillRedemptionOrderId(1L, 55L);

        verify(transactionRepository, never()).save(any());
    }

    // ── awardPointsForOrder edge cases ────────────────────────────────────────

    @Test
    @DisplayName("awardPointsForOrder: skips when customer not found")
    void awardPoints_customerNotFound_noOp() {
        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(99L)).thenReturn(Optional.empty());

        loyaltyService.awardPointsForOrder(99L, 1L, new BigDecimal("100000"));

        verify(customerRepository, never()).save(any());
    }

    // ── redeemPoints edge cases ───────────────────────────────────────────────

    @Test
    @DisplayName("redeemPoints: throws ResourceNotFoundException when customer not found")
    void redeemPoints_customerNotFound_throws() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(customerRepository.findByIdActive(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.redeemPoints(99L, 100, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── saveProgram all-fields coverage ───────────────────────────────────────

    @Test
    @DisplayName("saveProgram updates all null-checked fields when all provided")
    void saveProgram_allFields_updatesAll() {
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(activeProgram));
        when(programRepository.save(any(LoyaltyProgram.class))).thenReturn(activeProgram);

        SaveLoyaltyProgramRequest req = SaveLoyaltyProgramRequest.builder()
                .pointsPerAmount(3)
                .amountPerPoints(5000L)
                .redemptionPointsPerDiscount(50)
                .redemptionDiscountAmount(new BigDecimal("5000"))
                .minRedemptionPoints(50)
                .isActive(false)
                .build();

        loyaltyService.saveProgram(req);

        verify(programRepository).save(argThat(p ->
                p.getPointsPerAmount() == 3 &&
                p.getAmountPerPoints() == 5000L &&
                p.getRedemptionPointsPerDiscount() == 50));
    }

    // ── updateTier all-fields coverage ────────────────────────────────────────

    @Test
    @DisplayName("updateTier updates all fields when all provided in request")
    void updateTier_allFields_updatesAll() {
        LoyaltyTier tier = LoyaltyTier.builder().name("Silver").build();
        tier.setId(1L);
        tier.setDeleted(false);
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier));
        when(tierRepository.save(any(LoyaltyTier.class))).thenReturn(tier);

        CreateLoyaltyTierRequest req = CreateLoyaltyTierRequest.builder()
                .name("Platinum")
                .minSpend(new BigDecimal("5000000"))
                .pointsMultiplier(new BigDecimal("2.0"))
                .color("#FFD700")
                .description("Top tier customers")
                .sortOrder(3)
                .build();

        loyaltyService.updateTier(1L, req);

        verify(tierRepository).save(argThat(t ->
                "Platinum".equals(t.getName()) &&
                t.getMinSpend().compareTo(new BigDecimal("5000000")) == 0 &&
                "#FFD700".equals(t.getColor()) &&
                "Top tier customers".equals(t.getDescription()) &&
                t.getSortOrder() == 3));
    }

    // ── awardPointsForOrder: inactive program ─────────────────────────────────

    @Test
    @DisplayName("awardPointsForOrder: skips when program is found but inactive")
    void awardPointsForOrder_inactiveProgram_skips() {
        LoyaltyProgram inactiveProgram = LoyaltyProgram.builder()
                .pointsPerAmount(1).amountPerPoints(10000L)
                .redemptionPointsPerDiscount(100)
                .redemptionDiscountAmount(new BigDecimal("10000"))
                .minRedemptionPoints(100)
                .isActive(false)
                .build();

        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(programRepository.findActiveProgram()).thenReturn(Optional.of(inactiveProgram));

        loyaltyService.awardPointsForOrder(10L, 1L, new BigDecimal("100000"));

        verify(customerRepository, never()).save(any());
    }

    // ── getCustomerLoyalty: nextTier present ──────────────────────────────────

    @Test
    @DisplayName("getCustomerLoyalty: computes amountToNextTier when next tier is available")
    void getCustomerLoyalty_withNextTier_computesAmount() {
        customer.setId(1L);
        LoyaltyTier nextTierEntity = LoyaltyTier.builder()
                .name("Gold").minSpend(new BigDecimal("1000000")).sortOrder(2).build();
        nextTierEntity.setId(2L);
        nextTierEntity.setDeleted(false);
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(tierRepository.findTierForSpend(any())).thenReturn(Optional.empty());
        when(tierRepository.findNextTierForSpend(any())).thenReturn(Optional.of(nextTierEntity));

        var result = loyaltyService.getCustomerLoyalty(1L);

        // amountToNext = 1000000 - 500000 = 500000
        assertThat(result.getAmountToNextTier()).isEqualByComparingTo("500000");
        assertThat(result.getNextTier()).isNotNull();
        assertThat(result.getNextTier().getName()).isEqualTo("Gold");
    }

    // ── adjustPoints: null description ────────────────────────────────────────

    @Test
    @DisplayName("adjustPoints: uses default description when null is passed")
    void adjustPoints_nullDescription_usesDefault() {
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var tx = loyaltyService.adjustPoints(10L, 50, null);

        verify(transactionRepository).save(argThat(
                t -> "Điều chỉnh điểm thủ công".equals(t.getDescription())));
    }
}
