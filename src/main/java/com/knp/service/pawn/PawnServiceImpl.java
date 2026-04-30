package com.knp.service.pawn;

import com.knp.config.AuthContext;
import com.knp.exception.PawnStatusNotAllowException;
import com.knp.service.tenant.ShopInfoService;
import com.knp.service.audit.ActivityLogService;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.customer.CustomerDTO;
import com.knp.model.dto.pawn.*;
import com.knp.model.entity.pawn.PawnAuditEntity;
import com.knp.model.entity.pawn.PawnEntity;
import com.knp.model.entity.pawn.PawnQuery;
import com.knp.model.entity.pawn.ReqMoneyEntity;
import com.knp.model.enums.ActivityAction;
import com.knp.model.enums.PawnStatus;
import com.knp.model.mapper.PawnMapper;
import com.knp.model.spec.PawnSpecification;
import com.knp.model.spec.PawnSpecificationBuilder;
import com.knp.multitenant.TenantContext;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.pawn.PawnAuditRepository;
import com.knp.repository.pawn.PawnQueryRepository;
import com.knp.repository.pawn.PawnRepository;
import com.knp.repository.pawn.ReqMoneyRepository;
import com.knp.util.DateTimeUtil;
import com.knp.util.FastExcelHelper;
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

import static com.knp.model.spec.PawnSpecification.excludeOldRedeemedItems;
import static com.knp.model.enums.PawnInterestCalculation.INTEREST_BY_DAY_25DAY_PER_MONTH;
import static com.knp.model.enums.PawnInterestCalculation.INTEREST_BY_DAY_FULL_MONTH;

@Service
@RequiredArgsConstructor
@Slf4j
public class PawnServiceImpl implements PawnService {
    private static final int INTEREST_DAY_PER_MONTH = 30;

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

    @Value("${schedule.cleanup.pawn.cutoffMonths:3}")
    private int cutoffMonths;

