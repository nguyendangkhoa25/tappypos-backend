package com.tappy.pos.service.pawn;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.PawnStatusNotAllowException;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.service.storage.R2StorageService;
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
import java.time.LocalDate;

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
    private final FeatureContext featureContext;
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
    private final PawnJewelryRepository jewelryRepository;
    private final R2StorageService r2StorageService;
    private final R2CleanupService r2CleanupService;

    @Value("${schedule.cleanup.pawn.cutoffMonths:3}")
    private int cutoffMonths;

    /** Cap on a decoded signature PNG — a finger-drawn signature is well under this. */
    private static final int MAX_SIGNATURE_BYTES = 256 * 1024;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

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
        // PAWN_VIEW_ALL sub-feature: scope list to createdBy when absent (mirrors ORDER_VIEW_ALL).
        if (!featureContext.hasFeature("PAWN_VIEW_ALL")) {
            specifications = specifications.and(PawnSpecification.filterByCreatedBy(authContext.getCurrentUsername()));
            log.info("PAWN_VIEW_ALL absent — scoping pawn list to createdBy={}", authContext.getCurrentUsername());
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
        pawnEntity.setItemBrand(pawnRequest.getItemBrand());
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        pawnEntity.setPawnAmount(pawnRequest.getPawnAmount());
        pawnEntity.setPawnDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDate()));
        pawnEntity.setPawnDueDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getPawnDueDate()));
        pawnEntity.setTotalAmount(pawnRequest.getTotalAmount());
        pawnEntity.setRedeemDate(DateTimeUtil.fromLocalDateTime(pawnRequest.getRedeemDate()));
        pawnEntity.setInterestAmount(pawnRequest.getInterestAmount());
        // Only overwrite status when the caller explicitly sends one.
        // A plain mobile edit never includes pawnStatus; blindly setting null here
        // would wipe the existing PAWNED/REDEEMED/... status and hide the record.
        if (pawnRequest.getPawnStatus() != null) {
            pawnEntity.setPawnStatus(pawnRequest.getPawnStatus());
        }
        pawnEntity.setInterestCalcMode(pawnRequest.getInterestCalcMode());
        // Only overwrite heldDays when explicitly provided (redeem/extend flows send it;
        // a plain edit never does and the primitive zero would corrupt the stored value).
        if (pawnRequest.getHeldDays() != null) {
            pawnEntity.setHeldDays(pawnRequest.getHeldDays().intValue());
        }
        pawnEntity.setPawnCategory(pawnRequest.getPawnCategory());
    }

    @Override
    public PawnResponse getPawnDetails(Long pawnId) {
        log.info("Get pawn details for pawnId {}", pawnId);
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        // Ownership guard: mirrors ORDER_VIEW_ALL pattern (CLAUDE.md → "Granular Sub-Feature Pattern").
        // Throws 404 (not 403) to avoid leaking record existence to staff who cannot view all contracts.
        if (!featureContext.hasFeature("PAWN_VIEW_ALL")) {
            String currentUser = authContext.getCurrentUsername();
            if (!currentUser.equals(pawnEntity.getCreatedBy())) {
                log.warn("User {} attempted to access pawn {} created by {} without PAWN_VIEW_ALL",
                        currentUser, pawnId, pawnEntity.getCreatedBy());
                throw new ResourceNotFoundException();
            }
        }
        PawnResponse pawnResponse = pawnMapper.fromPawnEntity(pawnEntity);
        if (CollectionUtils.isNotEmpty(pawnEntity.getReqMoneys())) {
            pawnResponse.setReqMoneys(pawnEntity.getReqMoneys().stream().map(pawnMapper::fromReqMoneyEntity).collect(Collectors.toSet()));
        }
        customerRepository.findById(pawnEntity.getCustomerId()).ifPresent(customer -> {
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .id(customer.getId())
                    .name(customer.getName())
                    .phone(customer.getPhone())
                    .idNumber(customer.getIdNumber())
                    .build();
            pawnResponse.setCustomer(customerDTO);
            // Prefer the name stored on the pawn itself (set at creation for visiting guests).
            String resolvedName = StringUtils.isNotEmpty(pawnEntity.getCustomerName())
                    ? pawnEntity.getCustomerName() : customer.getName();
            pawnResponse.setCustomerName(resolvedName);
            pawnResponse.setPhone(customer.getPhone());
            pawnResponse.setCustomerIdNumber(customer.getIdNumber());
        });
        enrichWithItemDetail(pawnResponse, pawnId, pawnEntity.getPawnCategory());
        List<PawnAuditEntity> auditEntities = auditRepository.findByPawnIdOrderByActionIdAsc(pawnId);
        List<PawnAudit> pawnAudits = auditEntities.stream().map(pawnMapper::auditFromPawnAuditEntity).toList();
        pawnResponse.setAudits(pawnAudits);
        return pawnResponse;
    }

    @Override
    public PawnResponse signContract(Long pawnId, SignPawnRequest request) {
        PawnEntity pawnEntity = loadPawnWithOwnershipGuard(pawnId);
        // Only an active contract can be signed (not redeemed/forfeited/cancelled).
        if (pawnEntity.getPawnStatus() != PawnStatus.PAWNED) {
            throw new PawnStatusNotAllowException(messageService.getMessage(
                    "error.pawn.statusNotAllowed", new Object[]{pawnEntity.getPawnStatus()}));
        }

        byte[] png = decodeSignaturePng(request.getSignature());

        // Upload the new signature first; only delete the old object once the new one is stored.
        String oldKey = r2StorageService.keyFromUrl(pawnEntity.getCustomerSignatureUrl());
        String key = "pawn-signatures/" + tenantContext.getCurrentTenantId() + "/"
                + pawnId + "-" + UUID.randomUUID() + ".png";
        String url = r2StorageService.upload(key, png, "image/png");

        pawnEntity.setCustomerSignatureUrl(StringUtils.isBlank(url) ? null : url);
        pawnEntity.setSignedAt(LocalDateTime.now());
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnRepository.save(pawnEntity);

        r2CleanupService.deleteAsync(oldKey);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.PAWN_SIGNED, "PAWN", String.valueOf(pawnId),
                messageService.getMessage("activity.pawn.signed", pawnId), null);

        return getPawnDetails(pawnId);
    }

    @Override
    public PawnResponse removeSignature(Long pawnId) {
        PawnEntity pawnEntity = loadPawnWithOwnershipGuard(pawnId);
        String oldKey = r2StorageService.keyFromUrl(pawnEntity.getCustomerSignatureUrl());
        pawnEntity.setCustomerSignatureUrl(null);
        pawnEntity.setSignedAt(null);
        pawnEntity.setUpdatedBy(authContext.getCurrentUsername());
        pawnEntity.setUpdatedAt(LocalDateTime.now());
        pawnRepository.save(pawnEntity);
        r2CleanupService.deleteAsync(oldKey);
        return getPawnDetails(pawnId);
    }

    /**
     * Loads a pawn and enforces the PAWN_VIEW_ALL ownership guard — a staffer who cannot view a
     * contract cannot sign/modify it. Throws 404 (not 403) to avoid leaking record existence,
     * mirroring {@link #getPawnDetails}.
     */
    private PawnEntity loadPawnWithOwnershipGuard(Long pawnId) {
        PawnEntity pawnEntity = pawnRepository.findById(pawnId).orElseThrow(ResourceNotFoundException::new);
        if (!featureContext.hasFeature("PAWN_VIEW_ALL")) {
            String currentUser = authContext.getCurrentUsername();
            if (!currentUser.equals(pawnEntity.getCreatedBy())) {
                log.warn("User {} attempted to modify pawn {} created by {} without PAWN_VIEW_ALL",
                        currentUser, pawnId, pawnEntity.getCreatedBy());
                throw new ResourceNotFoundException();
            }
        }
        return pawnEntity;
    }

    /** Decodes a PNG data URL / base64 signature, validating it is a PNG within the size cap. */
    private byte[] decodeSignaturePng(String signature) {
        String base64 = signature == null ? "" : signature.trim();
        int comma = base64.indexOf(',');
        if (base64.startsWith("data:") && comma > 0) {
            base64 = base64.substring(comma + 1);
        }
        base64 = base64.replaceAll("\\s", "");
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageService.getMessage("error.pawn.signatureInvalid"));
        }
        if (bytes.length < PNG_MAGIC.length || !startsWith(bytes, PNG_MAGIC)) {
            throw new BadRequestException(messageService.getMessage("error.pawn.signatureInvalid"));
        }
        if (bytes.length > MAX_SIGNATURE_BYTES) {
            throw new BadRequestException(messageService.getMessage("error.pawn.signatureTooLarge"));
        }
        return bytes;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public PawnResponse lookupByCode(String code) {
        log.info("Lookup pawn by code: {}", code);
        PawnEntity entity;
        try {
            long numericId = Long.parseLong(code.trim());
            entity = pawnRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException e) {
            entity = null;
        }
        if (entity == null) {
            entity = pawnRepository.findByLegacyId(code.trim()).orElseThrow(ResourceNotFoundException::new);
        }
        // Re-use the existing detail fetch (applies PAWN_VIEW_ALL ownership guard)
        return getPawnDetails(entity.getPawnId());
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
        jewelryRepository.deleteByPawnIdIn(pawnIds);
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
        invalidPawnStatus(PawnStatus.CANCELLED, pawnEntity);
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
        if (forfeitRequest.getForfeitedDate() == null) {
            throw new IllegalArgumentException(messageService.getMessage("error.pawn.forfeitDateRequired"));
        }
        if (forfeitRequest.getForfeitedAmount() == null || forfeitRequest.getForfeitedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(messageService.getMessage("error.pawn.forfeitAmountRequired"));
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
                    .idNumber(customer.getIdNumber())
                    .build();
            pawnResponse.setCustomer(customerDTO);
            String resolvedName = StringUtils.isNotEmpty(pawnEntity.getCustomerName())
                    ? pawnEntity.getCustomerName() : customer.getName();
            pawnResponse.setCustomerName(resolvedName);
            pawnResponse.setPhone(customer.getPhone());
            pawnResponse.setCustomerIdNumber(customer.getIdNumber());
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
            // Fall back to pawnDate when requestDate is missing (guards against legacy null rows)
            LocalDateTime reqDate = reqMoneyEntity.getRequestDate() != null
                    ? reqMoneyEntity.getRequestDate()
                    : pawnEntity.getPawnDate();
            long heldDays = calculateHeldDays(redeemDate, reqDate, isExtending);
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
        invalidPawnStatus(PawnStatus.FORFEITED, pawnEntity);
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
                .requestDate(reqMoneyRequest.getRequestDate() != null ? reqMoneyRequest.getRequestDate().atStartOfDay() : null)
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
        pawnEntity.setPawnStatus(PawnStatus.EXTENDED);
        PawnEntity redeemedPawn = pawnRepository.save(pawnEntity);
        PawnEntity savedExtendEntity = pawnRepository.save(extendingPawn);
        // Copy category-specific detail to the new contract so item metadata is not lost on extend.
        String tid = tenantContext.getCurrentTenantId();
        Long newId = savedExtendEntity.getPawnId();
        String cat = pawnEntity.getPawnCategory();
        if ("JEWELRY".equals(cat)) {
            jewelryRepository.findByPawnId(pawnId).ifPresent(o ->
                    jewelryRepository.save(pawnMapper.toJewelryEntity(tid, newId, pawnMapper.fromJewelryEntity(o))));
        } else if ("ELECTRONICS".equals(cat)) {
            electronicsRepository.findByPawnId(pawnId).ifPresent(o ->
                    electronicsRepository.save(pawnMapper.toElectronicsEntity(tid, newId, pawnMapper.fromElectronicsEntity(o))));
        } else if ("VEHICLE".equals(cat) || "MOTORBIKE".equals(cat) || "CAR".equals(cat) || "BIKE".equals(cat)) {
            vehicleRepository.findByPawnId(pawnId).ifPresent(o ->
                    vehicleRepository.save(pawnMapper.toVehicleEntity(tid, newId, pawnMapper.fromVehicleEntity(o))));
        } else if ("WATCH".equals(cat)) {
            watchRepository.findByPawnId(pawnId).ifPresent(o ->
                    watchRepository.save(pawnMapper.toWatchEntity(tid, newId, pawnMapper.fromWatchEntity(o))));
        } else if ("REAL_ESTATE".equals(cat)) {
            realEstateRepository.findByPawnId(pawnId).ifPresent(o ->
                    realEstateRepository.save(pawnMapper.toRealEstateEntity(tid, newId, pawnMapper.fromRealEstateEntity(o))));
        } else if ("GENERAL".equals(cat) || "OTHER".equals(cat)) {
            generalRepository.findByPawnId(pawnId).ifPresent(o ->
                    generalRepository.save(pawnMapper.toGeneralEntity(tid, newId, pawnMapper.fromGeneralEntity(o))));
        }
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
                // Copy raw string directly — enum round-trip via fromCode() would silently
                // replace any unrecognised value with DAILY_30, changing the new contract's mode.
                .interestCalcMode(pawnEntity.getInterestCalcMode())
                // Carry the per-pawn guest name override so the extended contract
                // retains the correct customer name even for walk-in customers.
                .customerName(pawnEntity.getCustomerName())
                .originalId(pawnEntity.getPawnId())
                .pawnStatus(PawnStatus.PAWNED)
                .createdBy(authContext.getCurrentUsername())
                .pawnAmount(requestedAmount)
                .pawnDate(DateTimeUtil.fromLocalDateTime(extendRequest.getExtendDate()))
                .pawnDueDate(DateTimeUtil.fromLocalDateTime(extendRequest.getExtendDueDate()))
                .pawnCategory(pawnEntity.getPawnCategory())
                .visible(true)
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
        // Real-time stats always use current date — independent of the request date range
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime upcomingEnd = LocalDate.now().plusDays(3).atTime(LocalTime.MAX);
        PawnKPIs dueTodayData = getUpComingDueAmount(todayStart, todayEnd, visibleFlag);
        PawnKPIs upcomingData = getUpcomingPawns(tomorrowStart, upcomingEnd, visibleFlag);
        PawnKPIs overDue = getOverdueAmount(LocalDate.now().atTime(LocalTime.MAX), visibleFlag);
        PawnKPIs newPawn = getNewPawnsAmount(fromDate, toDate, visibleFlag);
        PawnKPIs newMoneyRequest = getNewRequestMoneyAmount(fromDate, toDate, visibleFlag);
        PawnKPIs redeemPawn = getRedeemedPawnAmount(fromDate, toDate, visibleFlag);
        PawnKPIs forfeited = getForfeitedPawnAmount(fromDate, toDate, visibleFlag);
        PawnKPIs extended = getExtendedPawnAmount(fromDate, toDate, visibleFlag);
        PawnKPIs activePawn = getTotalActivePawns(visibleFlag);
        return PawnKPIs.builder()
                .totalPawnedCount(activePawn.getTotalPawnedCount())
                .totalPawnedAmount(activePawn.getTotalPawnedAmount())
                .dueTodayCount(dueTodayData.getDueTodayCount())
                .dueTodayAmount(dueTodayData.getDueTodayAmount())
                .upcomingCount(upcomingData.getUpcomingCount())
                .upcomingAmount(upcomingData.getUpcomingAmount())
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
                .extendedPawnCount(extended.getExtendedPawnCount())
                .extendedPawnAmount(extended.getExtendedPawnAmount())
                .interestPawnAmount(getInterestPawnAmount(fromDate, toDate, visibleFlag))
                .closedPawnPureAmount(redeemPawn.getClosedPawnPureAmount() + forfeited.getClosedPawnPureAmount())
                .unsignedContractCount(pawnRepository.countUnsignedActivePawnContracts().intValue())
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

    private PawnKPIs getUpcomingPawns(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusAndPawnDueDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        Long requestAmount = pawnRepository.sumRequestAmountByPawnStatusAndPawnDueDateBetween(PawnStatus.PAWNED, fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setUpcomingAmount(totalAmount.longValue() + requestAmount);
            pawnKPIs.setUpcomingCount((int) totalCount);
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
        // Count all original contracts created in the period regardless of current status.
        // originalId IS NULL excludes extension contracts (which also receive a new pawnDate).
        // newPawnsAmount stores pure SUM(pawnAmount) only — additional draws are tracked separately
        // via newRequestMoneyAmount so they don't inflate the funnel "Created" figure.
        List<Object[]> resultList = pawnRepository.sumNewOriginalPawnsByPawnDate(fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setNewPawnsAmount(totalAmount.longValue());
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
            // Pure pawnAmount without additional draws — used for avgLoan on closed cohort
            pawnKPIs.setClosedPawnPureAmount(totalAmount.longValue());
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
            // Pure pawnAmount without additional draws
            pawnKPIs.setClosedPawnPureAmount(totalAmount.longValue());
        }
        return pawnKPIs;
    }

    @Override
    @Transactional(readOnly = true)
    public PawnCustomerInsights getPawnCustomerInsights(LocalDate from, LocalDate to) {
        boolean visibleFlag = shopInfoService.getExcludeVisibleItemFlag();
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toDate = to.atTime(LocalTime.MAX);
        long total = pawnRepository.countDistinctPawnCustomers(fromDate, toDate, visibleFlag);
        long newCount = pawnRepository.countNewPawnCustomers(fromDate, toDate, visibleFlag);
        long walkIn = pawnRepository.countWalkInPawns(fromDate, toDate, visibleFlag);
        return PawnCustomerInsights.builder()
                .totalCustomers(total)
                .newCustomers(newCount)
                .returningCustomers(Math.max(0, total - newCount))
                .walkInCount(walkIn)
                .build();
    }

    private PawnKPIs getExtendedPawnAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        PawnKPIs pawnKPIs = PawnKPIs.builder().build();
        List<Object[]> resultList = pawnRepository.sumByPawnStatusAndUpdatedAtBetween(PawnStatus.EXTENDED, fromDate, toDate, visibleFlag);
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result != null ? (Long) result[1] : 0;
            pawnKPIs.setExtendedPawnAmount(totalAmount.longValue());
            pawnKPIs.setExtendedPawnCount((int) totalCount);
        }
        return pawnKPIs;
    }

    private long getInterestPawnAmount(LocalDateTime fromDate, LocalDateTime toDate, boolean visibleFlag) {
        Long closedInterest = pawnRepository.sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(
                Arrays.asList(PawnStatus.REDEEMED, PawnStatus.FORFEITED), fromDate, toDate, visibleFlag);
        Long extendedInterest = pawnRepository.sumInterestAmountByExtendedAndUpdatedAtBetween(fromDate, toDate, visibleFlag);
        return (closedInterest == null ? 0 : closedInterest) + (extendedInterest == null ? 0 : extendedInterest);
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
        String granularity = dateFilter.getGranularity() != null ? dateFilter.getGranularity() : "month";
        log.info("getPawnBars({}, granularity={})", dateFilter, granularity);
        LocalDateTime fromDate = Instant.ofEpochMilli(dateFilter.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toDate = Instant.ofEpochMilli(dateFilter.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();

        List<BarsData> pawnedBars, redeemBars, forfeitedBars, interestBars;

        if ("day".equals(granularity) || "week".equals(granularity)) {
            // Fetch day-level data from DB, then aggregate to week in Java if needed
            List<BarsData> pawnedDay = convertDayBarsData(pawnRepository.getAmountByDayPawnDate(fromDate, toDate, visibleItemFlag));
            List<BarsData> redeemDay = convertDayBarsData(pawnRepository.getAmountByDayRedeemDate(Collections.singletonList(PawnStatus.REDEEMED), fromDate, toDate, visibleItemFlag));
            List<BarsData> forfeitedDay = convertDayBarsData(pawnRepository.getAmountByDayForfeitedDate(Collections.singletonList(PawnStatus.FORFEITED), fromDate, toDate, visibleItemFlag));
            List<BarsData> redeemedIntDay = convertDayBarsData(pawnRepository.getRedeemedInterestByDay(PawnStatus.REDEEMED, fromDate, toDate, visibleItemFlag));
            List<BarsData> forfeitedIntDay = convertDayBarsData(pawnRepository.getForfeitedInterestByDay(PawnStatus.FORFEITED, fromDate, toDate, visibleItemFlag));
            List<BarsData> extendedIntDay = convertDayBarsData(pawnRepository.getExtendedInterestByDay(fromDate, toDate, visibleItemFlag));

            if ("week".equals(granularity)) {
                pawnedBars    = aggregateToWeek(pawnedDay);
                redeemBars    = aggregateToWeek(redeemDay);
                forfeitedBars = aggregateToWeek(forfeitedDay);
                interestBars  = mergeInterestBarsData(mergeInterestBarsData(aggregateToWeek(redeemedIntDay), aggregateToWeek(forfeitedIntDay)), aggregateToWeek(extendedIntDay));
            } else {
                pawnedBars    = pawnedDay;
                redeemBars    = redeemDay;
                forfeitedBars = forfeitedDay;
                interestBars  = mergeInterestBarsData(mergeInterestBarsData(redeemedIntDay, forfeitedIntDay), extendedIntDay);
            }
        } else if ("year".equals(granularity)) {
            // Fetch month-level data, aggregate to year in Java
            List<BarsData> pawnedMonth    = convertBarsData(pawnRepository.getAmountAndTotalCountByMonthPawnDateAndStatus(fromDate, toDate, visibleItemFlag));
            List<BarsData> redeemMonth    = convertBarsData(pawnRepository.getAmountAndTotalCountByMonthRedeemDateAndStatus(Collections.singletonList(PawnStatus.REDEEMED), fromDate, toDate, visibleItemFlag));
            List<BarsData> forfeitedMonth = convertBarsData(pawnRepository.getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(Collections.singletonList(PawnStatus.FORFEITED), fromDate, toDate, visibleItemFlag));
            List<BarsData> redeemedInt    = convertBarsData(pawnRepository.getRedeemedInterestAmount(PawnStatus.REDEEMED, fromDate, toDate, visibleItemFlag));
            List<BarsData> forfeitedInt   = convertBarsData(pawnRepository.getForfeitedInterestAmount(PawnStatus.FORFEITED, fromDate, toDate, visibleItemFlag));
            List<BarsData> extendedInt    = convertBarsData(pawnRepository.getExtendedInterestByMonth(fromDate, toDate, visibleItemFlag));
            pawnedBars    = aggregateToYear(pawnedMonth);
            redeemBars    = aggregateToYear(redeemMonth);
            forfeitedBars = aggregateToYear(forfeitedMonth);
            interestBars  = mergeInterestBarsData(mergeInterestBarsData(aggregateToYear(redeemedInt), aggregateToYear(forfeitedInt)), aggregateToYear(extendedInt));
        } else {
            // Default: month-level (existing behavior)
            List<Object[]> pawnedObjs        = pawnRepository.getAmountAndTotalCountByMonthPawnDateAndStatus(fromDate, toDate, visibleItemFlag);
            List<Object[]> redeemObjs        = pawnRepository.getAmountAndTotalCountByMonthRedeemDateAndStatus(Collections.singletonList(PawnStatus.REDEEMED), fromDate, toDate, visibleItemFlag);
            List<Object[]> forfeitedObjs     = pawnRepository.getAmountAndTotalCountByMonthForfeitedDateDateAndStatus(Collections.singletonList(PawnStatus.FORFEITED), fromDate, toDate, visibleItemFlag);
            List<Object[]> redeemedInterest  = pawnRepository.getRedeemedInterestAmount(PawnStatus.REDEEMED, fromDate, toDate, visibleItemFlag);
            List<Object[]> forfeitedInterest = pawnRepository.getForfeitedInterestAmount(PawnStatus.FORFEITED, fromDate, toDate, visibleItemFlag);
            List<Object[]> extendedInterest  = pawnRepository.getExtendedInterestByMonth(fromDate, toDate, visibleItemFlag);
            pawnedBars    = convertBarsData(pawnedObjs);
            redeemBars    = convertBarsData(redeemObjs);
            forfeitedBars = convertBarsData(forfeitedObjs);
            interestBars  = mergeInterestBarsData(mergeInterestBarsData(convertBarsData(redeemedInterest), convertBarsData(forfeitedInterest)), convertBarsData(extendedInterest));
        }

        return PawnBarsResponse.builder()
                .pawnedBars(pawnedBars)
                .redeemBars(redeemBars)
                .forfeitedBars(forfeitedBars)
                .interestBars(interestBars)
                .build();
    }

    /** Aggregate day-level BarsData into ISO-week buckets (label = Monday date as "YYYY-MM-DD"). */
    private List<BarsData> aggregateToWeek(List<BarsData> dayBars) {
        Map<String, BarsData> weekMap = new LinkedHashMap<>();
        for (BarsData bar : dayBars) {
            LocalDate date = LocalDate.of(bar.getYear(), bar.getMonth(), bar.getDay());
            LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
            String key = weekStart.toString(); // "YYYY-MM-DD"
            weekMap.computeIfAbsent(key, k -> BarsData.builder().label(k).amount(0L).count(0).weight(0.0).build());
            BarsData bucket = weekMap.get(key);
            bucket.setAmount(bucket.getAmount() + bar.getAmount());
            bucket.setCount(bucket.getCount() + bar.getCount());
            bucket.setWeight(bucket.getWeight() + bar.getWeight());
        }
        return new ArrayList<>(weekMap.values());
    }

    /** Aggregate month-level BarsData into year buckets (label = "YYYY"). */
    private List<BarsData> aggregateToYear(List<BarsData> monthBars) {
        Map<String, BarsData> yearMap = new LinkedHashMap<>();
        for (BarsData bar : monthBars) {
            String key = String.valueOf(bar.getYear());
            yearMap.computeIfAbsent(key, k -> BarsData.builder().label(k).year(bar.getYear()).amount(0L).count(0).weight(0.0).build());
            BarsData bucket = yearMap.get(key);
            bucket.setAmount(bucket.getAmount() + bar.getAmount());
            bucket.setCount(bucket.getCount() + bar.getCount());
            bucket.setWeight(bucket.getWeight() + bar.getWeight());
        }
        return new ArrayList<>(yearMap.values());
    }

    /** Merge two interest bar lists by label key (works for any granularity since label is always set). */
    public static List<BarsData> mergeInterestBarsData(List<BarsData> redeemedBars, List<BarsData> forfeitedBars) {
        Map<String, BarsData> mergedMap = new LinkedHashMap<>();
        for (BarsData data : redeemedBars) {
            String key = data.getLabel() != null ? data.getLabel() : (data.getYear() + "-" + data.getMonth());
            mergedMap.putIfAbsent(key, BarsData.builder().label(key).year(data.getYear()).month(data.getMonth()).amount(0L).count(0).weight(0.0).build());
            BarsData existing = mergedMap.get(key);
            existing.setAmount(existing.getAmount() + data.getAmount());
            existing.setCount(existing.getCount() + data.getCount());
            existing.setWeight(existing.getWeight() + data.getWeight());
        }
        for (BarsData data : forfeitedBars) {
            String key = data.getLabel() != null ? data.getLabel() : (data.getYear() + "-" + data.getMonth());
            mergedMap.putIfAbsent(key, BarsData.builder().label(key).year(data.getYear()).month(data.getMonth()).amount(0L).count(0).weight(0.0).build());
            BarsData existing = mergedMap.get(key);
            existing.setAmount(existing.getAmount() + data.getAmount());
            existing.setCount(existing.getCount() + data.getCount());
            existing.setWeight(existing.getWeight() + data.getWeight());
        }
        return new ArrayList<>(mergedMap.values());
    }

    /** Convert month-level Object[] rows → BarsData with label = "YYYY-MM". */
    private List<BarsData> convertBarsData(List<Object[]> resultList) {
        List<BarsData> bars = new ArrayList<>();
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result[1] != null ? (Long) result[1] : 0;
            int year  = result[2] != null ? (Integer) result[2] : 0;
            int month = result[3] != null ? (Integer) result[3] : 0;
            String label = String.format("%d-%02d", year, month);
            bars.add(BarsData.builder().label(label).amount(totalAmount.longValue()).count((int) totalCount).year(year).month(month).build());
        }
        return bars;
    }

    /** Convert day-level Object[] rows [amount, count, year, month, day] → BarsData with label = "YYYY-MM-DD". */
    private List<BarsData> convertDayBarsData(List<Object[]> resultList) {
        List<BarsData> bars = new ArrayList<>();
        for (Object[] result : resultList) {
            BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            long totalCount = result[1] != null ? (Long) result[1] : 0;
            int year  = result[2] != null ? (Integer) result[2] : 0;
            int month = result[3] != null ? (Integer) result[3] : 0;
            int day   = result[4] != null ? (Integer) result[4] : 0;
            String label = String.format("%d-%02d-%02d", year, month, day);
            bars.add(BarsData.builder().label(label).amount(totalAmount.longValue()).count((int) totalCount).year(year).month(month).day(day).build());
        }
        return bars;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopPawnCustomers(int limit, LocalDate from, LocalDate to) {
        boolean visibleItemFlag = shopInfoService.getExcludeVisibleItemFlag();
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toDate = to.atTime(LocalTime.MAX);
        List<Object[]> rows = pawnRepository.findTopCustomersByPawnAmount(
                fromDate, toDate, visibleItemFlag,
                org.springframework.data.domain.PageRequest.of(0, limit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("customerId", row[0]);
            m.put("name", row[1] != null ? row[1].toString() : "Khách vãng lai");
            m.put("totalCount", row[2] != null ? ((Number) row[2]).intValue() : 0);
            m.put("totalAmount", row[3] != null ? ((Number) row[3]).longValue() : 0L);
            m.put("interestAmount", row[4] != null ? ((Number) row[4]).longValue() : 0L);
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerPawnKpi(DateFilterRequest filter) {
        boolean visibleItemFlag = shopInfoService.getExcludeVisibleItemFlag();
        LocalDateTime fromDate = Instant.ofEpochMilli(filter.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toDate = Instant.ofEpochMilli(filter.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        Pageable top = org.springframework.data.domain.PageRequest.of(0, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topPawnedAmount", mapCustomerKpiRows(pawnRepository.findTopCustomersByPawnAmount(fromDate, toDate, visibleItemFlag, top)));
        result.put("topPawnedCount", mapCustomerKpiRows(pawnRepository.findTopCustomersByPawnCount(fromDate, toDate, visibleItemFlag, top)));
        result.put("topCompletedPawnAmount", mapCustomerKpiRows(pawnRepository.findTopCustomersByCompletedAmount(fromDate, toDate, visibleItemFlag, top)));
        result.put("topCompletedPawnCount", mapCustomerKpiRows(pawnRepository.findTopCustomersByCompletedCount(fromDate, toDate, visibleItemFlag, top)));
        result.put("topInterestAmount", mapCustomerKpiRows(pawnRepository.findTopCustomersByInterestAmount(fromDate, toDate, visibleItemFlag, top)));
        return result;
    }

    /** Maps a grouped-by-customer KPI row [customerId, customerName, count, pawnAmount, interestAmount]
     *  to the field names the customer pawn-KPI widget renders. */
    private List<Map<String, Object>> mapCustomerKpiRows(List<Object[]> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("customerId", row[0]);
            m.put("name", row[1] != null ? row[1].toString() : "Khách vãng lai");
            m.put("pawnCount", row[2] != null ? ((Number) row[2]).intValue() : 0);
            m.put("pawnAmount", row[3] != null ? ((Number) row[3]).longValue() : 0L);
            m.put("interestAmount", row[4] != null ? ((Number) row[4]).longValue() : 0L);
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerPawnSummary(Long customerId) {
        boolean visibleItemFlag = shopInfoService.getExcludeVisibleItemFlag();
        List<PawnStatus> allStatuses = List.of(PawnStatus.PAWNED, PawnStatus.REDEEMED, PawnStatus.EXTENDED, PawnStatus.FORFEITED);
        List<PawnStatus> activeStatuses = List.of(PawnStatus.PAWNED);

        List<Object[]> allRows    = pawnRepository.sumByPawnStatusInAndCustomerIdEquals(customerId, allStatuses, visibleItemFlag);
        List<Object[]> activeRows = pawnRepository.sumByPawnStatusInAndCustomerIdEquals(customerId, activeStatuses, visibleItemFlag);

        long totalCount    = allRows.isEmpty()    || allRows.get(0)[0] == null    ? 0L : ((Number) allRows.get(0)[0]).longValue();
        long totalPrincipal = allRows.isEmpty()   || allRows.get(0)[1] == null   ? 0L : ((Number) allRows.get(0)[1]).longValue();
        long totalInterest  = allRows.isEmpty()   || allRows.get(0)[2] == null   ? 0L : ((Number) allRows.get(0)[2]).longValue();
        long activeCount   = activeRows.isEmpty() || activeRows.get(0)[0] == null ? 0L : ((Number) activeRows.get(0)[0]).longValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", totalCount);
        result.put("activeCount", activeCount);
        result.put("totalPrincipal", totalPrincipal);
        result.put("totalInterest", totalInterest);
        return result;
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
        List<Long> vehicleIds     = responses.stream().filter(r -> "MOTORBIKE".equals(r.getPawnCategory()) || "CAR".equals(r.getPawnCategory()) || "VEHICLE".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> watchIds       = responses.stream().filter(r -> "WATCH".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> realEstateIds  = responses.stream().filter(r -> "REAL_ESTATE".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> generalIds     = responses.stream().filter(r -> "GENERAL".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();
        List<Long> jewelryIds     = responses.stream().filter(r -> "JEWELRY".equals(r.getPawnCategory())).map(PawnResponse::getPawnId).toList();

        Map<Long, PawnElectronicsDetail> electronicsMap = electronicIds.isEmpty() ? Map.of() : electronicsRepository.findByPawnIdIn(electronicIds).stream().collect(Collectors.toMap(PawnElectronicsEntity::getPawnId, pawnMapper::fromElectronicsEntity));
        Map<Long, PawnVehicleDetail>     vehicleMap     = vehicleIds.isEmpty()    ? Map.of() : vehicleRepository.findByPawnIdIn(vehicleIds).stream().collect(Collectors.toMap(PawnVehicleEntity::getPawnId, pawnMapper::fromVehicleEntity));
        Map<Long, PawnWatchDetail>       watchMap       = watchIds.isEmpty()      ? Map.of() : watchRepository.findByPawnIdIn(watchIds).stream().collect(Collectors.toMap(PawnWatchEntity::getPawnId, pawnMapper::fromWatchEntity));
        Map<Long, PawnRealEstateDetail>  realEstateMap  = realEstateIds.isEmpty() ? Map.of() : realEstateRepository.findByPawnIdIn(realEstateIds).stream().collect(Collectors.toMap(PawnRealEstateEntity::getPawnId, pawnMapper::fromRealEstateEntity));
        Map<Long, PawnGeneralDetail>     generalMap     = generalIds.isEmpty()    ? Map.of() : generalRepository.findByPawnIdIn(generalIds).stream().collect(Collectors.toMap(PawnGeneralEntity::getPawnId, pawnMapper::fromGeneralEntity));
        Map<Long, PawnJewelryDetail>     jewelryMap     = jewelryIds.isEmpty()    ? Map.of() : jewelryRepository.findByPawnIdIn(jewelryIds).stream().collect(Collectors.toMap(PawnJewelryEntity::getPawnId, pawnMapper::fromJewelryEntity));

        for (PawnResponse r : responses) {
            if (electronicsMap.containsKey(r.getPawnId()))  r.setElectronicsDetail(electronicsMap.get(r.getPawnId()));
            if (vehicleMap.containsKey(r.getPawnId()))      r.setVehicleDetail(vehicleMap.get(r.getPawnId()));
            if (watchMap.containsKey(r.getPawnId()))        r.setWatchDetail(watchMap.get(r.getPawnId()));
            if (realEstateMap.containsKey(r.getPawnId()))   r.setRealEstateDetail(realEstateMap.get(r.getPawnId()));
            if (generalMap.containsKey(r.getPawnId()))      r.setGeneralDetail(generalMap.get(r.getPawnId()));
            if (jewelryMap.containsKey(r.getPawnId()))      r.setJewelryDetail(jewelryMap.get(r.getPawnId()));
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
            case "MOTORBIKE", "CAR", "VEHICLE" -> {
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
            case "JEWELRY" -> {
                jewelryRepository.deleteByPawnId(pawnId);
                if (req.getJewelryDetail() != null) {
                    jewelryRepository.save(pawnMapper.toJewelryEntity(tenantId, pawnId, req.getJewelryDetail()));
                }
            }
        }
    }

    private void enrichWithItemDetail(PawnResponse response, Long pawnId, String category) {
        if (StringUtils.isEmpty(category)) return;
        switch (category) {
            case "ELECTRONICS" -> electronicsRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setElectronicsDetail(pawnMapper.fromElectronicsEntity(e)));
            case "MOTORBIKE", "CAR", "VEHICLE" -> vehicleRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setVehicleDetail(pawnMapper.fromVehicleEntity(e)));
            case "WATCH" -> watchRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setWatchDetail(pawnMapper.fromWatchEntity(e)));
            case "REAL_ESTATE" -> realEstateRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setRealEstateDetail(pawnMapper.fromRealEstateEntity(e)));
            case "GENERAL" -> generalRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setGeneralDetail(pawnMapper.fromGeneralEntity(e)));
            case "JEWELRY" -> jewelryRepository.findByPawnId(pawnId)
                    .ifPresent(e -> response.setJewelryDetail(pawnMapper.fromJewelryEntity(e)));
        }
    }
}
