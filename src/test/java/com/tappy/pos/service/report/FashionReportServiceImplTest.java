package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.FashionReportDTO;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductVariant;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FashionReportService Unit Tests")
class FashionReportServiceImplTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private InventoryRepository inventoryRepository;

    @InjectMocks
    private FashionReportServiceImpl service;

    private Product shirt;

    @BeforeEach
    void setUp() {
        shirt = Product.builder().name("Áo thun").build();
        shirt.setId(1L);
    }

    private ProductVariant variant(long id, String sku, Map<String, String> opts) {
        ProductVariant v = ProductVariant.builder().product(shirt).sku(sku).variantOptions(opts).build();
        v.setId(id);
        return v;
    }

    @Test
    @DisplayName("getFashionReport: classifies best sellers, dead stock and sell-through")
    void getFashionReport_computesStats() {
        ProductVariant small = variant(10L, "AO-S", Map.of("Size", "S"));
        ProductVariant medium = variant(11L, "AO-M", Map.of("Size", "M"));

        when(productVariantRepository.findAllActiveWithProduct()).thenReturn(List.of(small, medium));
        // S sold 5; M sold 0
        when(orderItemRepository.sumQtySoldByVariantSince(any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 5L}));
        // S on-hand 3; M on-hand 10
        when(inventoryRepository.findOnHandByVariant())
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}, new Object[]{11L, 10L}));

        FashionReportDTO dto = service.getFashionReport(30);

        assertThat(dto.getWindowDays()).isEqualTo(30);
        assertThat(dto.getTotalSold()).isEqualTo(5L);
        assertThat(dto.getTotalOnHand()).isEqualTo(13L);
        // 5 / (5 + 13) = 27.8%
        assertThat(dto.getSellThroughPct()).isEqualTo(27.8d);

        assertThat(dto.getBestSellers()).hasSize(1);
        assertThat(dto.getBestSellers().get(0).getSku()).isEqualTo("AO-S");
        assertThat(dto.getBestSellers().get(0).getSold()).isEqualTo(5L);

        assertThat(dto.getDeadStock()).hasSize(1);
        assertThat(dto.getDeadStock().get(0).getSku()).isEqualTo("AO-M");
        assertThat(dto.getDeadStock().get(0).getOnHand()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getFashionReport: no variants → zeroed report, no division by zero")
    void getFashionReport_empty() {
        when(productVariantRepository.findAllActiveWithProduct()).thenReturn(List.of());
        when(orderItemRepository.sumQtySoldByVariantSince(any())).thenReturn(List.of());
        when(inventoryRepository.findOnHandByVariant()).thenReturn(List.of());

        FashionReportDTO dto = service.getFashionReport(30);

        assertThat(dto.getSellThroughPct()).isEqualTo(0d);
        assertThat(dto.getBestSellers()).isEmpty();
        assertThat(dto.getDeadStock()).isEmpty();
    }
}
