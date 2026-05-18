package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.revenue.CategoryRevenueDTO;
import com.tappy.pos.model.dto.revenue.DayOfWeekRevenueDTO;
import com.tappy.pos.model.dto.revenue.HourlyRevenueDTO;
import com.tappy.pos.model.dto.revenue.PaymentBreakdownDTO;
import com.tappy.pos.model.dto.revenue.RevenueOverviewDTO;
import com.tappy.pos.model.dto.revenue.RevenuePeriodDTO;
import com.tappy.pos.model.dto.revenue.TopEmployeeDTO;
import com.tappy.pos.model.dto.revenue.TopProductDTO;

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
