package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.FashionReportDTO;
import com.tappy.pos.model.dto.report.FashionReportDTO.VariantStat;
import com.tappy.pos.model.entity.product.ProductVariant;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FashionReportServiceImpl implements FashionReportService {

    private static final int BEST_SELLERS_LIMIT = 10;
    private static final int DEAD_STOCK_LIMIT = 20;
    private static final int MAX_WINDOW_DAYS = 365;

    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public FashionReportDTO getFashionReport(int days) {
        int window = Math.min(Math.max(days, 1), MAX_WINDOW_DAYS);
        LocalDateTime since = LocalDateTime.now().minusDays(window);

        Map<Long, Long> soldByVariant = toLongMap(orderItemRepository.sumQtySoldByVariantSince(since));
        Map<Long, Long> onHandByVariant = toLongMap(inventoryRepository.findOnHandByVariant());

        List<ProductVariant> variants = productVariantRepository.findAllActiveWithProduct();

        List<VariantStat> stats = variants.stream().map(v -> {
            long sold = soldByVariant.getOrDefault(v.getId(), 0L);
            long onHand = onHandByVariant.getOrDefault(v.getId(), 0L);
            return VariantStat.builder()
                    .variantId(v.getId())
                    .productId(v.getProduct().getId())
                    .productName(v.getProduct().getName())
                    .sku(v.getSku())
                    .options(v.getVariantOptions())
                    .sold(sold)
                    .onHand(onHand)
                    .build();
        }).collect(Collectors.toList());

        List<VariantStat> bestSellers = stats.stream()
                .filter(s -> s.getSold() > 0)
                .sorted(Comparator.comparingLong(VariantStat::getSold).reversed())
                .limit(BEST_SELLERS_LIMIT)
                .collect(Collectors.toList());

        List<VariantStat> deadStock = stats.stream()
                .filter(s -> s.getOnHand() > 0 && s.getSold() == 0)
                .sorted(Comparator.comparingLong(VariantStat::getOnHand).reversed())
                .limit(DEAD_STOCK_LIMIT)
                .collect(Collectors.toList());

        long totalSold = stats.stream().mapToLong(VariantStat::getSold).sum();
        long totalOnHand = stats.stream().mapToLong(VariantStat::getOnHand).sum();
        double sellThrough = (totalSold + totalOnHand) == 0
                ? 0d
                : Math.round((totalSold * 1000d) / (totalSold + totalOnHand)) / 10d;

        return FashionReportDTO.builder()
                .windowDays(window)
                .totalSold(totalSold)
                .totalOnHand(totalOnHand)
                .sellThroughPct(sellThrough)
                .bestSellers(bestSellers)
                .deadStock(deadStock)
                .build();
    }

    private Map<Long, Long> toLongMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            Long key = ((Number) row[0]).longValue();
            Long val = row[1] == null ? 0L : ((Number) row[1]).longValue();
            map.merge(key, val, Long::sum);
        }
        return map;
    }
}
