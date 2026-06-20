package com.tappy.pos.service.tradein;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tradein.CreateTradeInRequest;
import com.tappy.pos.model.dto.tradein.TradeInDTO;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductType;
import com.tappy.pos.model.entity.tradein.TradeInEntity;
import com.tappy.pos.model.entity.vehicle.VehicleUnitEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.InventoryMode;
import com.tappy.pos.model.enums.TradeInMode;
import com.tappy.pos.model.enums.TradeInStatus;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.product.ProductTypeRepository;
import com.tappy.pos.repository.tradein.TradeInRepository;
import com.tappy.pos.repository.vehicle.VehicleUnitRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Trade-in (thu cũ đổi mới / mua xe cũ). Gated by TRADE_IN; TRADE_IN_VIEW_ALL controls list/detail
 * scope (own-vs-all, mirrors ORDER_VIEW_ALL). On create it auto-spawns a resale Product + a
 * TRADED_IN vehicle_unit and nets the valuation against a new-vehicle sale. VEHICLE_SHOP_SHOP_TYPE_PLAN §4c.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradeInServiceImpl implements TradeInService {

    private static final String VIEW_ALL = "TRADE_IN_VIEW_ALL";
    private static final DateTimeFormatter NUM_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TradeInRepository tradeInRepository;
    private final VehicleUnitRepository vehicleUnitRepository;
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final FeatureContext featureContext;
    private final ActivityLogService activityLogService;
    private final MessageService messageService;

    @Override
    public TradeInDTO create(CreateTradeInRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        String actor = authContext.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        TradeInMode mode = request.getMode() != null ? request.getMode() : TradeInMode.NETTED;

        // Resolve seller name from the customer record when only an id was supplied.
        String sellerName = request.getSellerName();
        if (StringUtils.isBlank(sellerName) && request.getSellerId() != null) {
            sellerName = customerRepository.findByIdActiveAndTenantId(request.getSellerId(), tid)
                    .map(Customer::getName).orElse(null);
        }

        String number = "TI-" + now.format(NUM_FMT) + "-" + (System.nanoTime() % 1000);

        // 1) Auto-create the resale Product (UNIQUE, holds the listing for the used xe).
        Product resaleProduct = createResaleProduct(tid, request, number);

        // 2) Auto-create the used vehicle_unit (status TRADED_IN, valuation as purchase/current value).
        VehicleUnitEntity unit = VehicleUnitEntity.builder()
                .tenantId(tid)
                .productId(resaleProduct.getId())
                .frameNo(StringUtils.trimToNull(request.getFrameNo()))
                .engineNo(StringUtils.trimToNull(request.getEngineNo()))
                .licensePlate(StringUtils.trimToNull(request.getLicensePlate()))
                .color(request.getColor())
                .odometerKm(request.getOdometerKm())
                .purchasePrice(request.getTradeValue())
                .currentValue(request.getResalePrice() != null ? request.getResalePrice() : request.getTradeValue())
                .status(VehicleUnitStatus.TRADED_IN)
                .conditionGrade("Cũ")
                .paperworkStatus(null)
                .notes(request.getConditionNotes())
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();
        VehicleUnitEntity savedUnit = vehicleUnitRepository.save(unit);

        // 3) Settlement.
        BigDecimal netAmount = null;
        if (mode == TradeInMode.NETTED && request.getNewPrice() != null) {
            netAmount = request.getNewPrice().subtract(request.getTradeValue());
        }

        TradeInEntity entity = TradeInEntity.builder()
                .tenantId(tid)
                .tradeInNumber(number)
                .sellerId(request.getSellerId())
                .sellerName(sellerName)
                .sellerPhone(request.getSellerPhone())
                .sellerIdNumber(request.getSellerIdNumber())
                .vehicleType(request.getVehicleType())
                .brand(request.getBrand())
                .model(request.getModel())
                .year(request.getYear())
                .frameNo(StringUtils.trimToNull(request.getFrameNo()))
                .engineNo(StringUtils.trimToNull(request.getEngineNo()))
                .licensePlate(StringUtils.trimToNull(request.getLicensePlate()))
                .color(request.getColor())
                .odometerKm(request.getOdometerKm())
                .conditionNotes(request.getConditionNotes())
                .tradeValue(request.getTradeValue())
                .mode(mode)
                .newSaleOrderId(request.getNewSaleOrderId())
                .newPrice(request.getNewPrice())
                .netAmount(netAmount)
                .resaleProductId(resaleProduct.getId())
                .resaleUnitId(savedUnit.getId())
                .status(TradeInStatus.COMPLETED)
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();
        TradeInEntity saved = tradeInRepository.save(entity);

        activityLogService.logAsync(tid, actor, null,
                ActivityAction.TRADE_IN_CREATED, "TRADE_IN", String.valueOf(saved.getId()),
                "Thu xe cũ: " + StringUtils.defaultString(request.getBrand()) + " "
                        + StringUtils.defaultString(request.getModel()) + " — giá thu " + request.getTradeValue() + "đ", null);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TradeInDTO getById(Long id) {
        TradeInEntity entity = tradeInRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(ResourceNotFoundException::new);
        // Ownership guard (mirrors ORDER_VIEW_ALL): 404, not 403, to avoid leaking existence.
        if (!featureContext.hasFeature(VIEW_ALL)
                && !authContext.getCurrentUsername().equals(entity.getCreatedBy())) {
            throw new ResourceNotFoundException();
        }
        return toDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TradeInDTO> search(TradeInStatus status, Pageable pageable) {
        Page<TradeInEntity> page = featureContext.hasFeature(VIEW_ALL)
                ? tradeInRepository.findAllActive(status, pageable)
                : tradeInRepository.findAllActiveByCreatedBy(status, authContext.getCurrentUsername(), pageable);
        return page.map(this::toDTO);
    }

    @Override
    public TradeInDTO cancel(Long id, String reason) {
        TradeInEntity entity = tradeInRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(ResourceNotFoundException::new);
        if (entity.getStatus() == TradeInStatus.CANCELLED) {
            throw new BadRequestException(messageService.getMessage("error.trade_in.alreadyCancelled"));
        }
        entity.setStatus(TradeInStatus.CANCELLED);
        entity.setCanceledReason(reason);
        entity.setUpdatedAt(LocalDateTime.now());
        // Pull the resale unit out of stock (DAMAGED keeps it out of sellable inventory).
        if (entity.getResaleUnitId() != null) {
            vehicleUnitRepository.findByIdAndDeletedFalse(entity.getResaleUnitId()).ifPresent(u -> {
                if (u.getStatus() != VehicleUnitStatus.SOLD) {
                    u.setStatus(VehicleUnitStatus.DAMAGED);
                    u.setUpdatedAt(LocalDateTime.now());
                    vehicleUnitRepository.save(u);
                }
            });
        }
        tradeInRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.TRADE_IN_CANCELLED, "TRADE_IN", String.valueOf(id),
                "Hủy phiếu thu xe #" + id, null);
        return toDTO(entity);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private Product createResaleProduct(String tid, CreateTradeInRequest request, String number) {
        String typeCode = StringUtils.defaultIfBlank(request.getVehicleType(), "MOTORBIKE");
        ProductType type = productTypeRepository.findByCode(typeCode)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.trade_in.vehicleTypeUnseeded", new Object[]{typeCode})));
        String name = (StringUtils.defaultString(request.getBrand()) + " "
                + StringUtils.defaultString(request.getModel())).trim();
        if (name.isEmpty()) name = "Xe cũ thu vào";
        BigDecimal resalePrice = request.getResalePrice() != null ? request.getResalePrice() : request.getTradeValue();
        Product product = Product.builder()
                .tenantId(tid)
                .productType(type)
                .sku("XECU-" + number)
                .name(name + " (đã qua sử dụng)")
                .description("Xe cũ thu vào — phiếu " + number)
                .price(resalePrice)
                .costPrice(request.getTradeValue())
                .unit("piece")
                .inventoryMode(InventoryMode.UNIQUE)
                .status(Product.ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
    }

    private TradeInDTO toDTO(TradeInEntity e) {
        return TradeInDTO.builder()
                .id(e.getId())
                .tradeInNumber(e.getTradeInNumber())
                .sellerId(e.getSellerId())
                .sellerName(e.getSellerName())
                .sellerPhone(e.getSellerPhone())
                .sellerIdNumber(e.getSellerIdNumber())
                .vehicleType(e.getVehicleType())
                .brand(e.getBrand())
                .model(e.getModel())
                .year(e.getYear())
                .frameNo(e.getFrameNo())
                .engineNo(e.getEngineNo())
                .licensePlate(e.getLicensePlate())
                .color(e.getColor())
                .odometerKm(e.getOdometerKm())
                .conditionNotes(e.getConditionNotes())
                .tradeValue(e.getTradeValue())
                .mode(e.getMode())
                .newSaleOrderId(e.getNewSaleOrderId())
                .newPrice(e.getNewPrice())
                .netAmount(e.getNetAmount())
                .resaleProductId(e.getResaleProductId())
                .resaleUnitId(e.getResaleUnitId())
                .status(e.getStatus())
                .canceledReason(e.getCanceledReason())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
