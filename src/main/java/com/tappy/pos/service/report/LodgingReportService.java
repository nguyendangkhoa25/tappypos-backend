package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.LodgingReportDTO;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.repository.room.RoomRepository;
import com.tappy.pos.repository.room.RoomStayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LodgingReportService {

    private static final int MAX_WINDOW_DAYS = 365;

    private final RoomStayRepository roomStayRepository;
    private final RoomRepository roomRepository;

    public LodgingReportDTO getLodgingReport(int days) {
        int window = Math.min(Math.max(days, 1), MAX_WINDOW_DAYS);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(window);

        long roomCount = roomRepository.countByDeletedFalse();
        List<RoomStayEntity> stays = roomStayRepository.findCheckedOutBetween(from, to);

        long roomNights = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (RoomStayEntity s : stays) {
            roomNights += nightsOf(s);
            if (s.getRoomCharge() != null) revenue = revenue.add(s.getRoomCharge());
        }

        long availableRoomNights = (long) roomCount * window;
        double occupancy = availableRoomNights == 0 ? 0d
                : Math.round((roomNights * 1000d) / availableRoomNights) / 10d;
        BigDecimal adr = roomNights == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(roomNights), 0, RoundingMode.HALF_UP);
        BigDecimal revpar = availableRoomNights == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(availableRoomNights), 0, RoundingMode.HALF_UP);
        double los = stays.isEmpty() ? 0d
                : Math.round((roomNights * 10d) / stays.size()) / 10d;

        return LodgingReportDTO.builder()
                .windowDays(window)
                .roomCount(roomCount)
                .stays(stays.size())
                .roomNightsSold(roomNights)
                .roomRevenue(revenue)
                .occupancyPct(occupancy)
                .adr(adr)
                .revpar(revpar)
                .avgLengthOfStay(los)
                .build();
    }

    /** Whole nights for a stay (min 1); falls back to {@code units} when timestamps are missing. */
    private long nightsOf(RoomStayEntity s) {
        if (s.getCheckinAt() != null && s.getCheckoutAt() != null) {
            long n = Duration.between(s.getCheckinAt(), s.getCheckoutAt()).toDays();
            return Math.max(1, n);
        }
        return s.getUnits() != null ? Math.max(1, s.getUnits()) : 1;
    }
}
