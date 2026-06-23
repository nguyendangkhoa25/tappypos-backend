package com.tappy.pos.service.marketgold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.model.dto.marketgold.MarketGoldPriceResponse;
import com.tappy.pos.model.entity.marketgold.MarketGoldPrice;
import com.tappy.pos.model.entity.marketgold.MarketGoldPriceHistory;
import com.tappy.pos.repository.marketgold.MarketGoldPriceHistoryRepository;
import com.tappy.pos.repository.marketgold.MarketGoldPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketGoldPriceService Unit Tests")
class MarketGoldPriceServiceTest {

    @Mock private MarketGoldPriceRepository repository;
    @Mock private MarketGoldPriceHistoryRepository historyRepository;
    @Mock private RestTemplate restTemplate;

    private MarketGoldPriceService service;

    @BeforeEach
    void setUp() {
        service = new MarketGoldPriceService(repository, historyRepository, restTemplate, new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private void stubHttp(String body) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    // ── getLatest ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLatest with a source filters by that source and reports its label")
    void getLatest_withSource() {
        MarketGoldPrice row = MarketGoldPrice.builder()
                .ktype("SJC").source("PNJ").name("Vàng SJC")
                .buyPrice(new BigDecimal("100")).sellPrice(new BigDecimal("102"))
                .fetchedAt(LocalDateTime.now()).build();
        when(repository.findAllBySourceOrderByKtypeAsc("PNJ")).thenReturn(List.of(row));

        MarketGoldPriceResponse resp = service.getLatest("PNJ");

        assertThat(resp.source()).isEqualTo("PNJ");
        assertThat(resp.prices()).hasSize(1);
        assertThat(resp.fetchedAt()).isNotNull();
    }

    @Test
    @DisplayName("getLatest without a source returns ALL and null fetchedAt when empty")
    void getLatest_allEmpty() {
        when(repository.findAllByOrderBySourceAscKtypeAsc()).thenReturn(List.of());

        MarketGoldPriceResponse resp = service.getLatest(null);

        assertThat(resp.source()).isEqualTo("ALL");
        assertThat(resp.prices()).isEmpty();
        assertThat(resp.fetchedAt()).isNull();
    }

    // ── getHistory / pruneHistory ─────────────────────────────────────────────

    @Test
    @DisplayName("getHistory maps history rows to price items")
    void getHistory() {
        MarketGoldPriceHistory h = MarketGoldPriceHistory.builder()
                .ktype("999").source("MIHONG").name("Vàng 999.9 (24K)")
                .buyPrice(new BigDecimal("90")).sellPrice(new BigDecimal("92"))
                .fetchedAt(LocalDateTime.now()).build();
        when(historyRepository.findHistory(eq("MIHONG"), eq("999"), any())).thenReturn(List.of(h));

        List<MarketGoldPriceResponse.PriceItem> result = service.getHistory("MIHONG", "999", 7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ktype()).isEqualTo("999");
    }

    @Test
    @DisplayName("pruneHistory delegates to repository with a 90-day cutoff")
    void pruneHistory() {
        when(historyRepository.deleteOlderThan(any())).thenReturn(5);

        service.pruneHistory();

        verify(historyRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    // ── pollMihong ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("pollMihong parses codes, maps names, and saves latest + history")
    void pollMihong_ok() {
        stubHttp("[{\"code\":\"999\",\"buyingPrice\":90000000,\"sellingPrice\":92000000}," +
                 "{\"code\":\"XYZ\",\"buyingPrice\":0,\"sellingPrice\":null}]");

        service.pollMihong();

        ArgumentCaptor<List<MarketGoldPrice>> cap = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(cap.capture());
        List<MarketGoldPrice> saved = cap.getValue();
        assertThat(saved).hasSize(2);
        MarketGoldPrice known = saved.stream().filter(p -> p.getKtype().equals("999")).findFirst().orElseThrow();
        assertThat(known.getName()).isEqualTo("Vàng 999.9 (24K)");
        assertThat(known.getBuyPrice()).isEqualByComparingTo("90000000");
        MarketGoldPrice unknown = saved.stream().filter(p -> p.getKtype().equals("XYZ")).findFirst().orElseThrow();
        assertThat(unknown.getName()).isEqualTo("Vàng XYZ");
        assertThat(unknown.getBuyPrice()).isNull();   // 0 → null
        assertThat(unknown.getSellPrice()).isNull();  // null stays null
        verify(historyRepository).saveAll(any());
    }

    @Test
    @DisplayName("poll skips persistence on empty HTTP body")
    void poll_emptyBody() {
        stubHttp("   ");
        service.pollMihong();
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("poll skips persistence when parser yields no items")
    void poll_noItemsParsed() {
        stubHttp("[]");
        service.pollMihong();
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("poll swallows network exceptions")
    void poll_networkError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));
        service.pollMihong();
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("poll swallows malformed JSON (parser returns empty)")
    void poll_malformedJson() {
        stubHttp("not json");
        service.pollMihong();
        verify(repository, never()).saveAll(any());
    }

    // ── pollPnj (prices in thousands ×1000) ─────────────────────────────────────

    @Test
    @DisplayName("pollPnj multiplies prices by 1000")
    void pollPnj_ok() {
        stubHttp("{\"data\":[{\"masp\":\"SJC\",\"tensp\":\"Vàng SJC\",\"giaban\":92000,\"giamua\":90000}]}");

        service.pollPnj();

        ArgumentCaptor<List<MarketGoldPrice>> cap = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(cap.capture());
        MarketGoldPrice p = cap.getValue().get(0);
        assertThat(p.getSellPrice()).isEqualByComparingTo("92000000");
        assertThat(p.getBuyPrice()).isEqualByComparingTo("90000000");
        assertThat(p.getSource()).isEqualTo("PNJ");
    }

    @Test
    @DisplayName("pollPnj handles null data array")
    void pollPnj_nullData() {
        stubHttp("{}");
        service.pollPnj();
        verify(repository, never()).saveAll(any());
    }

    // ── pollSjc (Vietnamese dot format) ──────────────────────────────────────────

    @Test
    @DisplayName("pollSjc parses Vietnamese dot-formatted prices")
    void pollSjc_ok() {
        stubHttp("[{\"@ktype\":\"1\",\"name\":\"Vàng miếng\",\"buy\":\"119.000.000\",\"sell\":\"121.000.000\"}," +
                 "{\"@ktype\":\"\",\"name\":\"skip\",\"buy\":\"0\",\"sell\":\"0\"}]");

        service.pollSjc();

        ArgumentCaptor<List<MarketGoldPrice>> cap = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(1);
        MarketGoldPrice p = cap.getValue().get(0);
        assertThat(p.getBuyPrice()).isEqualByComparingTo("119000000");
        assertThat(p.getSellPrice()).isEqualByComparingTo("121000000");
    }

    // ── pollBtmc (full VND, strip non-digits) ────────────────────────────────────

    @Test
    @DisplayName("pollBtmc strips non-digits and drops rows with no price")
    void pollBtmc_ok() {
        stubHttp("{\"DataList\":{\"Data\":[" +
                 "{\"@n_1\":\"Vàng SJC\",\"@pb\":\"119,000,000\",\"@ps\":\"121.000.000\"}," +
                 "{\"@n_1\":\"NoPrice\",\"@pb\":\"\",\"@ps\":\"0\"}]}}");

        service.pollBtmc();

        ArgumentCaptor<List<MarketGoldPrice>> cap = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(1);
        assertThat(cap.getValue().get(0).getBuyPrice()).isEqualByComparingTo("119000000");
    }

    @Test
    @DisplayName("pollBtmc handles null DataList")
    void pollBtmc_nullDataList() {
        stubHttp("{}");
        service.pollBtmc();
        verify(repository, never()).saveAll(any());
    }
}
