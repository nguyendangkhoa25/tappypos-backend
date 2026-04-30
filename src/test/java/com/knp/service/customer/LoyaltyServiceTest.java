package com.knp.service.customer;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.entity.customer.Customer;
import com.knp.model.entity.customer.LoyaltyProgram;
import com.knp.model.entity.customer.LoyaltyTier;
import com.knp.model.entity.customer.LoyaltyTransaction;
import com.knp.model.enums.LoyaltyTransactionType;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.customer.LoyaltyProgramRepository;
import com.knp.repository.customer.LoyaltyTierRepository;
import com.knp.repository.customer.LoyaltyTransactionRepository;
import com.knp.service.MessageService;
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
}
