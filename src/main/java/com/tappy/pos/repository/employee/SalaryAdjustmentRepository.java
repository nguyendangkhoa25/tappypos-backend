package com.tappy.pos.repository.employee;

import com.tappy.pos.model.entity.employee.SalaryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryAdjustmentRepository extends JpaRepository<SalaryAdjustment, Long> {

    List<SalaryAdjustment> findBySalaryIdOrderByCreatedAtAsc(Long salaryId);

    Optional<SalaryAdjustment> findByIdAndSalaryId(Long id, Long salaryId);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM salary_adjustment WHERE salary_id = :salaryId AND type = :type",
           nativeQuery = true)
    BigDecimal sumByType(@Param("salaryId") Long salaryId, @Param("type") String type);
}
