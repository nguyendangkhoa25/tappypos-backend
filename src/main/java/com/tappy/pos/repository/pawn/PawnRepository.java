package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnEntity;
import com.tappy.pos.model.enums.PawnStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PawnRepository extends JpaRepository<PawnEntity, Long> {
    Optional<PawnEntity> findByLegacyId(String legacyId);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as totalAmount, COUNT(p.pawnId) as totalCount FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) ")
    List<Object[]> getPawnAmountByPawnStatus(PawnStatus pawnStatus, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(r.requestAmount), 0) FROM ReqMoneyEntity r where r.pawnId in (SELECT p.pawnId as totalCount FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true)) ")
    Long getPawnRequestAmountByPawnStatus(PawnStatus pawnStatus, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as totalAmount, COUNT(DISTINCT p.pawnId) as totalCount FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and p.pawnDueDate between :fromDate and :toDate")
    List<Object[]> sumByPawnStatusAndPawnDueDateBetween(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(sum(r.requestAmount), 0) as totalAmount FROM ReqMoneyEntity r  where r.pawnId in (SELECT p.pawnId FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and p.pawnDueDate between :fromDate and :toDate)")
    Long sumRequestAmountByPawnStatusAndPawnDueDateBetween(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as amount, COUNT(DISTINCT p.pawnId) as totalCount FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and p.pawnDueDate < :queryDate")
    List<Object[]> sumByPawnStatusAndPawnDueDateBefore(PawnStatus pawnStatus, LocalDateTime queryDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(sum(r.requestAmount), 0) FROM ReqMoneyEntity r where r.pawnId in(SELECT p.pawnId FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and p.pawnDueDate < :queryDate)")
    Long sumRequestAmountByPawnStatusAndPawnDueDateBefore(PawnStatus pawnStatus, LocalDateTime queryDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as amount, COUNT(DISTINCT p.pawnId) as totalCount FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and p.pawnDate between :fromDate and :toDate ")
    List<Object[]> sumByPawnStatusAndPawnDateBetween(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(sum(r.requestAmount), 0) as amount FROM ReqMoneyEntity r where r.pawnId in (select p.pawnId FROM PawnEntity p where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and p.pawnDate between :fromDate and :toDate) ")
    Long sumRequestAmountByPawnStatusAndPawnDateBetween(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    /** Counts all original pawn contracts created in the period regardless of current status (excludes extensions via originalId IS NULL). */
    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as amount, COUNT(DISTINCT p.pawnId) as totalCount FROM PawnEntity p WHERE p.originalId IS NULL AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) AND p.pawnDate BETWEEN :fromDate AND :toDate")
    List<Object[]> sumNewOriginalPawnsByPawnDate(LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(r.requestAmount), 0) FROM ReqMoneyEntity r WHERE r.pawnId IN (SELECT p.pawnId FROM PawnEntity p WHERE p.originalId IS NULL AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) AND p.pawnDate BETWEEN :fromDate AND :toDate)")
    Long sumRequestAmountForNewOriginalPawnsByPawnDate(LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(r.requestAmount), 0), COUNT(r.pawnId) as totalCount  FROM ReqMoneyEntity r join PawnEntity p on p.pawnId = r.pawnId where p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and r.requestDate between :fromDate and :toDate ")
    List<Object[]> sumRequestMoneyByPawnStatusAndRequestDateBetween(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as amount, COUNT(p.pawnId) as totalCount FROM PawnEntity p where p.pawnStatus in :pawnStatuses AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and (p.redeemDate between :fromDate and :toDate or p.forfeitedDate between :fromDate and :toDate) ")
    List<Object[]> sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List<PawnStatus> pawnStatuses, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT  COALESCE(sum(r.requestAmount), 0) as amount FROM ReqMoneyEntity r where r.pawnId in (SELECT p.pawnId FROM PawnEntity p where p.pawnStatus in :pawnStatuses AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and (p.redeemDate between :fromDate and :toDate or p.forfeitedDate between :fromDate and :toDate)) ")
    Long sumRequestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List<PawnStatus> pawnStatuses, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.interestAmount), 0) FROM PawnEntity p where p.pawnStatus in :pawnStatuses AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) and (p.redeemDate between :fromDate and :toDate or p.forfeitedDate between :fromDate and :toDate) ")
    Long sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List<PawnStatus> pawnStatuses, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) AS amount, " +
            "COUNT(DISTINCT p.pawnId) AS totalCount, YEAR(p.pawnDate) AS year, MONTH(p.pawnDate) AS month " +
            "FROM PawnEntity p " +
            "WHERE (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.pawnDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.pawnDate), MONTH(p.pawnDate)")
    List<Object[]> getAmountAndTotalCountByMonthPawnDateAndStatus(LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) AS amount, " +
            "COUNT(DISTINCT p.pawnId) AS totalCount, YEAR(p.redeemDate) AS year, MONTH(p.redeemDate) AS month " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus in :pawnStatuses " +
            "AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.redeemDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.redeemDate), MONTH(p.redeemDate)")
    List<Object[]> getAmountAndTotalCountByMonthRedeemDateAndStatus(List<PawnStatus> pawnStatuses, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) AS amount, " +
            "COUNT(DISTINCT p.pawnId) AS totalCount, YEAR(p.forfeitedDate) AS year, MONTH(p.forfeitedDate) AS month " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus in :pawnStatuses " +
            "AND p.forfeitedDate BETWEEN :fromDate AND :toDate " +
            "AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) " +
            "GROUP BY YEAR(p.forfeitedDate), MONTH(p.forfeitedDate)")
    List<Object[]> getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(List<PawnStatus> pawnStatuses, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT ifnull(SUM(p.interestAmount), 0), " +
            "COUNT(DISTINCT p.pawnId) AS totalCount, YEAR(p.redeemDate) AS year, MONTH(p.redeemDate) AS month " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = :pawnStatus " +
            "AND p.redeemDate between :fromDate and :toDate  " +
            "AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) " +
            "GROUP BY YEAR(p.redeemDate), MONTH(p.redeemDate)")
    List<Object[]> getRedeemedInterestAmount(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT ifnull(SUM(p.interestAmount), 0), " +
            "COUNT(DISTINCT p.pawnId) AS totalCount, YEAR(p.forfeitedDate) AS year, MONTH(p.forfeitedDate) AS month " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = :pawnStatus " +
            "AND p.forfeitedDate between :fromDate and :toDate " +
            "AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) " +
            "GROUP BY YEAR(p.forfeitedDate), MONTH(p.forfeitedDate)")
    List<Object[]> getForfeitedInterestAmount(PawnStatus pawnStatus, LocalDateTime fromDate, LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Modifying
    @Query("update PawnEntity p set p.visible = :visible where p.pawnId in (:pawnIds)")
    int updateVisibleStatus(@Param("pawnIds") List<Long> pawnIds, @Param("visible") boolean visible);

    @Query("SELECT COUNT(p.pawnId) FROM PawnEntity p WHERE p.pawnStatus = 'PAWNED' AND (p.visible IS NULL OR p.visible = true)")
    Long countActivePawnContracts();

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) FROM PawnEntity p WHERE p.pawnStatus = 'PAWNED' AND (p.visible IS NULL OR p.visible = true)")
    java.math.BigDecimal sumActivePawnAmount();

    @Query("SELECT COUNT(p.pawnId) FROM PawnEntity p WHERE p.pawnStatus <> 'CANCELLED' AND (p.visible IS NULL OR p.visible = true) AND YEAR(p.pawnDate) = :year AND MONTH(p.pawnDate) = :month")
    Long countNewPawnsByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) FROM PawnEntity p WHERE p.pawnStatus <> 'CANCELLED' AND (p.visible IS NULL OR p.visible = true) AND YEAR(p.pawnDate) = :year AND MONTH(p.pawnDate) = :month")
    java.math.BigDecimal sumNewPawnAmountByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(p.interestAmount), 0) FROM PawnEntity p WHERE p.pawnStatus = 'REDEEMED' AND (p.visible IS NULL OR p.visible = true) AND YEAR(p.redeemDate) = :year AND MONTH(p.redeemDate) = :month")
    Long sumInterestEarnedByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(p.pawnId) FROM PawnEntity p WHERE p.pawnStatus <> 'CANCELLED' AND (p.visible IS NULL OR p.visible = true) AND p.pawnDate >= :from AND p.pawnDate <= :to")
    Long countNewPawnsByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) FROM PawnEntity p WHERE p.pawnStatus <> 'CANCELLED' AND (p.visible IS NULL OR p.visible = true) AND p.pawnDate >= :from AND p.pawnDate <= :to")
    java.math.BigDecimal sumNewPawnAmountByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.interestAmount), 0) FROM PawnEntity p WHERE p.pawnStatus = 'REDEEMED' AND (p.visible IS NULL OR p.visible = true) AND p.redeemDate >= :from AND p.redeemDate <= :to")
    Long sumInterestEarnedByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query(" SELECT COUNT(DISTINCT p.pawnId) AS totalCount, SUM(p.pawnAmount + COALESCE(r.totalRequestAmount, 0)) AS amount, SUM(p.interestAmount) AS totalInterest FROM PawnEntity p LEFT JOIN (SELECT r.pawnId AS pawnId, SUM(r.requestAmount) AS totalRequestAmount FROM ReqMoneyEntity r GROUP BY r.pawnId) r ON r.pawnId = p.pawnId WHERE p.pawnStatus IN :pawnStatuses AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) AND p.customerId = :customerId")
    List<Object[]> sumByPawnStatusInAndCustomerIdEquals(
            @Param("customerId") long customerId,
            @Param("pawnStatuses") List<PawnStatus> pawnStatuses,
            @Param("excludeVisibleItem") boolean excludeVisibleItem
    );

    // ── Day-granularity chart queries ─────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0), COUNT(DISTINCT p.pawnId), " +
            "YEAR(p.pawnDate), MONTH(p.pawnDate), DAY(p.pawnDate) " +
            "FROM PawnEntity p " +
            "WHERE (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.pawnDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.pawnDate), MONTH(p.pawnDate), DAY(p.pawnDate) " +
            "ORDER BY YEAR(p.pawnDate), MONTH(p.pawnDate), DAY(p.pawnDate)")
    List<Object[]> getAmountByDayPawnDate(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0), COUNT(DISTINCT p.pawnId), " +
            "YEAR(p.redeemDate), MONTH(p.redeemDate), DAY(p.redeemDate) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus IN :pawnStatuses " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.redeemDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.redeemDate), MONTH(p.redeemDate), DAY(p.redeemDate) " +
            "ORDER BY YEAR(p.redeemDate), MONTH(p.redeemDate), DAY(p.redeemDate)")
    List<Object[]> getAmountByDayRedeemDate(
            @Param("pawnStatuses") List<PawnStatus> pawnStatuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0), COUNT(DISTINCT p.pawnId), " +
            "YEAR(p.forfeitedDate), MONTH(p.forfeitedDate), DAY(p.forfeitedDate) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus IN :pawnStatuses " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.forfeitedDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.forfeitedDate), MONTH(p.forfeitedDate), DAY(p.forfeitedDate) " +
            "ORDER BY YEAR(p.forfeitedDate), MONTH(p.forfeitedDate), DAY(p.forfeitedDate)")
    List<Object[]> getAmountByDayForfeitedDate(
            @Param("pawnStatuses") List<PawnStatus> pawnStatuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    @Query("SELECT IFNULL(SUM(p.interestAmount), 0), COUNT(DISTINCT p.pawnId), " +
            "YEAR(p.redeemDate), MONTH(p.redeemDate), DAY(p.redeemDate) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = :pawnStatus " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.redeemDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.redeemDate), MONTH(p.redeemDate), DAY(p.redeemDate) " +
            "ORDER BY YEAR(p.redeemDate), MONTH(p.redeemDate), DAY(p.redeemDate)")
    List<Object[]> getRedeemedInterestByDay(
            @Param("pawnStatus") PawnStatus pawnStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    @Query("SELECT IFNULL(SUM(p.interestAmount), 0), COUNT(DISTINCT p.pawnId), " +
            "YEAR(p.forfeitedDate), MONTH(p.forfeitedDate), DAY(p.forfeitedDate) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = :pawnStatus " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.forfeitedDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.forfeitedDate), MONTH(p.forfeitedDate), DAY(p.forfeitedDate) " +
            "ORDER BY YEAR(p.forfeitedDate), MONTH(p.forfeitedDate), DAY(p.forfeitedDate)")
    List<Object[]> getForfeitedInterestByDay(
            @Param("pawnStatus") PawnStatus pawnStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    // ── Top customers by pawn amount ──────────────────────────────────────────

    @Query("SELECT p.customerId, p.customerName, " +
            "COUNT(DISTINCT p.pawnId), " +
            "COALESCE(SUM(p.pawnAmount), 0), " +
            "COALESCE(SUM(p.interestAmount), 0) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'PAWNED' " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.pawnDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY p.customerId, p.customerName " +
            "ORDER BY COALESCE(SUM(p.pawnAmount), 0) DESC")
    List<Object[]> findTopCustomersByPawnAmount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible,
            Pageable pageable);

    @Query("SELECT p.customerId, p.customerName, " +
            "COUNT(DISTINCT p.pawnId), " +
            "COALESCE(SUM(p.pawnAmount), 0), " +
            "COALESCE(SUM(p.interestAmount), 0) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'PAWNED' " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.pawnDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY p.customerId, p.customerName " +
            "ORDER BY COUNT(DISTINCT p.pawnId) DESC")
    List<Object[]> findTopCustomersByPawnCount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible,
            Pageable pageable);

    @Query("SELECT p.customerId, p.customerName, " +
            "COUNT(DISTINCT p.pawnId), " +
            "COALESCE(SUM(p.pawnAmount), 0), " +
            "COALESCE(SUM(p.interestAmount), 0) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'REDEEMED' " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.redeemDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY p.customerId, p.customerName " +
            "ORDER BY COALESCE(SUM(p.pawnAmount), 0) DESC")
    List<Object[]> findTopCustomersByCompletedAmount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible,
            Pageable pageable);

    @Query("SELECT p.customerId, p.customerName, " +
            "COUNT(DISTINCT p.pawnId), " +
            "COALESCE(SUM(p.pawnAmount), 0), " +
            "COALESCE(SUM(p.interestAmount), 0) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'REDEEMED' " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.redeemDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY p.customerId, p.customerName " +
            "ORDER BY COUNT(DISTINCT p.pawnId) DESC")
    List<Object[]> findTopCustomersByCompletedCount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible,
            Pageable pageable);

    @Query("SELECT p.customerId, p.customerName, " +
            "COUNT(DISTINCT p.pawnId), " +
            "COALESCE(SUM(p.pawnAmount), 0), " +
            "COALESCE(SUM(p.interestAmount), 0) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'REDEEMED' " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.redeemDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY p.customerId, p.customerName " +
            "ORDER BY COALESCE(SUM(p.interestAmount), 0) DESC")
    List<Object[]> findTopCustomersByInterestAmount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.pawnAmount), 0) as amount, COUNT(DISTINCT p.pawnId) as totalCount FROM PawnEntity p WHERE p.pawnStatus = :pawnStatus AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) AND p.updatedAt BETWEEN :fromDate AND :toDate")
    List<Object[]> sumByPawnStatusAndUpdatedAtBetween(@Param("pawnStatus") PawnStatus pawnStatus, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    /** Sum interest collected from EXTENDED contracts whose updatedAt falls in the period. */
    @Query("SELECT COALESCE(SUM(p.interestAmount), 0) FROM PawnEntity p WHERE p.pawnStatus = 'EXTENDED' AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) AND p.updatedAt BETWEEN :fromDate AND :toDate")
    Long sumInterestAmountByExtendedAndUpdatedAtBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT IFNULL(SUM(p.interestAmount), 0), COUNT(DISTINCT p.pawnId), " +
            "YEAR(p.updatedAt), MONTH(p.updatedAt), DAY(p.updatedAt) " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'EXTENDED' " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "AND p.updatedAt BETWEEN :fromDate AND :toDate " +
            "GROUP BY YEAR(p.updatedAt), MONTH(p.updatedAt), DAY(p.updatedAt) " +
            "ORDER BY YEAR(p.updatedAt), MONTH(p.updatedAt), DAY(p.updatedAt)")
    List<Object[]> getExtendedInterestByDay(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    @Query("SELECT IFNULL(SUM(p.interestAmount), 0), " +
            "COUNT(DISTINCT p.pawnId) AS totalCount, YEAR(p.updatedAt) AS year, MONTH(p.updatedAt) AS month " +
            "FROM PawnEntity p " +
            "WHERE p.pawnStatus = 'EXTENDED' " +
            "AND p.updatedAt BETWEEN :fromDate AND :toDate " +
            "AND (:excludeVisible = false OR p.visible IS NULL OR p.visible = true) " +
            "GROUP BY YEAR(p.updatedAt), MONTH(p.updatedAt)")
    List<Object[]> getExtendedInterestByMonth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("excludeVisible") boolean excludeVisible);

    @Query("SELECT COUNT(DISTINCT p.customerId) FROM PawnEntity p WHERE p.customerId IS NOT NULL AND p.pawnDate BETWEEN :fromDate AND :toDate AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true)")
    long countDistinctPawnCustomers(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COUNT(DISTINCT p.customerId) FROM PawnEntity p WHERE p.customerId IS NOT NULL AND p.pawnDate BETWEEN :fromDate AND :toDate AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true) AND NOT EXISTS (SELECT 1 FROM PawnEntity p2 WHERE p2.customerId = p.customerId AND p2.pawnDate < :fromDate)")
    long countNewPawnCustomers(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);

    @Query("SELECT COUNT(p) FROM PawnEntity p WHERE p.customerId IS NULL AND p.pawnDate BETWEEN :fromDate AND :toDate AND (:excludeVisibleItem = false OR p.visible IS NULL OR p.visible = true)")
    long countWalkInPawns(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("excludeVisibleItem") boolean excludeVisibleItem);
}
