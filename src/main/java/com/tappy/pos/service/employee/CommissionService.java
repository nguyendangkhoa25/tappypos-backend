package com.tappy.pos.service.employee;

import com.tappy.pos.model.dto.employee.CommissionReportDTO;
import com.tappy.pos.model.dto.employee.MyCommissionDTO;

public interface CommissionService {

    /**
     * Returns the commission summary + item list for the authenticated user.
     * Throws ResourceNotFoundException (404) if the user is not linked to an employee record.
     */
    MyCommissionDTO getMyCommission(String username, int month, int year);

    /**
     * Returns a team-wide commission report for the given month/year.
     * Requires COMMISSION_VIEW_ALL feature.
     */
    CommissionReportDTO getCommissionReport(int month, int year);
}
