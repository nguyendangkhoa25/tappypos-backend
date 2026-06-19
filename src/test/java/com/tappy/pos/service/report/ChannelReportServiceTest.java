package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.ChannelRevenueDTO;
import com.tappy.pos.repository.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelReportService Unit Tests")
class ChannelReportServiceTest {

    @Mock private OrderRepository orderRepository;

    @InjectMocks private ChannelReportService service;

    @Test
    @DisplayName("getRevenueByChannel: seeds all channels at zero and fills the queried ones")
    void getRevenueByChannel_seedsAndFills() {
        when(orderRepository.revenueByChannelSince(any())).thenReturn(List.<Object[]>of(
                new Object[]{ com.tappy.pos.model.entity.order.Order.OrderChannel.DINE_IN, 3L, new BigDecimal("300000") },
                new Object[]{ com.tappy.pos.model.entity.order.Order.OrderChannel.DELIVERY, 1L, new BigDecimal("90000") }
        ));

        List<ChannelRevenueDTO> result = service.getRevenueByChannel(30);

        Map<String, ChannelRevenueDTO> byChannel = result.stream()
                .collect(Collectors.toMap(ChannelRevenueDTO::getChannel, r -> r));
        // All three channels always present.
        assertThat(byChannel.keySet()).containsExactlyInAnyOrder("DINE_IN", "TAKEAWAY", "DELIVERY");
        assertThat(byChannel.get("DINE_IN").getOrderCount()).isEqualTo(3L);
        assertThat(byChannel.get("DINE_IN").getRevenue()).isEqualByComparingTo("300000");
        assertThat(byChannel.get("DELIVERY").getOrderCount()).isEqualTo(1L);
        // Unqueried channel stays at zero.
        assertThat(byChannel.get("TAKEAWAY").getOrderCount()).isZero();
        assertThat(byChannel.get("TAKEAWAY").getRevenue()).isEqualByComparingTo("0");
    }
}
