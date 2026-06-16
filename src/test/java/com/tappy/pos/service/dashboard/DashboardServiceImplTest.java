package com.tappy.pos.service.dashboard;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.dto.dashboard.DashboardKpiDTO;
import com.tappy.pos.model.dto.dashboard.DashboardSummaryDTO;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Tests")
class DashboardServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PawnRepository pawnRepository;
    @Mock private FeatureContext featureContext;

    @InjectMocks
    private DashboardServiceImpl service;

    private void stubSummaryQueries() {
        lenient().when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("10000000"));
        lenient().when(orderRepository.sumRevenueByMonth(anyInt(), anyInt())).thenReturn(new BigDecimal("2000000"));
        lenient().when(orderRepository.sumRevenueByYear(anyInt())).thenReturn(new BigDecimal("8000000"));
        lenient().when(orderRepository.countCompleted()).thenReturn(120L);
        lenient().when(orderRepository.countByDeletedFalseAndStatus(any())).thenReturn(5L);
        lenient().when(customerRepository.countAllActive()).thenReturn(40L);
        lenient().when(orderItemRepository.sumTotalItemsSold()).thenReturn(300L);
        lenient().when(orderItemRepository.sumItemsSoldByMonth(anyInt(), anyInt())).thenReturn(60L);
        lenient().when(orderItemRepository.sumItemsSoldByYear(anyInt())).thenReturn(250L);
        lenient().when(orderRepository.findRecentCompleted(any())).thenReturn(List.of());
        lenient().when(employeeRepository.countAllActive()).thenReturn(8L);
        lenient().when(employeeRepository.countActive()).thenReturn(7L);
        lenient().when(employeeRepository.countInactive()).thenReturn(1L);
    }

    @Test
    @DisplayName("getSummary: without PAWN feature, pawn metrics stay null")
    void getSummary_noPawn() {
        stubSummaryQueries();
        when(featureContext.hasFeature("PAWN")).thenReturn(false);

        DashboardSummaryDTO dto = service.getSummary();

        assertThat(dto.getTotalRevenue()).isEqualByComparingTo("10000000");
        assertThat(dto.getPendingOrders()).isEqualTo(5L);
        assertThat(dto.getTotalCustomers()).isEqualTo(40L);
        assertThat(dto.getActivePawnContracts()).isNull();
        assertThat(dto.getRecentOrders()).isEmpty();
    }

    @Test
    @DisplayName("getSummary: with PAWN feature, pawn metrics populated")
    void getSummary_withPawn() {
        stubSummaryQueries();
        when(featureContext.hasFeature("PAWN")).thenReturn(true);
        when(pawnRepository.countActivePawnContracts()).thenReturn(12L);
        when(pawnRepository.sumActivePawnAmount()).thenReturn(new BigDecimal("50000000"));
        when(pawnRepository.countNewPawnsByMonth(anyInt(), anyInt())).thenReturn(3L);
        when(pawnRepository.sumNewPawnAmountByMonth(anyInt(), anyInt())).thenReturn(new BigDecimal("9000000"));
        when(pawnRepository.sumInterestEarnedByMonth(anyInt(), anyInt())).thenReturn(400000L);

        DashboardSummaryDTO dto = service.getSummary();

        assertThat(dto.getActivePawnContracts()).isEqualTo(12L);
        assertThat(dto.getActivePawnAmount()).isEqualByComparingTo("50000000");
        assertThat(dto.getMonthInterestEarned()).isEqualTo(400000L);
    }

    @Test
    @DisplayName("getKpi: without PAWN feature, pawn KPIs stay null")
    void getKpi_noPawn() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(orderItemRepository.sumItemsSoldByDateRange(from, to)).thenReturn(75L);
        when(orderRepository.sumRevenueByDateRange(from, to)).thenReturn(new BigDecimal("3000000"));
        when(featureContext.hasFeature("PAWN")).thenReturn(false);

        DashboardKpiDTO dto = service.getKpi(from, to, "2026-06-01", "2026-06-07");

        assertThat(dto.getItemsSold()).isEqualTo(75L);
        assertThat(dto.getRevenue()).isEqualByComparingTo("3000000");
        assertThat(dto.getNewPawnContracts()).isNull();
        assertThat(dto.getFromDate()).isEqualTo("2026-06-01");
    }

    @Test
    @DisplayName("getKpi: with PAWN feature, pawn KPIs populated")
    void getKpi_withPawn() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(orderItemRepository.sumItemsSoldByDateRange(from, to)).thenReturn(75L);
        when(orderRepository.sumRevenueByDateRange(from, to)).thenReturn(new BigDecimal("3000000"));
        when(featureContext.hasFeature("PAWN")).thenReturn(true);
        when(pawnRepository.countNewPawnsByDateRange(from, to)).thenReturn(2L);
        when(pawnRepository.sumNewPawnAmountByDateRange(from, to)).thenReturn(new BigDecimal("6000000"));
        when(pawnRepository.sumInterestEarnedByDateRange(from, to)).thenReturn(150000L);

        DashboardKpiDTO dto = service.getKpi(from, to, "2026-06-01", "2026-06-07");

        assertThat(dto.getNewPawnContracts()).isEqualTo(2L);
        assertThat(dto.getInterestEarned()).isEqualTo(150000L);
    }
}
