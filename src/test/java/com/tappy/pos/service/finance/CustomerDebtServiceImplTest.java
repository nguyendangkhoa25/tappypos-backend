package com.tappy.pos.service.finance;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.finance.CreateDebtRequest;
import com.tappy.pos.model.dto.finance.CustomerDebtDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtStatementDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtSummaryDTO;
import com.tappy.pos.model.dto.finance.DebtPaymentDTO;
import com.tappy.pos.model.dto.finance.RecordDebtPaymentRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.finance.CustomerDebt;
import com.tappy.pos.model.entity.finance.DebtPayment;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.DebtStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.finance.CustomerDebtRepository;
import com.tappy.pos.repository.finance.DebtPaymentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerDebtServiceImpl Unit Tests")
class CustomerDebtServiceImplTest {

    private static final String TENANT = "test-tenant";
    private static final String USER = "user1";

    @Mock private CustomerDebtRepository debtRepository;
    @Mock private DebtPaymentRepository paymentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private MessageService messageService;

    @InjectMocks
    private CustomerDebtServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(authContext.getCurrentUsername()).thenReturn(USER);
        lenient().when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    private Customer customer(Long id, String name) {
        Customer c = new Customer();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private CustomerDebt debt(Long id, Long customerId, String name, BigDecimal original,
                              BigDecimal paid, BigDecimal outstanding, DebtStatus status) {
        return CustomerDebt.builder()
                .id(id)
                .tenantId(TENANT)
                .customerId(customerId)
                .customerName(name)
                .originalAmount(original)
                .paidAmount(paid)
                .outstandingAmount(outstanding)
                .status(status)
                .createdBy(USER)
                .build();
    }

    // ── getBalances ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBalances")
    class GetBalances {

        @Test
        @DisplayName("maps rows; overdue when earliest due date is in the past")
        void mapsRowsAndOverdue() {
            LocalDate past = LocalDate.now().minusDays(5);
            Object[] row = {10L, "Anh Ba", new BigDecimal("300000"), 2, Date.valueOf(past)};
            when(debtRepository.findOutstandingBalancesByCustomer(TENANT))
                    .thenReturn(List.<Object[]>of(row));

            List<CustomerDebtSummaryDTO> result = service.getBalances();

            assertThat(result).hasSize(1);
            CustomerDebtSummaryDTO dto = result.get(0);
            assertThat(dto.getCustomerId()).isEqualTo(10L);
            assertThat(dto.getCustomerName()).isEqualTo("Anh Ba");
            assertThat(dto.getTotalOutstanding()).isEqualByComparingTo("300000");
            assertThat(dto.getDebtCount()).isEqualTo(2);
            assertThat(dto.getEarliestDueDate()).isEqualTo(past);
            assertThat(dto.isOverdue()).isTrue();
        }

        @Test
        @DisplayName("not overdue when due date is in the future and handles null due date")
        void notOverdueAndNullDueDate() {
            LocalDate future = LocalDate.now().plusDays(5);
            Object[] withDue = {10L, "A", new BigDecimal("1"), 1, Date.valueOf(future)};
            Object[] noDue = {11L, "B", new BigDecimal("2"), 1, null};
            when(debtRepository.findOutstandingBalancesByCustomer(TENANT))
                    .thenReturn(List.of(withDue, noDue));

            List<CustomerDebtSummaryDTO> result = service.getBalances();

            assertThat(result.get(0).isOverdue()).isFalse();
            assertThat(result.get(0).getEarliestDueDate()).isEqualTo(future);
            assertThat(result.get(1).getEarliestDueDate()).isNull();
            assertThat(result.get(1).isOverdue()).isFalse();
        }

        @Test
        @DisplayName("returns empty list when no rows")
        void empty() {
            when(debtRepository.findOutstandingBalancesByCustomer(TENANT)).thenReturn(List.of());
            assertThat(service.getBalances()).isEmpty();
        }
    }

    // ── getCustomerStatement ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerStatement")
    class GetCustomerStatement {

        @Test
        @DisplayName("uses last debt's customer name and maps debts + payments")
        void withDebts() {
            CustomerDebt d1 = debt(1L, 5L, "Tên Cũ", new BigDecimal("100"),
                    BigDecimal.ZERO, new BigDecimal("100"), DebtStatus.OPEN);
            CustomerDebt d2 = debt(2L, 5L, "Tên Mới", new BigDecimal("200"),
                    BigDecimal.ZERO, new BigDecimal("200"), DebtStatus.OPEN);
            DebtPayment p = DebtPayment.builder()
                    .id(9L).customerId(5L).amount(new BigDecimal("50"))
                    .method("CASH").paidAt(LocalDateTime.now()).createdBy(USER).build();

            when(debtRepository.findByCustomerIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(5L, TENANT))
                    .thenReturn(List.of(d1, d2));
            when(paymentRepository.findByCustomerIdAndTenantIdOrderByPaidAtDesc(5L, TENANT))
                    .thenReturn(List.of(p));
            when(debtRepository.sumOutstandingByCustomer(TENANT, 5L)).thenReturn(new BigDecimal("300"));

            CustomerDebtStatementDTO stmt = service.getCustomerStatement(5L);

            assertThat(stmt.getCustomerId()).isEqualTo(5L);
            assertThat(stmt.getCustomerName()).isEqualTo("Tên Mới");
            assertThat(stmt.getTotalOutstanding()).isEqualByComparingTo("300");
            assertThat(stmt.getDebts()).hasSize(2);
            assertThat(stmt.getPayments()).hasSize(1);
            assertThat(stmt.getPayments().get(0).getAmount()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("falls back to customer repository name when no debts; null-safe outstanding")
        void noDebtsFallsBackToCustomerName() {
            when(debtRepository.findByCustomerIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(7L, TENANT))
                    .thenReturn(List.of());
            when(paymentRepository.findByCustomerIdAndTenantIdOrderByPaidAtDesc(7L, TENANT))
                    .thenReturn(List.of());
            when(debtRepository.sumOutstandingByCustomer(TENANT, 7L)).thenReturn(null);
            when(customerRepository.findByIdActiveAndTenantId(7L, TENANT))
                    .thenReturn(Optional.of(customer(7L, "Khách Mới")));

            CustomerDebtStatementDTO stmt = service.getCustomerStatement(7L);

            assertThat(stmt.getCustomerName()).isEqualTo("Khách Mới");
            assertThat(stmt.getTotalOutstanding()).isEqualByComparingTo("0");
            assertThat(stmt.getDebts()).isEmpty();
        }

        @Test
        @DisplayName("null customer name when no debts and customer not found")
        void noDebtsAndCustomerNotFound() {
            when(debtRepository.findByCustomerIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(8L, TENANT))
                    .thenReturn(List.of());
            when(paymentRepository.findByCustomerIdAndTenantIdOrderByPaidAtDesc(8L, TENANT))
                    .thenReturn(List.of());
            when(debtRepository.sumOutstandingByCustomer(TENANT, 8L)).thenReturn(BigDecimal.ZERO);
            when(customerRepository.findByIdActiveAndTenantId(8L, TENANT)).thenReturn(Optional.empty());

            CustomerDebtStatementDTO stmt = service.getCustomerStatement(8L);

            assertThat(stmt.getCustomerName()).isNull();
        }
    }

    // ── createDebt ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createDebt")
    class CreateDebt {

        private CreateDebtRequest request(Long customerId, BigDecimal amount) {
            CreateDebtRequest r = new CreateDebtRequest();
            r.setCustomerId(customerId);
            r.setAmount(amount);
            r.setOrderId(99L);
            r.setOrderNumber("DH-1");
            r.setDueDate(LocalDate.now().plusDays(30));
            r.setNote("ghi nợ");
            return r;
        }

        @Test
        @DisplayName("creates debt with OPEN status, full outstanding, zero paid, logs activity")
        void success() {
            CreateDebtRequest req = request(5L, new BigDecimal("250000"));
            when(customerRepository.findByIdActiveAndTenantId(5L, TENANT))
                    .thenReturn(Optional.of(customer(5L, "Chị Tư")));
            when(debtRepository.save(any(CustomerDebt.class))).thenAnswer(i -> {
                CustomerDebt d = i.getArgument(0);
                d.setId(42L);
                return d;
            });

            CustomerDebtDTO dto = service.createDebt(req);

            ArgumentCaptor<CustomerDebt> cap = ArgumentCaptor.forClass(CustomerDebt.class);
            verify(debtRepository).save(cap.capture());
            CustomerDebt saved = cap.getValue();
            assertThat(saved.getCustomerId()).isEqualTo(5L);
            assertThat(saved.getCustomerName()).isEqualTo("Chị Tư");
            assertThat(saved.getOrderId()).isEqualTo(99L);
            assertThat(saved.getOrderNumber()).isEqualTo("DH-1");
            assertThat(saved.getOriginalAmount()).isEqualByComparingTo("250000");
            assertThat(saved.getOutstandingAmount()).isEqualByComparingTo("250000");
            assertThat(saved.getPaidAmount()).isEqualByComparingTo("0");
            assertThat(saved.getStatus()).isEqualTo(DebtStatus.OPEN);
            assertThat(saved.getCreatedBy()).isEqualTo(USER);
            assertThat(dto.getId()).isEqualTo(42L);

            verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null),
                    eq(ActivityAction.DEBT_CREATED), eq("CUSTOMER_DEBT"), eq("42"),
                    eq("activity.debt.created"), eq(null), eq("Chị Tư"), eq(new BigDecimal("250000")));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when customer not found")
        void customerNotFound() {
            CreateDebtRequest req = request(404L, new BigDecimal("1"));
            when(customerRepository.findByIdActiveAndTenantId(404L, TENANT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createDebt(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("error.customer.not.found");

            verify(debtRepository, never()).save(any());
            verifyNoInteractions(activityLogService);
        }
    }

    // ── recordPayment ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordPayment")
    class RecordPayment {

        private RecordDebtPaymentRequest request(Long customerId, BigDecimal amount, String method) {
            RecordDebtPaymentRequest r = new RecordDebtPaymentRequest();
            r.setCustomerId(customerId);
            r.setAmount(amount);
            r.setMethod(method);
            r.setNote("thu nợ");
            return r;
        }

        @Test
        @DisplayName("allocates oldest-first; fully pays first debt and partially pays second")
        void allocatesOldestFirst() {
            RecordDebtPaymentRequest req = request(5L, new BigDecimal("150"), "TRANSFER");
            when(debtRepository.sumOutstandingByCustomer(TENANT, 5L)).thenReturn(new BigDecimal("300"));

            CustomerDebt d1 = debt(1L, 5L, "A", new BigDecimal("100"),
                    BigDecimal.ZERO, new BigDecimal("100"), DebtStatus.OPEN);
            CustomerDebt d2 = debt(2L, 5L, "A", new BigDecimal("200"),
                    BigDecimal.ZERO, new BigDecimal("200"), DebtStatus.OPEN);
            when(debtRepository.findByCustomerIdAndTenantIdAndStatusNotAndDeletedFalseOrderByCreatedAtAsc(
                    5L, TENANT, DebtStatus.PAID)).thenReturn(new ArrayList<>(List.of(d1, d2)));
            when(debtRepository.save(any(CustomerDebt.class))).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(i -> i.getArgument(0));

            DebtPaymentDTO dto = service.recordPayment(req);

            // d1 fully paid
            assertThat(d1.getPaidAmount()).isEqualByComparingTo("100");
            assertThat(d1.getOutstandingAmount()).isEqualByComparingTo("0");
            assertThat(d1.getStatus()).isEqualTo(DebtStatus.PAID);
            // d2 partially paid by remaining 50
            assertThat(d2.getPaidAmount()).isEqualByComparingTo("50");
            assertThat(d2.getOutstandingAmount()).isEqualByComparingTo("150");
            assertThat(d2.getStatus()).isEqualTo(DebtStatus.PARTIAL);

            ArgumentCaptor<DebtPayment> cap = ArgumentCaptor.forClass(DebtPayment.class);
            verify(paymentRepository).save(cap.capture());
            DebtPayment pay = cap.getValue();
            assertThat(pay.getCustomerId()).isEqualTo(5L);
            assertThat(pay.getDebtId()).isNull();
            assertThat(pay.getAmount()).isEqualByComparingTo("150");
            assertThat(pay.getMethod()).isEqualTo("TRANSFER");
            assertThat(pay.getCreatedBy()).isEqualTo(USER);
            assertThat(dto.getAmount()).isEqualByComparingTo("150");

            verify(debtRepository, times(2)).save(any(CustomerDebt.class));
            verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null),
                    eq(ActivityAction.DEBT_PAYMENT), eq("CUSTOMER_DEBT"), eq("5"),
                    eq("activity.debt.payment"), eq(null), eq(new BigDecimal("150")));
        }

        @Test
        @DisplayName("defaults method to CASH when absent; stops allocating once remaining is zero")
        void defaultMethodAndStopsWhenSettled() {
            RecordDebtPaymentRequest req = request(5L, new BigDecimal("100"), null);
            when(debtRepository.sumOutstandingByCustomer(TENANT, 5L)).thenReturn(new BigDecimal("300"));

            CustomerDebt d1 = debt(1L, 5L, "A", new BigDecimal("100"),
                    BigDecimal.ZERO, new BigDecimal("100"), DebtStatus.OPEN);
            CustomerDebt d2 = debt(2L, 5L, "A", new BigDecimal("200"),
                    BigDecimal.ZERO, new BigDecimal("200"), DebtStatus.OPEN);
            when(debtRepository.findByCustomerIdAndTenantIdAndStatusNotAndDeletedFalseOrderByCreatedAtAsc(
                    5L, TENANT, DebtStatus.PAID)).thenReturn(new ArrayList<>(List.of(d1, d2)));
            when(debtRepository.save(any(CustomerDebt.class))).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(i -> i.getArgument(0));

            DebtPaymentDTO dto = service.recordPayment(req);

            assertThat(dto.getMethod()).isEqualTo("CASH");
            // only first debt touched; second untouched because remaining hit zero
            assertThat(d1.getStatus()).isEqualTo(DebtStatus.PAID);
            assertThat(d2.getOutstandingAmount()).isEqualByComparingTo("200");
            assertThat(d2.getStatus()).isEqualTo(DebtStatus.OPEN);
            verify(debtRepository, times(1)).save(any(CustomerDebt.class));
        }

        @Test
        @DisplayName("throws when there is no outstanding balance")
        void noOutstanding() {
            RecordDebtPaymentRequest req = request(5L, new BigDecimal("50"), "CASH");
            when(debtRepository.sumOutstandingByCustomer(TENANT, 5L)).thenReturn(BigDecimal.ZERO);

            assertThatThrownBy(() -> service.recordPayment(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("error.debt.no_outstanding");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when outstanding is null (treated as zero)")
        void nullOutstanding() {
            RecordDebtPaymentRequest req = request(5L, new BigDecimal("50"), "CASH");
            when(debtRepository.sumOutstandingByCustomer(TENANT, 5L)).thenReturn(null);

            assertThatThrownBy(() -> service.recordPayment(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("error.debt.no_outstanding");
        }

        @Test
        @DisplayName("throws when payment exceeds outstanding")
        void exceedsOutstanding() {
            RecordDebtPaymentRequest req = request(5L, new BigDecimal("500"), "CASH");
            when(debtRepository.sumOutstandingByCustomer(TENANT, 5L)).thenReturn(new BigDecimal("300"));

            assertThatThrownBy(() -> service.recordPayment(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("error.debt.payment_exceeds_outstanding");

            verify(paymentRepository, never()).save(any());
        }
    }

    // ── getTotalOutstanding ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTotalOutstanding")
    class GetTotalOutstanding {

        @Test
        @DisplayName("returns repository sum")
        void returnsSum() {
            when(debtRepository.sumTotalOutstanding(TENANT)).thenReturn(new BigDecimal("12345"));
            assertThat(service.getTotalOutstanding()).isEqualByComparingTo("12345");
        }

        @Test
        @DisplayName("null-safe: returns zero when repository returns null")
        void nullSafe() {
            when(debtRepository.sumTotalOutstanding(TENANT)).thenReturn(null);
            assertThat(service.getTotalOutstanding()).isEqualByComparingTo("0");
        }
    }

    // ── deleteDebt ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteDebt")
    class DeleteDebt {

        @Test
        @DisplayName("soft-deletes the debt and logs activity")
        void success() {
            CustomerDebt d = debt(3L, 5L, "Anh Năm", new BigDecimal("100"),
                    BigDecimal.ZERO, new BigDecimal("100"), DebtStatus.OPEN);
            when(debtRepository.findByIdAndTenantIdAndDeletedFalse(3L, TENANT)).thenReturn(Optional.of(d));
            when(debtRepository.save(any(CustomerDebt.class))).thenAnswer(i -> i.getArgument(0));

            service.deleteDebt(3L);

            assertThat(d.isDeleted()).isTrue();
            verify(debtRepository).save(d);
            verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null),
                    eq(ActivityAction.DEBT_DELETED), eq("CUSTOMER_DEBT"), eq("3"),
                    eq("activity.debt.deleted"), eq(null), eq("Anh Năm"));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when debt not found")
        void notFound() {
            when(debtRepository.findByIdAndTenantIdAndDeletedFalse(404L, TENANT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteDebt(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("error.debt.not_found");

            verify(debtRepository, never()).save(any());
        }
    }
}