    @Override
    public PawnResponse createPawn(PawnRequest pawnRequest) {
        log.info("Creating pawn for customer {}", pawnRequest.getCustomerId());
        if (pawnRequest.isVisitingGuest() && (pawnRequest.getCustomerId() == null || pawnRequest.getCustomerId() == 0)) {
            String guestName = StringUtils.isNotEmpty(pawnRequest.getCustomerName()) ? pawnRequest.getCustomerName() : "Khách vãng lai";
            String guestPhone = "0000000000";
            Long customerId = customerRepository.findByName(guestName)
                    .map(c -> c.getId())
                    .orElseGet(() -> {
                        com.knp.model.entity.customer.Customer guest = com.knp.model.entity.customer.Customer.builder()
                                .name(guestName)
                                .phone(guestPhone + System.currentTimeMillis() % 10000)
                                .build();
                        return customerRepository.save(guest).getId();
                    });
            pawnRequest.setCustomerId(customerId);
        }
        PawnEntity savingPawnEntity = pawnMapper.fromPawnRequest(pawnRequest);
        savingPawnEntity.setPawnDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDate()));
        savingPawnEntity.setPawnDueDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDueDate()));
        savingPawnEntity.setCreatedAt(LocalDateTime.now());
        savingPawnEntity.setUpdatedAt(LocalDateTime.now());
        savingPawnEntity.setCreatedBy(authContext.getCurrentUsername());
        savingPawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        savingPawnEntity.setPawnStatus(PawnStatus.PAWNED);
        savingPawnEntity.setVisible(true);
        PawnEntity savedEntity = pawnRepository.save(savingPawnEntity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_CREATED, "PAWN", String.valueOf(savedEntity.getPawnId()),
                messageService.getMessage("activity.pawn.created", savedEntity.getItemName()), null);
        return getPawnDetails(savedEntity.getPawnId());
    }

    @Override
    public Page<PawnResponse> getPawns(Pageable pageable, SearchPawnRequest searchRequest) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(cutoffMonths);
        var specifications = new PawnSpecificationBuilder().buildPawnSpecifications(searchRequest).and(excludeOldRedeemedItems(threeMonthsAgo));
        if (shopInfoService.getExcludeVisibleItemFlag()) {
            specifications = specifications.and(PawnSpecification.includeVisibleStatus());
        }
        Page<PawnQuery> pawnsPage = queryRepository.findAll(specifications, pageable);
        log.info("Applying Specification: {}", specifications);
        return pawnsPage.map(pawnMapper::fromPawnQuery);
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
        pawnEntity.setInterestDaysPerMonth(pawnRequest.getInterestDaysPerMonth());
        pawnEntity.setHeldDays(pawnEntity.getHeldDays());
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
            pawnResponse.setCustomerName(customer.getName());
            pawnResponse.setPhone(customer.getPhone());
        });
        List<PawnAuditEntity> auditEntities = auditRepository.findByPawnIdOrderByActionIdAsc(pawnId);
        List<PawnAudit> pawnAudits = auditEntities.parallelStream().map(pawnMapper::auditFromPawnAuditEntity).toList();
        pawnResponse.setAudits(pawnAudits);
        return pawnResponse;
    }

    @Override
    public void deletePawnByPawnIds(List<Long> pawnIds) {
        log.info("Process deleting pawns by given PawnIds {}", pawnIds);
        List<PawnEntity> pawnEntities = pawnRepository.findAllById(pawnIds);
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
            pawnResponse.setCustomerName(customer.getName());
            pawnResponse.setPhone(customer.getPhone());
        });
        if (redeemRequest != null && redeemRequest.getRedeemDate() != null) {
            redeemDate = DateTimeUtil.fromLocalDateTime(redeemRequest.getRedeemDate());
        }
        long pawnedDays = calculateHeldDays(redeemDate, pawnResponse.getPawnDate(), isExtending);
        int interestDaysPerMonth = getInterestDaysPerMonth(pawnEntity.getInterestDaysPerMonth());
        BigDecimal mainInterestAmount = calculateInterestAmount(pawnedDays, interestDaysPerMonth, pawnResponse.getInterestRate(), pawnResponse.getPawnAmount());
        pawnResponse.setMainInterestAmount(mainInterestAmount);
        BigDecimal interestAmount = pawnResponse.getMainInterestAmount();
        BigDecimal requestedAmount = pawnResponse.getPawnAmount();
        Set<ReqMoneyResponse> reqMoneys = new HashSet<>();
        for (ReqMoneyEntity reqMoneyEntity : pawnEntity.getReqMoneys()) {
            long heldDays = calculateHeldDays(redeemDate, reqMoneyEntity.getRequestDate(), isExtending);
            BigDecimal subInterestAmount = calculateInterestAmount(heldDays, interestDaysPerMonth, pawnResponse.getInterestRate(), reqMoneyEntity.getRequestAmount());
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
        List<PawnAudit> pawnAudits = auditEntities.parallelStream().map(pawnMapper::auditFromPawnAuditEntity).toList();
        pawnResponse.setAudits(pawnAudits);
        return pawnResponse;
    }

    private int getInterestDaysPerMonth(Integer interestDaysPerMonth) {
        return interestDaysPerMonth != null ? interestDaysPerMonth : INTEREST_DAY_PER_MONTH;
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

    private BigDecimal calculateInterestAmount(long pawnedDays, int interestDaysPerMonth, BigDecimal interestRate, BigDecimal pawnAmount) {
        BigDecimal dayRate = interestRate.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP);
        BigDecimal rateAmountPerDay = pawnAmount.multiply(dayRate).divide(BigDecimal.valueOf(INTEREST_DAY_PER_MONTH), 5, RoundingMode.HALF_UP);
        if (interestDaysPerMonth == INTEREST_BY_DAY_25DAY_PER_MONTH.code) {
            long fullPeriods = pawnedDays / 30;
            long remainingDays = pawnedDays % 30;
            BigDecimal interestForFullPeriods = rateAmountPerDay.multiply(BigDecimal.valueOf(INTEREST_BY_DAY_25DAY_PER_MONTH.code)).multiply(BigDecimal.valueOf(fullPeriods));
            BigDecimal interestForRemainingDays = BigDecimal.ZERO;
            if (remainingDays > 0 && remainingDays <= INTEREST_BY_DAY_25DAY_PER_MONTH.code) {
                interestForRemainingDays = rateAmountPerDay.multiply(BigDecimal.valueOf(remainingDays));
            } else if (remainingDays > INTEREST_BY_DAY_25DAY_PER_MONTH.code) {
                interestForRemainingDays = rateAmountPerDay.multiply(BigDecimal.valueOf(INTEREST_BY_DAY_25DAY_PER_MONTH.code));
            }
            return interestForFullPeriods.add(interestForRemainingDays);
        } else if (interestDaysPerMonth == INTEREST_BY_DAY_FULL_MONTH.code) {
            return rateAmountPerDay.multiply(BigDecimal.valueOf(pawnedDays));
        } else {
            if ((pawnedDays - interestDaysPerMonth) > 0 && (pawnedDays - INTEREST_DAY_PER_MONTH) > 0) {
                return rateAmountPerDay.multiply(BigDecimal.valueOf(interestDaysPerMonth)).add(rateAmountPerDay.multiply(BigDecimal.valueOf(pawnedDays - INTEREST_DAY_PER_MONTH)));
            } else {
                return rateAmountPerDay.multiply(BigDecimal.valueOf(pawnedDays));
            }
        }
    }

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
        ReqMoneyEntity reqMoneyEntity = ReqMoneyEntity.builder()
                .requestAmount(reqMoneyRequest.getRequestAmount())
                .pawnId(pawnId)
                .requestDate(reqMoneyRequest.getRequestDate() != null ? reqMoneyRequest.getRequestDate().atTime(LocalTime.now()) : null)
                .createdBy(authContext.getCurrentUsername())
                .build();
        ReqMoneyEntity savedEntity = reqMoneyRepository.save(reqMoneyEntity);
        PawnEntity pawnActivityEntity = getPawnActivity(pawnEntity, reqMoneyEntity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_REQUEST_MONEY, "PAWN", String.valueOf(pawnEntity.getPawnId()),
                messageService.getMessage("activity.pawn.request_money", pawnEntity.getItemName()), null);
        return pawnMapper.fromReqMoneyEntity(savedEntity);
    }

    private PawnEntity getPawnActivity(PawnEntity pawnEntity, ReqMoneyEntity reqMoneyEntity) {
        pawnEntity.setUpdatedBy(reqMoneyEntity.getCreatedBy());
        pawnEntity.setPawnAmount(reqMoneyEntity.getRequestAmount());
        return pawnEntity;
    }

    @Override
    public PawnResponse extendPawn(Long pawnId, PawnRequest pawnRequest) {
        log.info("Extending Pawn for id: {}", pawnId);
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        PawnEntity extendingPawn = createExtendPawn(pawnEntity, pawnRequest);
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
                .interestDaysPerMonth(pawnEntity.getInterestDaysPerMonth())
                .originalId(pawnEntity.getPawnId())
                .pawnStatus(PawnStatus.PAWNED)
                .createdBy(authContext.getCurrentUsername())
                .pawnAmount(requestedAmount)
                .pawnDate(DateTimeUtil.fromLocalDateTime(extendRequest.getExtendDate()))
                .pawnDueDate(DateTimeUtil.fromLocalDateTime(extendRequest.getExtendDueDate()))
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
}
