package com.knp.service.finance;

import com.knp.model.dto.revenue.CategoryRevenueDTO;
import com.knp.model.dto.revenue.DayOfWeekRevenueDTO;
import com.knp.model.dto.revenue.HourlyRevenueDTO;
import com.knp.model.dto.revenue.PaymentBreakdownDTO;
import com.knp.model.dto.revenue.RevenueOverviewDTO;
import com.knp.model.dto.revenue.RevenuePeriodDTO;
import com.knp.model.dto.revenue.TopEmployeeDTO;
import com.knp.model.dto.revenue.TopProductDTO;

import java.util.List;

public interface RevenueService {

    RevenueOverviewDTO getOverview();

    List<RevenuePeriodDTO> getMonthlyBreakdown(int year);

    List<RevenuePeriodDTO> getDailyBreakdown(int year, int month);

    List<TopProductDTO> getTopProducts(Integer year, Integer month, int limit);

    List<PaymentBreakdownDTO> getPaymentBreakdown(Integer year, Integer month);

    List<DayOfWeekRevenueDTO> getDayOfWeekBreakdown(Integer year, Integer month);

    List<HourlyRevenueDTO> getHourlyBreakdown(Integer year, Integer month);

    List<CategoryRevenueDTO> getCategoryBreakdown(Integer year, Integer month);

    List<TopEmployeeDTO> getTopEmployees(Integer year, Integer month, int limit);
}
