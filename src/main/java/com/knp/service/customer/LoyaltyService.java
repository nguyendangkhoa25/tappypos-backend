package com.knp.service.customer;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.loyalty.*;
import com.knp.model.entity.customer.Customer;
import com.knp.model.entity.customer.LoyaltyProgram;
import com.knp.model.entity.customer.LoyaltyTier;
import com.knp.model.entity.customer.LoyaltyTransaction;
import com.knp.model.enums.LoyaltyTransactionType;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.customer.LoyaltyProgramRepository;
import com.knp.repository.customer.LoyaltyTierRepository;
import com.knp.repository.customer.LoyaltyTransactionRepository;
import com.knp.service.MessageService;
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
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty tier not found: " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty tier not found: " + id));
        tier.softDelete();
        tierRepository.save(tier);
    }

    // ── Customer Loyalty ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerLoyaltySummaryDTO getCustomerLoyalty(Long customerId) {
        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        LoyaltyTierDTO currentTier = tierRepository.findTierForSpend(totalSpent).map(this::mapTierToDTO).orElse(null);
        LoyaltyTierDTO nextTier = tierRepository.findNextTierForSpend(totalSpent).map(this::mapTierToDTO).orElse(null);
        BigDecimal amountToNext = nextTier != null ? nextTier.getMinSpend().subtract(totalSpent) : null;

        return CustomerLoyaltySummaryDTO.builder()
                .customerId(customerId)
                .customerName(customer.getName())
                .loyaltyPoints(customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0)
                .totalSpent(totalSpent)
                .currentTier(currentTier)
                .nextTier(nextTier)
                .amountToNextTier(amountToNext)
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
     * Manually adjust customer points (positive = add, negative = deduct).
     */
    public LoyaltyTransactionDTO adjustPoints(Long customerId, int points, String description) {
        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        int balanceBefore = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int balanceAfter = Math.max(0, balanceBefore + points);

        customer.setLoyaltyPoints(balanceAfter);
        customerRepository.save(customer);

        LoyaltyTransaction tx = transactionRepository.save(LoyaltyTransaction.builder()
                .customerId(customerId)
                .type(LoyaltyTransactionType.ADJUSTED)
                .points(points)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Điều chỉnh điểm thủ công")
                .build());

        return mapTransactionToDTO(tx);
    }

    /**
     * Redeem points for a discount. Returns the discount amount in VND.
     */
    public BigDecimal redeemPoints(Long customerId, int pointsToRedeem, Long orderId) {
        LoyaltyProgram program = programRepository.findActiveProgram()
                .orElseThrow(() -> new BadRequestException("No active loyalty program"));

        Customer customer = customerRepository.findByIdActive(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

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
                .customerId(customerId)
                .orderId(orderId)
                .type(LoyaltyTransactionType.REDEEMED)
                .points(-pointsToRedeem)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description("Đổi " + pointsToRedeem + " điểm lấy " + discountAmount.toPlainString() + " VND")
                .build());

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
