package com.tappy.pos.service.pawn;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.PawnStatusNotAllowException;
import com.tappy.pos.service.tenant.ShopInfoService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.pawn.*;
import com.tappy.pos.model.entity.pawn.*;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.PawnStatus;
import com.tappy.pos.model.mapper.PawnMapper;
import com.tappy.pos.model.spec.PawnSpecification;
import com.tappy.pos.model.spec.PawnSpecificationBuilder;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.pawn.*;
import com.tappy.pos.util.DateTimeUtil;
import com.tappy.pos.util.FastExcelHelper;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.tappy.pos.model.enums.PawnInterestCalculation;
import static com.tappy.pos.model.spec.PawnSpecification.excludeOldRedeemedItems;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PawnServiceImpl implements PawnService {
    private final PawnRepository pawnRepository;
    private final PawnQueryRepository queryRepository;
    private final PawnMapper pawnMapper;
    private final CustomerRepository customerRepository;
    private final ReqMoneyRepository reqMoneyRepository;
    private final AuthContext authContext;
    private final ActivityLogService activityLogService;
    private final PawnAuditRepository auditRepository;
    private final ShopInfoService shopInfoService;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final PawnElectronicsRepository electronicsRepository;
    private final PawnVehicleRepository vehicleRepository;
    private final PawnWatchRepository watchRepository;
    private final PawnRealEstateRepository realEstateRepository;
    private final PawnGeneralRepository generalRepository;

    @Value("${schedule.cleanup.pawn.cutoffMonths:3}")
    private int cutoffMonths;

    @Override
    public PawnResponse createPawn(PawnRequest pawnRequest) {
        log.info("Creating pawn for customer {}", pawnRequest.getCustomerId());
        if (pawnRequest.isVisitingGuest() && (pawnRequest.getCustomerId() == null || pawnRequest.getCustomerId() == 0)) {
            String guestName = StringUtils.isNotEmpty(pawnRequest.getCustomerName()) ? pawnRequest.getCustomerName() : "Khách vãng lai";
            Long customerId = customerRepository.findByPhone("0000000000")
                    .map(c -> c.getId())
                    .orElseGet(() -> {
                        com.tappy.pos.model.entity.customer.Customer guest = com.tappy.pos.model.entity.customer.Customer.builder()
                                .name(guestName)
                                .phone("0000000000")
                                .build();
                        guest.setTenantId(tenantContext.getCurrentTenantId());
                        return customerRepository.save(guest).getId();
                    });
            pawnRequest.setCustomerId(customerId);
            // Store the per-pawn guest name so it isn't lost when the shared
            // walk-in customer record already exists with a different name.
            pawnRequest.setCustomerName(guestName);
        }
        PawnEntity savingPawnEntity = pawnMapper.fromPawnRequest(pawnRequest);
        savingPawnEntity.setTenantId(tenantContext.getCurrentTenantId());
        savingPawnEntity.setCustomerName(pawnRequest.getCustomerName());
        savingPawnEntity.setPawnDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDate()));
        savingPawnEntity.setPawnDueDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDueDate()));
        savingPawnEntity.setCreatedAt(LocalDateTime.now());
        savingPawnEntity.setUpdatedAt(LocalDateTime.now());
        savingPawnEntity.setCreatedBy(authContext.getCurrentUsername());
        savingPawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        savingPawnEntity.setPawnStatus(PawnStatus.PAWNED);
        savingPawnEntity.setVisible(true);
        PawnEntity savedEntity = pawnRepository.save(savingPawnEntity);
        saveItemDetail(savedEntity.getPawnId(), tenantContext.getCurrentTenantId(), pawnRequest);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_CREATED, "PAWN", String.valueOf(savedEntity.getPawnId()),
                messageService.getMessage("activity.pawn.created", savedEntity.getItemName()), null);
        return getPawnDetails(savedEntity.getPawnId());
    }

    @Override
    public PawnSearchResponse getPawns(Pageable pageable, SearchPawnRequest searchRequest) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(cutoffMonths);
        var specifications = new PawnSpecificationBuilder().buildPawnSpecifications(searchRequest).and(excludeOldRedeemedItems(threeMonthsAgo));
        if (shopInfoService.getExcludeVisibleItemFlag()) {
            specifications = specifications.and(PawnSpecification.includeVisibleStatus());
        }
        log.info("Applying Specification: {}", specifications);
        Page<PawnResponse> responsePage = queryRepository.findAll(specifications, pageable).map(pawnMapper::fromPawnQuery);
        batchEnrichWithItemDetail(responsePage.getContent());
        PawnSummary summary = queryRepository.getSummary(specifications);
        return PawnSearchResponse.from(responsePage, summary);
    }

    @Override
    public PawnResponse updatePawn(Long pawnId, PawnRequest pawnRequest) {
        log.info("Updating pawn for pawnId {}, {}", pawnId, pawnRequest.getCustomerId());
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        appendUpdateInfo(pawnEntity, pawnRequest);
        if (CollectionUtils.isNotEmpty(pawnRequest.getDeletedRequestIds())) {
            log.info("Deleting the Request Item {}", pawnRequest.getDeletedRequestIds());
            reqMoneyRepository.deleteAllById(pawnRequest.getDeletedRequestIds());
        }
        PawnEntity savedEntity = pawnRepository.save(pawnEntity);
        saveItemDetail(savedEntity.getPawnId(), tenantContext.getCurrentTenantId(), pawnRequest);
        ActivityAction activityAction = ActivityAction.PAWN_UPDATED;
        if (StringUtils.isNotEmpty(pawnRequest.getRequestType()) && pawnRequest.getRequestType().equalsIgnoreCase("REDEEMED")) {
            activityAction = ActivityAction.PAWN_REDEEMED;
        }
        String updateDesc = activityAction == ActivityAction.PAWN_REDEEMED
                ? messageService.getMessage("activity.pawn.redeemed", savedEntity.getItemName())
                : messageService.getMessage("activity.pawn.updated", savedEntity.getItemName());
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                activityAction, "PAWN", String.valueOf(savedEntity.getPawnId()),
                updateDesc, null);
        return getPawnDetails(savedEntity.getPawnId());
    }

    private void appendUpdateInfo(PawnEntity pawnEntity, PawnRequest pawnRequest) {
        log.info("Updating pawn entity pawnId {}", pawnEntity.getPawnId());
        pawnEntity.setInterestRate(pawnRequest.getInterestRate());
        pawnEntity.setItemType(pawnRequest.getItemType());
        pawnEntity.setItemDescription(pawnRequest.getItemDescription());
        pawnEntity.setItemValue(pawnRequest.getItemValue());
        pawnEntity.setItemName(pawnRequest.getItemName());
        pawnEntity.setItemWeight(pawnRequest.getItemWeight());
        pawnEntity.setItemBrand(pawnRequest.getItemBrand());
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        pawnEntity.setPawnAmount(pawnRequest.getPawnAmount());
        pawnEntity.setPawnDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDate()));
        pawnEntity.setPawnDueDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDueDate()));
        pawnEntity.setTotalAmount(pawnRequest.getTotalAmount());
        pawnEntity.setRedeemDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getRedeemDate()));
        pawnEntity.setInterestAmount(pawnRequest.getInterestAmount());
        pawnEntity.setPawnStatus(pawnRequest.getPawnStatus());
        pawnEntity.setGemWeight(pawnRequest.getGemWeight());
        pawnEntity.setInterestCalcMode(pawnRequest.getInterestCalcMode());
        pawnEntity.setHeldDays((int) pawnRequest.getHeldDays());
        pawnEntity.setPawnCategory(pawnRequest.getPawnCategory());
    }

    @Override
    public PawnResponse getPawnDetails(Long pawnId) {
        log.info("Get pawn details for pawnId {}", pawnId);
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        PawnResponse pawnResponse = pawnMapper.fromPawnEntity(pawnEntity);
        if (CollectionUtils.isNotEmpty(pawnEntity.getReqMoneys())) {
            pawnResponse.setReqMoneys(pawnEntity.getReqMoneys().stream().map(pawnMapper::fromReqMoneyEntity).collect(Collectors.toSet()));
        }
        customerRepository.findById(pawnEntity.getCustomerId()).ifPresent(customer -> {
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .id(customer.getId())
                    .name(customer.getName())
                    .phone(customer.getPhone())
                    .build();
            pawnResponse.setCustomer(customerDTO);
            // Prefer the name stored on the pawn itself (set at creation for visiting guests).
            String resolvedName = StringUtils.isNotEmpty(pawnEntity.getCustomerName())
                    ? pawnEntity.getCustomerName() : customer.getName();
            pawnResponse.setCustomerName(resolvedName);
            pawnResponse.setPhone(customer.getPhone());
        });
        enrichWithItemDetail(pawnResponse, pawnId, pawnEntity.getPawnCategory());
        List<PawnAuditEntity> auditEntities = auditRepository.findByPawnIdOrderByActionIdAsc(pawnId);
        List<PawnAudit> pawnAudits = auditEntities.stream().map(pawnMapper::auditFromPawnAuditEntity).toList();
        pawnResponse.setAudits(pawnAudits);
        return pawnResponse;
    }

    @Override
    public void deletePawnByPawnIds(List<Long> pawnIds) {
        log.info("Process deleting pawns by given PawnIds {}", pawnIds);
        List<PawnEntity> pawnEntities = pawnRepository.findAllById(pawnIds);
        // Clean up category-specific child tables first — deleteAllByIdInBatch
        // issues a direct SQL DELETE and bypasses JPA cascade, so orphans must
        // be removed explicitly.
        electronicsRepository.deleteByPawnIdIn(pawnIds);
        vehicleRepository.deleteByPawnIdIn(pawnIds);
        watchRepository.deleteByPawnIdIn(pawnIds);
        realEstateRepository.deleteByPawnIdIn(pawnIds);
        generalRepository.deleteByPawnIdIn(pawnIds);
        pawnRepository.deleteAllByIdInBatch(pawnIds);
        if (CollectionUtils.isNotEmpty(pawnEntities)) {
            pawnEntities.forEach(pawnEntity -> activityLogService.logAsync(
                    tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                    ActivityAction.PAWN_DELETED, "PAWN", String.valueOf(pawnEntity.getPawnId()),
                    messageService.getMessage("activity.pawn.deleted", pawnEntity.getItemName()), null));
        }
    }

    @Override
    public PawnResponse cancelPawnByPawnId(@NotBlank Long pawnId, @NotBlank String cancelReason) {
        log.info("Process cancelling pawn by given PawnId {}, with reason {}", pawnId, cancelReason);
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        validateCancelStatus(pawnEntity);
        pawnEntity.setPawnStatus(PawnStatus.CANCELLED);
        pawnEntity.setCanceledReason(cancelReason);
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        PawnEntity savedEntity = pawnRepository.save(pawnEntity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_CANCEL, "PAWN", String.valueOf(savedEntity.getPawnId()),
                messageService.getMessage("activity.pawn.cancelled", savedEntity.getItemName()), null);
        return getPawnDetails(savedEntity.getPawnId());
    }

    private void validateCancelStatus(PawnEntity pawnEntity) {
        invalidPawnStatus(PawnStatus.FORFEITED, pawnEntity);
        invalidPawnStatus(PawnStatus.REDEEMED, pawnEntity);
    }

    @Override
    public PawnResponse forfeitPawnByPawnId(@NotBlank Long pawnId, ForfeitRequest forfeitRequest) {
        log.info("Process forfeiting pawn by given PawnId {}", pawnId);
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        validateForfeitStatus(pawnEntity);
        if (forfeitRequest.getInterestAmount() == null || forfeitRequest.getInterestAmount().compareTo(BigDecimal.ZERO) < 0
                || forfeitRequest.getTotalAmount() == null || forfeitRequest.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(messageService.getMessage("error.pawn.invalidForfeitAmount"));
        }
        pawnEntity.setPawnStatus(PawnStatus.FORFEITED);
        pawnEntity.setForfeitedReason(forfeitRequest.getForfeitedReason());
        pawnEntity.setForfeitedDate(DateTimeUtil.fromLocalDateTime(forfeitRequest.getForfeitedDate()));
        pawnEntity.setForfeitedAmount(forfeitRequest.getForfeitedAmount());
        pawnEntity.setInterestAmount(forfeitRequest.getInterestAmount());
        pawnEntity.setTotalAmount(forfeitRequest.getTotalAmount());
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        PawnEntity savedEntity = pawnRepository.save(pawnEntity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_FORFEIT, "PAWN", String.valueOf(savedEntity.getPawnId()),
                messageService.getMessage("activity.pawn.forfeited", savedEntity.getItemName()), null);
        return getPawnDetails(savedEntity.getPawnId());
    }

    private void validateForfeitStatus(PawnEntity pawnEntity) {
        invalidPawnStatus(PawnStatus.CANCELLED, pawnEntity);
        invalidPawnStatus(PawnStatus.FORFEITED, pawnEntity);
        invalidPawnStatus(PawnStatus.REDEEMED, pawnEntity);
    }

    @Override
    public PawnResponse calculatePawnRedeem(Long pawnId, RedeemRequest redeemRequest) {
        boolean isExtending = (redeemRequest != null && redeemRequest.isExtendingRequest());
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        validateRedeemStatus(pawnEntity);
        LocalDateTime redeemDate = LocalDateTime.now();
        PawnResponse pawnResponse = pawnMapper.fromPawnEntity(pawnEntity);
        customerRepository.findById(pawnEntity.getCustomerId()).ifPresent(customer -> {
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .id(customer.getId())
                    .name(customer.getName())
                    .phone(customer.getPhone())
                    .build();
            pawnResponse.setCustomer(customerDTO);
            String resolvedName = StringUtils.isNotEmpty(pawnEntity.getCustomerName())
                    ? pawnEntity.getCustomerName() : customer.getName();
            pawnResponse.setCustomerName(resolvedName);
            pawnResponse.setPhone(customer.getPhone());
        });
        if (redeemRequest != null && redeemRequest.getRedeemDate() != null) {
            redeemDate = DateTimeUtil.fromLocalDateTime(redeemRequest.getRedeemDate());
        }
        long pawnedDays = calculateHeldDays(redeemDate, pawnResponse.getPawnDate(), isExtending);
        String calcModeCode = (redeemRequest != null && StringUtils.isNotEmpty(redeemRequest.getInterestCalcMode()))
                ? redeemRequest.getInterestCalcMode()
                : pawnEntity.getInterestCalcMode();
        PawnInterestCalculation calcMode = PawnInterestCalculation.fromCode(calcModeCode);
        BigDecimal mainInterestAmount = calculateInterestAmount(pawnedDays, calcMode, pawnResponse.getInterestRate(), pawnResponse.getPawnAmount());
        pawnResponse.setMainInterestAmount(mainInterestAmount);
        BigDecimal interestAmount = pawnResponse.getMainInterestAmount();
        BigDecimal requestedAmount = pawnResponse.getPawnAmount();
        Set<ReqMoneyResponse> reqMoneys = new HashSet<>();
        for (ReqMoneyEntity reqMoneyEntity : pawnEntity.getReqMoneys()) {
            long heldDays = calculateHeldDays(redeemDate, reqMoneyEntity.getRequestDate(), isExtending);
            BigDecimal subInterestAmount = calculateInterestAmount(heldDays, calcMode, pawnResponse.getInterestRate(), reqMoneyEntity.getRequestAmount());
            BigDecimal subRequestedAmount = reqMoneyEntity.getRequestAmount();
            requestedAmount = subRequestedAmount.add(requestedAmount);
            interestAmount = subInterestAmount.add(interestAmount);
            ReqMoneyResponse reqMoney = pawnMapper.fromReqMoneyEntity(reqMoneyEntity);
            reqMoney.setHeldDays(heldDays);
            reqMoney.setInterestAmount(subInterestAmount);
            reqMoneys.add(reqMoney);
        }
        pawnResponse.setReqMoneys(reqMoneys);
        pawnResponse.setRedeemDate(redeemDate);
        pawnResponse.setInterestAmount(interestAmount.setScale(0, RoundingMode.UP));
        pawnResponse.setHeldDays(pawnedDays);
        pawnResponse.setTotalAmount(requestedAmount.add(interestAmount).setScale(0, RoundingMode.UP));
        List<PawnAuditEntity> auditEntities = auditRepository.findByPawnIdOrderByActionIdAsc(pawnId);
        List<PawnAudit> pawnAudits = auditEntities.stream().map(pawnMapper::auditFromPawnAuditEntity).toList();
        pawnResponse.setAudits(pawnAudits);
        return pawnResponse;
    }

    private void validateRedeemStatus(PawnEntity pawnEntity) {
        invalidPawnStatus(PawnStatus.CANCELLED, pawnEntity);
        invalidPawnStatus(PawnStatus.REDEEMED, pawnEntity);
    }

    private void invalidPawnStatus(PawnStatus invalidStatus, PawnEntity pawnEntity) {
        if (pawnEntity.getPawnStatus().equals(invalidStatus)) {
            log.warn("Action is not allowed since current status is: " + invalidStatus);
            throw new PawnStatusNotAllowException(messageService.getMessage("error.pawn.statusNotAllowed", new Object[]{invalidStatus}));
        }
    }

    /**
     * Calculates the interest owed on a pawn loan.
     *
     * Inputs
     * ------
     * - pawnAmount    : principal loaned to the customer (VND)
     * - interestRate  : monthly rate entered by staff, e.g. 3 = 3 % per month
     * - pawnedDays    : held days computed by calculateHeldDays()
     * - calcMode      : one of the four PawnInterestCalculation modes
     *
     * Shared pre-step (all modes)
     * ---------------------------
     *   monthlyRate = interestRate / 100
     *   (e.g. 3 % → 0.03)
     *
     * Mode: DAILY_30  — charge every actual held day; month normalised to 30 days
     * -------------------------------------------------------------------------
     *   dailyRate = pawnAmount × monthlyRate / 30
     *   interest  = dailyRate × pawnedDays
     *
     *   Example: 10,000,000 ₫ · 3 % · 15 days
     *     dailyRate = 10,000,000 × 0.03 / 30 = 10,000 ₫/day
     *     interest  = 10,000 × 15 = 150,000 ₫
     *
     * Mode: DAILY_25  — same daily rate but cap chargeable days at 25 per 30-day period
     * ---------------------------------------------------------------------------------
     *   dailyRate      = pawnAmount × monthlyRate / 30   (same formula as DAILY_30)
     *   fullPeriods    = pawnedDays / 30                  (integer division)
     *   remainingDays  = pawnedDays % 30
     *   interest = dailyRate × 25 × fullPeriods
     *            + dailyRate × min(remainingDays, 25)
     *
     *   Example: 10,000,000 ₫ · 3 % · 45 days
     *     dailyRate     = 10,000 ₫/day
     *     fullPeriods   = 1,  remainingDays = 15
     *     interest = 10,000 × 25 × 1 + 10,000 × 15 = 250,000 + 150,000 = 400,000 ₫
     *     (vs DAILY_30: 10,000 × 45 = 450,000 ₫ — DAILY_25 saves the customer 50,000 ₫)
     *
     * Mode: MONTHLY  — round partial months UP to the next full month
     * ---------------------------------------------------------------
     *   months   = ceil(pawnedDays / 30)   implemented as (pawnedDays + 29) / 30
     *   interest = pawnAmount × monthlyRate × months
     *
     *   Example: 10,000,000 ₫ · 3 % · 31 days  → 2 months
     *     interest = 10,000,000 × 0.03 × 2 = 600,000 ₫
     *
     * Mode: BIWEEKLY  — round partial 15-day periods UP to the next half-month
     * -------------------------------------------------------------------------
     *   halfMonths = ceil(pawnedDays / 15)   implemented as (pawnedDays + 14) / 15
     *   interest   = pawnAmount × monthlyRate × halfMonths / 2
     *
     *   Example: 10,000,000 ₫ · 3 % · 16 days  → 2 half-months (= 1 full month)
     *     interest = 10,000,000 × 0.03 × 2 / 2 = 300,000 ₫
     *
     * Multi-disbursement pawns (reqMoney)
     * ------------------------------------
     * When a customer borrows additional money mid-contract (lấy thêm tiền), each
     * disbursement is tracked as a separate ReqMoneyEntity with its own requestDate.
     * Interest is calculated independently for each disbursement using the same
     * calcMode and interestRate but with its own heldDays (redeemDate – requestDate).
     * The total interest returned is the sum of all per-disbursement amounts.
     *
     * Rounding
     * ---------
     * Intermediate divisions use HALF_UP with 5 decimal places.
     * The final total is rounded UP to the nearest whole VND (RoundingMode.UP).
     */
    private BigDecimal calculateInterestAmount(long pawnedDays, PawnInterestCalculation calcMode, BigDecimal interestRate, BigDecimal pawnAmount) {
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP);
        return switch (calcMode) {
            case DAILY_30 -> {
                BigDecimal ratePerDay = pawnAmount.multiply(monthlyRate).divide(BigDecimal.valueOf(30), 5, RoundingMode.HALF_UP);
                yield ratePerDay.multiply(BigDecimal.valueOf(pawnedDays));
            }
            case DAILY_25 -> {
                BigDecimal ratePerDay = pawnAmount.multiply(monthlyRate).divide(BigDecimal.valueOf(30), 5, RoundingMode.HALF_UP);
                long fullPeriods = pawnedDays / 30;
                long remainingDays = pawnedDays % 30;
                BigDecimal fullPeriodsInterest = ratePerDay.multiply(BigDecimal.valueOf(25)).multiply(BigDecimal.valueOf(fullPeriods));
                BigDecimal remainingInterest = ratePerDay.multiply(BigDecimal.valueOf(Math.min(remainingDays, 25)));
                yield fullPeriodsInterest.add(remainingInterest);
            }
            case MONTHLY -> {
                long months = (pawnedDays + 29) / 30;
                yield pawnAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(months));
            }
            case BIWEEKLY -> {
                long halfMonths = (pawnedDays + 14) / 15;
                yield pawnAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(halfMonths)).divide(BigDecimal.valueOf(2), 5, RoundingMode.HALF_UP);
            }
        };
    }

    /**
     * Counts the number of chargeable held days between pawnDate and redeemDate.
     *
     * Counting rule
     * -------------
     * Same-day redemption (dateBetween == 0): charge 1 day minimum.
     *
     * Redeeming (isExtending = false):
     *   Both the pawn day AND the redeem day are counted, so we add 1.
     *   Example: pawned on 01/05, redeemed on 03/05 → 3 days (1, 2, 3).
     *   dateBetween = 2 → return 2 + 1 = 3.
     *
     * Extending / closing interest (isExtending = true):
     *   Only the days already elapsed are counted; the new contract starts fresh
     *   from the extend date, so we do NOT add the extra day.
     *   Example: pawned 01/05, closing interest on 03/05 → 2 days elapsed.
     *   dateBetween = 2 → return 2.
     *
     * Negative dateBetween (redeemDate before pawnDate): returns 0 — caller's guard.
     */
    private long calculateHeldDays(LocalDateTime redeemDate, LocalDateTime pawnDate, boolean isExtending) {
        long dateBetween = ChronoUnit.DAYS.between(pawnDate.toLocalDate(), redeemDate.toLocalDate());
        if (dateBetween == 0) return 1;
        else if (isExtending && dateBetween >= 1) return dateBetween;
        else if (dateBetween >= 1) return dateBetween + 1;
        else return 0;
    }

    @Override
    public ReqMoneyResponse requestMoreMoney(Long pawnId, ReqMoneyRequest reqMoneyRequest) {
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        validateReqMoneyStatus(pawnEntity);
        ReqMoneyEntity reqMoneyEntity = ReqMoneyEntity.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .requestAmount(reqMoneyRequest.getRequestAmount())
                .pawnId(pawnId)
                .requestDate(reqMoneyRequest.getRequestDate() != null ? reqMoneyRequest.getRequestDate().atTime(LocalTime.now()) : null)
                .createdBy(authContext.getCurrentUsername())
                .build();
        reqMoneyRepository.save(reqMoneyEntity);
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnRepository.save(pawnEntity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_REQUEST_MONEY, "PAWN", String.valueOf(pawnEntity.getPawnId()),
                messageService.getMessage("activity.pawn.request_money", pawnEntity.getItemName()), null);
        return pawnMapper.fromReqMoneyEntity(reqMoneyEntity);
    }

    private void validateReqMoneyStatus(PawnEntity pawnEntity) {
        invalidPawnStatus(PawnStatus.CANCELLED, pawnEntity);
        invalidPawnStatus(PawnStatus.REDEEMED, pawnEntity);
        invalidPawnStatus(PawnStatus.FORFEITED, pawnEntity);
    }

    @Override
    public PawnResponse extendPawn(Long pawnId, PawnRequest pawnRequest) {
        log.info("Extending Pawn for id: {}", pawnId);
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        PawnEntity extendingPawn = createExtendPawn(pawnEntity, pawnRequest);
        extendingPawn.setTenantId(tenantContext.getCurrentTenantId());
        extendingPawn.setCreatedAt(LocalDateTime.now());
        extendingPawn.setUpdatedAt(LocalDateTime.now());
        extendingPawn.setCreatedBy(authContext.getCurrentUsername());
        extendingPawn.setUpdatedBy(authContext.getCurrentUsername());
        extendingPawn.setPawnStatus(PawnStatus.PAWNED);
        appendUpdateInfo(pawnEntity, pawnRequest);
        pawnEntity.setPawnStatus(PawnStatus.REDEEMED);
        PawnEntity redeemedPawn = pawnRepository.save(pawnEntity);
        PawnEntity savedExtendEntity = pawnRepository.save(extendingPawn);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_EXTEND, "PAWN", String.valueOf(redeemedPawn.getPawnId()),
                messageService.getMessage("activity.pawn.extended", redeemedPawn.getItemName()), null);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_CREATED, "PAWN", String.valueOf(savedExtendEntity.getPawnId()),
                messageService.getMessage("activity.pawn.extended.new", savedExtendEntity.getItemName()), null);
        return getPawnDetails(savedExtendEntity.getPawnId());
    }

    private PawnEntity createExtendPawn(PawnEntity pawnEntity, PawnRequest extendRequest) {
        log.info("Create extending pawn from existing pawnId {}", pawnEntity.getPawnId());
        BigDecimal requestedAmount = pawnEntity.getPawnAmount();
        for (ReqMoneyEntity reqMoneyEntity : pawnEntity.getReqMoneys()) {
            requestedAmount = reqMoneyEntity.getRequestAmount().add(requestedAmount);
        }
        PawnEntity extendingPawn = PawnEntity.builder()
                .customerId(pawnEntity.getCustomerId())
                .interestRate(pawnEntity.getInterestRate())
                .itemType(pawnEntity.getItemType())
                .itemDescription(pawnEntity.getItemDescription())
                .itemValue(pawnEntity.getItemValue())
                .itemBrand(pawnEntity.getItemBrand())
                .itemName(pawnEntity.getItemName())
                .gemWeight(pawnEntity.getGemWeight())
                .itemWeight(pawnEntity.getItemWeight())
                .interestCalcMode(PawnInterestCalculation.fromCode(pawnEntity.getInterestCalcMode()).name())
                .originalId(pawnEntity.getPawnId())
                .pawnStatus(PawnStatus.PAWNED)
                .createdBy(authContext.getCurrentUsername())
                .pawnAmount(requestedAmount)
                .pawnDate(DateTimeUtil.fromLocalDateTime(extendRequest.getExtendDate()))
                .pawnDueDate(DateTimeUtil.fromLocalDateTime(extendRequest.getExtendDueDate()))
                .pawnCategory(pawnEntity.getPawnCategory())
                .build();
        extendingPawn.setOriginalId(pawnEntity.getOriginalId() != null ? pawnEntity.getOriginalId() : pawnEntity.getPawnId());
        return extendingPawn;
    }

    @Override
    public PawnKPIs getPawnKPIs(DateFilterRequest dateFilter) {
        log.info("Get Pawn KPIs data {}", dateFilter);
        LocalDateTime fromDate = Instant.ofEpochMilli(dateFilter.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toDate = Instant.ofEpochMilli(dateFilter.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        boolean visibleFlag = shopInfoService.getExcludeVisibleItemFlag();
        PawnKPIs upComing = getUpComingDueAmount(fromDate, toDate, visibleFlag);
        PawnKPIs overDue = getOverdueAmount(LocalDate.now().atTime(LocalTime.MAX), visibleFlag);
        PawnKPIs newPawn = getNewPawnsAmount(fromDate, toDate, visibleFlag);
        PawnKPIs newMoneyRequest = getNewRequestMoneyAmount(fromDate, toDate, visibleFlag);
        PawnKPIs redeemPawn = getRedeemedPawnAmount(fromDate, toDate, visibleFlag);
        PawnKPIs forfeited = getForfeitedPawnAmount(fromDate, toDate, visibleFlag);
        PawnKPIs activePawn = getTotalActivePawns(visibleFlag);
        return PawnKPIs.builder()
                .totalPawnedCount(activePawn.getTotalPawnedCount())
                .totalPawnedAmount(activePawn.getTotalPawnedAmount())
                .dueTodayCount(upComing.getDueTodayCount())
                .dueTodayAmount(upComing.getDueTodayAmount())
                .overdueCount(overDue.getOverdueCount())
                .overdueAmount(overDue.getOverdueAmount())
                .newPawnsCount(newPawn.getNewPawnsCount())
                .newPawnsAmount(newPawn.getNewPawnsAmount())
                .newRequestMoneyCount(newMoneyRequest.getNewRequestMoneyCount())
                .newRequestMoneyAmount(newMoneyRequest.getNewRequestMoneyAmount())
                .completedPawnAmount(redeemPawn.getCompletedPawnAmount())
                .completedPawnCount(redeemPawn.getCompletedPawnCount())
                .forfeitedPawnAmount(forfeited.getForfeitedPawnAmount())
                .forfeitedPawnCount(forfeited.getForfeitedPawnCount())
                .interestPawnAmount(getInterestPawnAmount(fromDate, toDate, visibleFlag))
                .build();
    }

    private PawnKPIs getTotalActivePawns(boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.getPawnAmountByPawnStatus(PawnStatus.PAWNED, visibleFlag);
        Long requestAmount = pawnRepository.getPawnRequestAmountByPawnStatus(PawnStatus.PAWNED, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setTotalPawnedAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setTotalPawnedCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private PawnKPIs getUpComingDueAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusAndPawnDueDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        Long requestAmount = pawnRepository.sumRequestAmountByPawnStatusAndPawnDueDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setDueTodayAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setDueTodayCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private PawnKPIs getOverdueAmount(LocalDateTime queryDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusAndPawnDueDateBefore(PawnStatus.PAWNED, queryDate, visibleFlag);
        Long requestAmount = pawnRepository.sumRequestAmountByPawnStatusAndPawnDueDateBefore(PawnStatus.PAWNED, queryDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setOverdueAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setOverdueCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private PawnKPIs getNewPawnsAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusAndPawnDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        Long requestAmount = pawnRepository.sumRequestAmountByPawnStatusAndPawnDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setNewPawnsAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setNewPawnsCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private PawnKPIs getNewRequestMoneyAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumRequestMoneyByPawnStatusAndRequestDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setNewRequestMoneyAmount(totalAmount.longValue());
            pawnKPIs.setNewRequestMoneyCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private PawnKPIs getRedeemedPawnAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List.of(PawnStatus.REDEEMED), fromDate, toDate, visibleFlag);
        Long requestAmount = pawnRepository.sumRequestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List.of(PawnStatus.REDEEMED), fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setCompletedPawnAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setCompletedPawnCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private PawnKPIs getForfeitedPawnAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List.of(PawnStatus.FORFEITED), fromDate, toDate, visibleFlag);
        Long requestAmount = pawnRepository.sumRequestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List.of(PawnStatus.FORFEITED), fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setForfeitedPawnAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setForfeitedPawnCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private long getInterestPawnAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        Long amount = pawnRepository.sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(
                Arrays.asList(PawnStatus.REDEEMED, PawnStatus.FORFEITED), fromDate, toDate, visibleFlag);
        return amount == null ? 0 : amount;
    }

    @Override
    public FileSystemResource exportPawns(SearchPawnRequest searchRequest) throws IOException {
        log.info("Export PAWNs data file {}", searchRequest);
        String excelFilePath = FastExcelHelper.getExportFileFullDir(UUID.randomUUID() + ".xlsx");
        var specifications = new PawnSpecificationBuilder().buildPawnSpecifications(searchRequest);
        if (shopInfoService.getExcludeVisibleItemFlag()) {
            specifications = specifications.and(PawnSpecification.includeVisibleStatus());
        }
        List<PawnQuery> pawnQueries = queryRepository.findAll(specifications);
        FastExcelHelper.exportPawnDataToExcel(excelFilePath, pawnQueries);
        return FastExcelHelper.downloadFile(excelFilePath);
    }

    @Override
    public PawnBarsResponse getPawnCharts(DateFilterRequest dateFilter) {
        if (dateFilter.getFromDate() == 0 || dateFilter.getToDate() == 0) {
            throw new IllegalArgumentException(messageService.getMessage("error.pawn.dateFilterRequired"));
        }
        boolean visibleItemFlag = shopInfoService.getExcludeVisibleItemFlag();
        log.info("getPawnBars({})", dateFilter);
        LocalDateTime fromDate = Instant.ofEpochMilli(dateFilter.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toDate = Instant.ofEpochMilli(dateFilter.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        List<Object[]> pawnedObjs = pawnRepository.getAmountAndTotalCountByMonthPawnDateAndStatus(fromDate, toDate, visibleItemFlag);
        List<Object[]> redeemObjs = pawnRepository.getAmountAndTotalCountByMonthRedeemDateAndStatus(Collections.singletonList(PawnStatus.REDEEMED), fromDate, toDate, visibleItemFlag);
        List<Object[]> forfeitedObjs = pawnRepository.getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(Collections.singletonList(PawnStatus.FORFEITED), fromDate, toDate, visibleItemFlag);
        List<Object[]> redeemedInterestObjs = pawnRepository.getRedeemedInterestAmount(PawnStatus.REDEEMED, fromDate, toDate, visibleItemFlag);
        List<Object[]> forfeitedInterest = pawnRepository.getForfeitedInterestAmount(PawnStatus.FORFEITED, fromDate, toDate, visibleItemFlag);
        List<BarsData> redeemedBars = mergeInterestBarsData(convertBarsData(redeemedInterestObjs), convertBarsData(forfeitedInterest));

        return PawnBarsResponse.builder()
                .pawnedBars(convertBarsData(pawnedObjs))
                .redeemBars(convertBarsData(redeemObjs))
                .forfeitedBars(convertBarsData(forfeitedObjs))
                .interestBars(redeemedBars)
                .build();
    }

    public static List<BarsData> mergeInterestBarsData(List<BarsData> redeemedBars, List<BarsData> forfeitedBars) {
        Map<String, BarsData> mergedMap = new HashMap<>();
        for (BarsData data : redeemedBars) {
            String key = data.getYear() + "-" + data.getMonth();
            mergedMap.putIfAbsent(key, BarsData.builder().year(data.getYear()).month(data.getMonth()).amount(0).count(0).weight(0.0).build());
            BarsData existing = mergedMap.get(key);
            existing.setAmount(existing.getAmount() + data.getAmount());
            existing.setCount(existing.getCount() + data.getCount());
            existing.setWeight(existing.getWeight() + data.getWeight());
        }
        for (BarsData data : forfeitedBars) {
            String key = data.getYear() + "-" + data.getMonth();
            mergedMap.putIfAbsent(key, BarsData.builder().year(data.getYear()).month(data.getMonth()).amount(0).count(0).weight(0.0).build());
            BarsData existing = mergedMap.get(key);
            existing.setAmount(existing.getAmount() + data.getAmount());
            existing.setCount(existing.getCount() + data.getCount());
            existing.setWeight(existing.getWeight() + data.getWeight());
        }
        return new ArrayList<>(mergedMap.values());
    }

    private List<BarsData> convertBarsData(List<Object[]> resultList) {
        List<BarsData> bars = new ArrayList<>();
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result[1] != null ? (Long) result[1] : 0;
            int year = result[2] != null ? (Integer) result[2] : 0;
            int month = result[3] != null ? (Integer) result[3] : 0;
            bars.add(BarsData.builder().amount(totalAmount.longValue()).count((int) totalCount).year(year).month(month).build());
        }
        return bars;
    }

    @Override
    @Transactional
    public int updateVisibleStatus(Long pawnId, boolean visibleStatus) {
        log.info("Update visible status pawn {}, status {}", pawnId, visibleStatus);
        return pawnRepository.updateVisibleStatus(Collections.singletonList(pawnId), visibleStatus);
    }

    @Override
    public List<Long> getPawnIdsToClean(Pageable pageable, SearchPawnRequest searchRequest) {
        var specifications = new PawnSpecificationBuilder().buildPawnSpecForDeletion(searchRequest);
        Page<PawnQuery> pawnsPage = queryRepository.findAll(specifications, pageable);
        return pawnsPage.stream().map(PawnQuery::getPawnId).collect(Collectors.toList());
    }

    @Override
    public PawnSetting updatePawnSetting(PawnSetting setting) {
        log.info("Update pawn setting {}", setting);
        return shopInfoService.updatePawnSetting(setting);
    }

    @Override
    public PawnSetting getPawnSetting() {
        return shopInfoService.getPawnSetting();
    }

    private void batchEnrichWithItemDetail(List<PawnResponse> responses) {
        if (responses.isEmpty()) return;
        List<Long> electronicIds  = responses.stream().filter(r -> "ELECTRONICS".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> vehicleIds     = responses.stream().filter(r -> "MOTORBIKE".equals(r.getPawnCategory()) || "CAR".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> watchIds       = responses.stream().filter(r -> "WATCH".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> realEstateIds  = responses.stream().filter(r -> "REAL_ESTATE".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> generalIds     = responses.stream().filter(r -> "GENERAL".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();

        Map<Long, PawnElectronicsDetail> electronicsMap = electronicsRepository.findByPawnIdIn(electronicIds).stream().collect(Collectors.toMap(PawnElectronicsEntity::getPawnId, pawnMapper::fromElectronicsEntity));
        Map<Long, PawnVehicleDetail>     vehicleMap     = vehicleRepository.findByPawnIdIn(vehicleIds).stream().collect(Collectors.toMap(PawnVehicleEntity::getPawnId, pawnMapper::fromVehicleEntity));
        Map<Long, PawnWatchDetail>       watchMap       = watchRepository.findByPawnIdIn(watchIds).stream().collect(Collectors.toMap(PawnWatchEntity::getPawnId, pawnMapper::fromWatchEntity));
        Map<Long, PawnRealEstateDetail>  realEstateMap  = realEstateRepository.findByPawnIdIn(realEstateIds).stream().collect(Collectors.toMap(PawnRealEstateEntity::getPawnId, pawnMapper::fromRealEstateEntity));
        Map<Long, PawnGeneralDetail>     generalMap     = generalRepository.findByPawnIdIn(generalIds).stream().collect(Collectors.toMap(PawnGeneralEntity::getPawnId, pawnMapper::fromGeneralEntity));

        for (PawnResponse r : responses) {
            if (electronicsMap.containsKey(r.getPawnId()))  r.setElectronicsDetail(electronicsMap.get(r.getPawnId()));
            if (vehicleMap.containsKey(r.getPawnId()))      r.setVehicleDetail(vehicleMap.get(r.getPawnId()));
            if (watchMap.containsKey(r.getPawnId()))        r.setWatchDetail(watchMap.get(r.getPawnId()));
            if (realEstateMap.containsKey(r.getPawnId()))   r.setRealEstateDetail(realEstateMap.get(r.getPawnId()));
            if (generalMap.containsKey(r.getPawnId()))      r.setGeneralDetail(generalMap.get(r.getPawnId()));
        }
    }

    private void saveItemDetail(Long pawnId, String tenantId, PawnRequest req) {
        String category = req.getPawnCategory();
        if (StringUtils.isEmpty(category)) return;
        switch (category) {
            case "ELECTRONICS" -> {
                electronicsRepository.deleteByPawnId(pawnId);
                if (req.getElectronicsDetail() != null) {
                    electronicsRepository.save(pawnMapper.toElectronicsEntity(tenantId, pawnId, req.getElectronicsDetail()));
                }
            }
            case "MOTORBIKE", "CAR" -> {
                vehicleRepository.deleteByPawnId(pawnId);
                if (req.getVehicleDetail() != null) {
                    vehicleRepository.save(pawnMapper.toVehicleEntity(tenantId, pawnId, req.getVehicleDetail()));
                }
            }
            case "WATCH" -> {
                watchRepository.deleteByPawnId(pawnId);
                if (req.getWatchDetail() != null) {
                    watchRepository.save(pawnMapper.toWatchEntity(tenantId, pawnId, req.getWatchDetail()));
                }
            }
            case "REAL_ESTATE" -> {
                realEstateRepository.deleteByPawnId(pawnId);
                if (req.getRealEstateDetail() != null) {
                    realEstateRepository.save(pawnMapper.toRealEstateEntity(tenantId, pawnId, req.getRealEstateDetail()));
                }
            }
            case "GENERAL" -> {
                generalRepository.deleteByPawnId(pawnId);
                if (req.getGeneralDetail() != null) {
                    generalRepository.save(pawnMapper.toGeneralEntity(tenantId, pawnId, req.getGeneralDetail()));
                }
            }
        }
    }

    private void enrichWithItemDetail(PawnResponse response, Long pawnId, String category) {
        if (StringUtils.isEmpty(category)) return;
        switch (category) {
            case "ELECTRONICS" -> electronicsRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setElectronicsDetail(pawnMapper.fromElectronicsEntity(e)));
            case "MOTORBIKE", "CAR" -> vehicleRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setVehicleDetail(pawnMapper.fromVehicleEntity(e)));
            case "WATCH" -> watchRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setWatchDetail(pawnMapper.fromWatchEntity(e)));
            case "REAL_ESTATE" -> realEstateRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setRealEstateDetail(pawnMapper.fromRealEstateEntity(e)));
            case "GENERAL" -> generalRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setGeneralDetail(pawnMapper.fromGeneralEntity(e)));
        }
    }
}
