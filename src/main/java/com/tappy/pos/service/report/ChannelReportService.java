package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.ChannelRevenueDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelReportService {

    private static final int MAX_WINDOW_DAYS = 365;

    private final OrderRepository orderRepository;

    /** Revenue + order count per fulfilment channel over a trailing {@code days} window. */
    public List<ChannelRevenueDTO> getRevenueByChannel(int days) {
        int window = Math.min(Math.max(days, 1), MAX_WINDOW_DAYS);
        LocalDateTime since = LocalDateTime.now().minusDays(window);

        // Seed all channels at zero so the report always shows the full set in a stable order.
        Map<String, ChannelRevenueDTO> byChannel = new LinkedHashMap<>();
        for (Order.OrderChannel c : Order.OrderChannel.values()) {
            byChannel.put(c.name(), ChannelRevenueDTO.builder()
                    .channel(c.name()).orderCount(0).revenue(BigDecimal.ZERO).build());
        }

        for (Object[] row : orderRepository.revenueByChannelSince(since)) {
            if (row[0] == null) continue;
            String channel = row[0].toString();
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            BigDecimal revenue = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            byChannel.put(channel, ChannelRevenueDTO.builder()
                    .channel(channel).orderCount(count).revenue(revenue).build());
        }

        return new ArrayList<>(byChannel.values());
    }
}
