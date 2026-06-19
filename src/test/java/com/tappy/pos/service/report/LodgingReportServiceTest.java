package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.LodgingReportDTO;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.repository.room.RoomRepository;
import com.tappy.pos.repository.room.RoomStayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LodgingReportService Unit Tests")
class LodgingReportServiceTest {

    @Mock private RoomStayRepository roomStayRepository;
    @Mock private RoomRepository roomRepository;

    @InjectMocks private LodgingReportService service;

    private RoomStayEntity stay(LocalDateTime in, LocalDateTime out, String charge) {
        RoomStayEntity s = new RoomStayEntity();
        s.setCheckinAt(in);
        s.setCheckoutAt(out);
        s.setRoomCharge(new BigDecimal(charge));
        return s;
    }

    @Test
    @DisplayName("getLodgingReport: occupancy, ADR, RevPAR and LOS over the window")
    void getLodgingReport_computes() {
        when(roomRepository.countByDeletedFalse()).thenReturn(10L);
        LocalDateTime base = LocalDateTime.now().minusDays(5);
        // Two stays: 2 nights @ 1,000,000 total, and 3 nights @ 1,500,000 total → 5 room-nights, 2,500,000 revenue
        when(roomStayRepository.findCheckedOutBetween(any(), any())).thenReturn(List.of(
                stay(base, base.plusDays(2), "1000000"),
                stay(base, base.plusDays(3), "1500000")));

        LodgingReportDTO dto = service.getLodgingReport(30);

        assertThat(dto.getRoomCount()).isEqualTo(10L);
        assertThat(dto.getStays()).isEqualTo(2L);
        assertThat(dto.getRoomNightsSold()).isEqualTo(5L);
        assertThat(dto.getRoomRevenue()).isEqualByComparingTo("2500000");
        // ADR = 2,500,000 / 5 = 500,000
        assertThat(dto.getAdr()).isEqualByComparingTo("500000");
        // occupancy = 5 / (10 × 30) = 1.7%
        assertThat(dto.getOccupancyPct()).isEqualTo(1.7d);
        // RevPAR = 2,500,000 / 300 = 8,333
        assertThat(dto.getRevpar()).isEqualByComparingTo("8333");
        // LOS = 5 / 2 = 2.5
        assertThat(dto.getAvgLengthOfStay()).isEqualTo(2.5d);
    }

    @Test
    @DisplayName("getLodgingReport: no stays → zeros, no division error")
    void getLodgingReport_empty() {
        when(roomRepository.countByDeletedFalse()).thenReturn(5L);
        when(roomStayRepository.findCheckedOutBetween(any(), any())).thenReturn(List.of());

        LodgingReportDTO dto = service.getLodgingReport(30);

        assertThat(dto.getOccupancyPct()).isZero();
        assertThat(dto.getAdr()).isEqualByComparingTo("0");
        assertThat(dto.getRevpar()).isEqualByComparingTo("0");
        assertThat(dto.getAvgLengthOfStay()).isZero();
    }
}
