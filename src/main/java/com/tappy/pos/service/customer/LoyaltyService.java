package com.tappy.pos.service.customer;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.loyalty.*;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.customer.LoyaltyProgram;
import com.tappy.pos.model.entity.customer.LoyaltyTier;
import com.tappy.pos.model.entity.customer.LoyaltyTransaction;
import com.tappy.pos.model.enums.LoyaltyTransactionType;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.customer.LoyaltyProgramRepository;
import com.tappy.pos.repository.customer.LoyaltyTierRepository;
import com.tappy.pos.repository.customer.LoyaltyTransactionRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyService {

    private final LoyaltyProgramRepository programRepository;
    private final LoyaltyTierRepository tierRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final MessageService messageService;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;
    private final AuthContext authContext;

    // ── Program ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoyaltyProgramDTO getProgram() {
        return programRepository.findActiveProgram()
                .map(this::mapProgramToDTO)
                .orElseGet(() -> {
                    LoyaltyProgram defaults = new LoyaltyProgram();
                    return mapProgramToDTO(defaults);
                });
    }

    public LoyaltyProgramDTO saveProgram(SaveLoyaltyProgramRequest req) {
        LoyaltyProgram program = programRepository.findActiveProgram()
                .orElseGet(LoyaltyProgram::new);

        if (req.getPointsPerAmount() != null) program.setPointsPerAmount(req.getPointsPerAmount());
        if (req.getAmountPerPoints() != null) program.setAmountPerPoints(req.getAmountPerPoints());
        if (req.getRedemptionPointsPerDiscount() != null) program.setRedemptionPointsPerDiscount(req.getRedemptionPointsPerDiscount());
        if (req.getRedemptionDiscountAmount() != null) program.setRedemptionDiscountAmount(req.getRedemptionDiscountAmount());
        if (req.getMinRedemptionPoints() != null) program.setMinRedemptionPoints(req.getMinRedemptionPoints());
        if (req.getIsActive() != null) program.setIsActive(req.getIsActive());
        if (req.getStampCardEnabled() != null) program.setStampCardEnabled(req.getStampCardEnabled());
        if (req.getStampCardSize() != null && req.getStampCardSize() > 0) program.setStampCardSize(req.getStampCardSize());
        if (req.getStampCardReward() != null && !req.getStampCardReward().isBlank()) program.setStampCardReward(req.getStampCardReward());

        return mapProgramToDTO(programRepository.save(program));
    }

    // ── Tiers ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LoyaltyTierDTO> getTiers() {
        return tierRepository.findAllActive().stream()
                .map(this::mapTierToDTO)
                .collect(Collectors.toList());
    }

    public LoyaltyTierDTO createTier(CreateLoyaltyTierRequest req) {
        LoyaltyTier tier = LoyaltyTier.builder()
                .name(req.getName())
                .minSpend(req.getMinSpend() != null ? req.getMinSpend() : BigDecimal.ZERO)
                .pointsMultiplier(req.getPointsMultiplier() != null ? req.getPointsMultiplier() : BigDecimal.ONE)
                .color(req.getColor() != null ? req.getColor() : "#9E9E9E")
                .description(req.getDescription())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        return mapTierToDTO(tierRepository.save(tier));
    }

    public LoyaltyTierDTO updateTier(Long id, CreateLoyaltyTierRequest req) {
        LoyaltyTier tier = tierRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.loyalty.tier.not.found", id)));
        if (req.getName() != null) tier.setName(req.getName());
        if (req.getMinSpend() != null) tier.setMinSpend(req.getMinSpend());
        if (req.getPointsMultiplier() != null) tier.setPointsMultiplier(req.getPointsMultiplier());
        if (req.getColor() != null) tier.setColor(req.getColor());
        if (req.getDescription() != null) tier.setDescription(req.getDescription());
        if (req.getSortOrder() != null) tier.setSortOrder(req.getSortOrder());
        return mapTierToDTO(tierRepository.save(tier));
    }

    public void deleteTier(Long id) {
        LoyaltyTier tier = tierRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.loyalty.tier.not.found", id)));
        tier.softDelete();
        tierRepository.save(tier);
    }

    // ── Customer Loyalty ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerLoyaltySummaryDTO getCustomerLoyalty(Long customerId) {
        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.customer.not.found", customerId)));

        BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        LoyaltyTierDTO currentTier = tierRepository.findTierForSpend(totalSpent).map(this::mapTierToDTO).orElse(null);
        LoyaltyTierDTO nextTier = tierRepository.findNextTierForSpend(totalSpent).map(this::mapTierToDTO).orElse(null);
        BigDecimal amountToNext = nextTier != null ? nextTier.getMinSpend().subtract(totalSpent) : null;

        LoyaltyProgram program = programRepository.findActiveProgram().orElse(null);
        boolean stampEnabled = program != null && Boolean.TRUE.equals(program.getStampCardEnabled());

        return CustomerLoyaltySummaryDTO.builder()
                .customerId(customerId)
                .customerName(customer.getName())
                .loyaltyPoints(customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0)
                .totalSpent(totalSpent)
                .currentTier(currentTier)
                .nextTier(nextTier)
                .amountToNextTier(amountToNext)
                .stampCardEnabled(stampEnabled)
                .stampCount(customer.getStampCount() != null ? customer.getStampCount() : 0)
                .stampCardSize(program != null ? program.getStampCardSize() : null)
                .stampRewards(customer.getStampRewards() != null ? customer.getStampRewards() : 0)
                .stampCardReward(program != null ? program.getStampCardReward() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionDTO> getTransactionHistory(Long customerId, Pageable pageable) {
        return transactionRepository.findByCustomerId(customerId, pageable)
                .map(this::mapTransactionToDTO);
    }

    /**
     * Award points on order completion. Idempotent — skipped if this order already has a transaction.
     */
    public void awardPointsForOrder(Long customerId, Long orderId, BigDecimal orderAmount) {
        if (transactionRepository.findByOrderId(orderId).isPresent()) {
            log.debug("Loyalty points already awarded for order {}", orderId);
            return;
        }

        LoyaltyProgram program = programRepository.findActiveProgram().orElse(null);
        if (program == null || !Boolean.TRUE.equals(program.getIsActive())) {
            log.debug("No active loyalty program — skipping point award for order {}", orderId);
            return;
        }

        Customer customer = customerRepository.findByIdActive(customerId).orElse(null);
        if (customer == null) return;

        BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        BigDecimal multiplier = tierRepository.findTierForSpend(totalSpent)
                .map(LoyaltyTier::getPointsMultiplier)
                .orElse(BigDecimal.ONE);

        // points = floor(orderAmount / amountPerPoints) * pointsPerAmount * multiplier
        long units = orderAmount.divide(BigDecimal.valueOf(program.getAmountPerPoints()), 0, RoundingMode.FLOOR).longValue();
        int basePoints = (int) (units * program.getPointsPerAmount());
        int earnedPoints = multiplier.multiply(BigDecimal.valueOf(basePoints)).setScale(0, RoundingMode.FLOOR).intValue();

        if (earnedPoints <= 0) return;

        int balanceBefore = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int balanceAfter = balanceBefore + earnedPoints;

        customer.setLoyaltyPoints(balanceAfter);
        customer.setTotalSpent(totalSpent.add(orderAmount));
        customerRepository.save(customer);

        transactionRepository.save(LoyaltyTransaction.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .customerId(customerId)
                .orderId(orderId)
                .type(LoyaltyTransactionType.EARNED)
                .points(earnedPoints)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description("Tích điểm đơn hàng #" + orderId)
                .build());

        log.info("Awarded {} loyalty points to customer {} for order {} (balance: {} → {})",
                earnedPoints, customerId, orderId, balanceBefore, balanceAfter);
    }

    /**
     * Accrue stamp-card stamps on a completed order ("mua N ly tặng 1"). No-op unless the
     * tenant has opted into the stamp card, so every other shop type is unaffected.
     * Each qualifying unit (cup/món) is one stamp; whenever the card fills, it converts to a
     * free-item reward and the count rolls over. Failures must never break the order flow.
     *
     * @param stampQty number of stamps to add (typically the total item quantity in the order)
     */
    public void awardStampsForOrder(Long customerId, Long orderId, int stampQty) {
        if (customerId == null || stampQty <= 0) return;

        LoyaltyProgram program = programRepository.findActiveProgram().orElse(null);
        if (program == null || !Boolean.TRUE.equals(program.getStampCardEnabled())) return;

        int size = program.getStampCardSize() != null && program.getStampCardSize() > 0
                ? program.getStampCardSize() : 10;

        Customer customer = customerRepository.findByIdActive(customerId).orElse(null);
        if (customer == null) return;

        int total = (customer.getStampCount() != null ? customer.getStampCount() : 0) + stampQty;
        int newRewards = total / size;
        int remaining = total % size;

        customer.setStampCount(remaining);
        if (newRewards > 0) {
            int rewards = (customer.getStampRewards() != null ? customer.getStampRewards() : 0) + newRewards;
            customer.setStampRewards(rewards);
        }
        customerRepository.save(customer);

        log.info("Awarded {} stamp(s) to customer {} for order {} (card {}/{}, +{} reward(s))",
                stampQty, customerId, orderId, remaining, size, newRewards);
    }

    /**
     * Redeem one filled stamp-card reward (staff gives the free item). Decrements the
     * available-reward counter and returns the refreshed loyalty summary.
     */
    public CustomerLoyaltySummaryDTO redeemStampReward(Long customerId) {
        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.customer.not.found", customerId)));

        int rewards = customer.getStampRewards() != null ? customer.getStampRewards() : 0;
        if (rewards <= 0) {
            throw new BadRequestException(messageService.getMessage("error.loyalty.noStampReward"));
        }
        customer.setStampRewards(rewards - 1);
        customerRepository.save(customer);
        return getCustomerLoyalty(customerId);
    }

    /**
     * Manually adjust customer points (positive = add, negative = deduct).
     */
    public LoyaltyTransactionDTO adjustPoints(Long customerId, int points, String description) {
        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.customer.not.found", customerId)));

        int balanceBefore = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int balanceAfter = Math.max(0, balanceBefore + points);

        customer.setLoyaltyPoints(balanceAfter);
        customerRepository.save(customer);

        LoyaltyTransaction tx = transactionRepository.save(LoyaltyTransaction.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .customerId(customerId)
                .type(LoyaltyTransactionType.ADJUSTED)
                .points(points)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Điều chỉnh điểm thủ công")
                .build());

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.LOYALTY_ADJUSTED, "LOYALTY", String.valueOf(customerId),
                "Điều chỉnh điểm khách hàng", null);

        return mapTransactionToDTO(tx);
    }

    /**
     * Redeem points for a discount. Returns the discount amount in VND.
     */
    public BigDecimal redeemPoints(Long customerId, int pointsToRedeem, Long orderId) {
        LoyaltyProgram program = programRepository.findActiveProgram()
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.loyalty.noActiveProgram")));

        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.customer.not.found", customerId)));

        int balance = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        if (pointsToRedeem < program.getMinRedemptionPoints()) {
            throw new BadRequestException(messageService.getMessage("error.loyalty.minPointsRequired", new Object[]{program.getMinRedemptionPoints()}));
        }
        if (balance < pointsToRedeem) {
            throw new BadRequestException(messageService.getMessage("error.loyalty.insufficientPoints", new Object[]{balance, pointsToRedeem}));
        }

        // discount = floor(pointsToRedeem / redemptionPointsPerDiscount) * redemptionDiscountAmount
        int units = pointsToRedeem / program.getRedemptionPointsPerDiscount();
        BigDecimal discountAmount = program.getRedemptionDiscountAmount().multiply(BigDecimal.valueOf(units));

        int balanceBefore = balance;
        int balanceAfter = balance - pointsToRedeem;
        customer.setLoyaltyPoints(balanceAfter);
        customerRepository.save(customer);

        transactionRepository.save(LoyaltyTransaction.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .customerId(customerId)
                .orderId(orderId)
                .type(LoyaltyTransactionType.REDEEMED)
                .points(-pointsToRedeem)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description("Đổi " + pointsToRedeem + " điểm lấy " + discountAmount.toPlainString() + " VND")
                .build());

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.LOYALTY_REDEEMED, "LOYALTY", String.valueOf(customerId),
                "Đổi điểm lấy ưu đãi", null);

        return discountAmount;
    }

    /**
     * Back-fills the orderId on the most recent REDEEMED transaction for this customer
     * that has a null orderId. Called after order is persisted at checkout.
     */
    public void backfillRedemptionOrderId(Long customerId, Long orderId) {
        transactionRepository.findTopRedemptionWithoutOrder(customerId).ifPresent(tx -> {
            tx.setOrderId(orderId);
            transactionRepository.save(tx);
        });
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private LoyaltyProgramDTO mapProgramToDTO(LoyaltyProgram p) {
        return LoyaltyProgramDTO.builder()
                .id(p.getId())
                .pointsPerAmount(p.getPointsPerAmount())
                .amountPerPoints(p.getAmountPerPoints())
                .redemptionPointsPerDiscount(p.getRedemptionPointsPerDiscount())
                .redemptionDiscountAmount(p.getRedemptionDiscountAmount())
                .minRedemptionPoints(p.getMinRedemptionPoints())
                .isActive(p.getIsActive())
                .stampCardEnabled(p.getStampCardEnabled())
                .stampCardSize(p.getStampCardSize())
                .stampCardReward(p.getStampCardReward())
                .build();
    }

    public LoyaltyTierDTO mapTierToDTO(LoyaltyTier t) {
        return LoyaltyTierDTO.builder()
                .id(t.getId())
                .name(t.getName())
                .minSpend(t.getMinSpend())
                .pointsMultiplier(t.getPointsMultiplier())
                .color(t.getColor())
                .description(t.getDescription())
                .sortOrder(t.getSortOrder())
                .build();
    }

    private LoyaltyTransactionDTO mapTransactionToDTO(LoyaltyTransaction t) {
        return LoyaltyTransactionDTO.builder()
                .id(t.getId())
                .customerId(t.getCustomerId())
                .orderId(t.getOrderId())
                .type(t.getType().name())
                .points(t.getPoints())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
