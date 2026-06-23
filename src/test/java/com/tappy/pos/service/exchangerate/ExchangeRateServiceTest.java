package com.tappy.pos.service.exchangerate;

import com.tappy.pos.model.dto.exchangerate.ExchangeRateResponse;
import com.tappy.pos.model.entity.exchangerate.ExchangeRate;
import com.tappy.pos.model.entity.exchangerate.ExchangeRateHistory;
import com.tappy.pos.repository.exchangerate.ExchangeRateHistoryRepository;
import com.tappy.pos.repository.exchangerate.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    private static final String VCB_URL =
            "https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx?b=10";

    @Mock
    private ExchangeRateRepository repository;

    @Mock
    private ExchangeRateHistoryRepository historyRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateService service;

    private String exrate(String code, String buy, String transfer, String sell) {
        return "<Exrate CurrencyCode=\"" + code + "\" CurrencyName=\"x\" Buy=\""
                + buy + "\" Transfer=\"" + transfer + "\" Sell=\"" + sell + "\" />";
    }

    // ─── pollVcb ──────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void pollVcbParsesSupportedRatesAndSavesRowsAndHistory() {
        String xml = "<ExrateList>"
                + exrate("USD", "24,500", "24,600.50", "24,800")
                + exrate("EUR", "26,000", "26,100", "26,300")
                + "</ExrateList>";
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class))).thenReturn(xml);

        service.pollVcb();

        ArgumentCaptor<List<ExchangeRate>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(rowsCaptor.capture());
        List<ExchangeRate> rows = rowsCaptor.getValue();
        assertThat(rows).hasSize(2);
        ExchangeRate usd = rows.get(0);
        assertThat(usd.getCurrencyCode()).isEqualTo("USD");
        assertThat(usd.getSource()).isEqualTo("VCB");
        assertThat(usd.getBuyRate()).isEqualByComparingTo(new BigDecimal("24500"));
        assertThat(usd.getTransferRate()).isEqualByComparingTo(new BigDecimal("24600.50"));
        assertThat(usd.getSellRate()).isEqualByComparingTo(new BigDecimal("24800"));
        assertThat(usd.getFetchedAt()).isNotNull();

        verify(historyRepository).saveAll(any(List.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pollVcbSkipsUnsupportedCurrencies() {
        String xml = "<ExrateList>"
                + exrate("USD", "24,500", "24,600", "24,800")
                + exrate("XYZ", "1", "1", "1")
                + "</ExrateList>";
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class))).thenReturn(xml);

        service.pollVcb();

        ArgumentCaptor<List<ExchangeRate>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(1);
        assertThat(rowsCaptor.getValue().get(0).getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pollVcbHandlesUnparseableRateAsNull() {
        String xml = "<ExrateList>" + exrate("USD", "-", "24,600", "abc") + "</ExrateList>";
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class))).thenReturn(xml);

        service.pollVcb();

        ArgumentCaptor<List<ExchangeRate>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(rowsCaptor.capture());
        ExchangeRate usd = rowsCaptor.getValue().get(0);
        assertThat(usd.getBuyRate()).isNull();
        assertThat(usd.getTransferRate()).isEqualByComparingTo(new BigDecimal("24600"));
        assertThat(usd.getSellRate()).isNull();
    }

    @Test
    void pollVcbReturnsEarlyWhenResponseNull() {
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class))).thenReturn(null);

        service.pollVcb();

        verify(repository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void pollVcbReturnsEarlyWhenResponseBlank() {
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class))).thenReturn("   ");

        service.pollVcb();

        verify(repository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void pollVcbReturnsEarlyWhenNoRatesParsed() {
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class)))
                .thenReturn("<ExrateList></ExrateList>");

        service.pollVcb();

        verify(repository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void pollVcbSwallowsRestClientException() {
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class)))
                .thenThrow(new RestClientException("VCB down"));

        assertThatCode(() -> service.pollVcb()).doesNotThrowAnyException();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void pollVcbSwallowsExceptionFromRepository() {
        String xml = "<ExrateList>" + exrate("USD", "24,500", "24,600", "24,800") + "</ExrateList>";
        when(restTemplate.getForObject(eq(VCB_URL), eq(String.class))).thenReturn(xml);
        when(repository.saveAll(any())).thenThrow(new RuntimeException("db error"));

        assertThatCode(() -> service.pollVcb()).doesNotThrowAnyException();
        verify(historyRepository, never()).saveAll(any());
    }

    // ─── getLatest ────────────────────────────────────────────────────────────

    @Test
    void getLatestReturnsRowsAndMaxFetchedAt() {
        LocalDateTime older = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 6, 23, 9, 0);
        ExchangeRate usd = ExchangeRate.builder().currencyCode("USD").source("VCB")
                .buyRate(new BigDecimal("24500")).transferRate(new BigDecimal("24600"))
                .sellRate(new BigDecimal("24800")).fetchedAt(older).build();
        ExchangeRate eur = ExchangeRate.builder().currencyCode("EUR").source("VCB")
                .buyRate(new BigDecimal("26000")).transferRate(new BigDecimal("26100"))
                .sellRate(new BigDecimal("26300")).fetchedAt(newer).build();
        when(repository.findAllBySource("VCB")).thenReturn(List.of(usd, eur));

        ExchangeRateResponse response = service.getLatest();

        assertThat(response.source()).isEqualTo("VCB");
        assertThat(response.fetchedAt()).isEqualTo(newer);
        assertThat(response.rates()).hasSize(2);
        ExchangeRateResponse.RateItem firstItem = response.rates().get(0);
        assertThat(firstItem.currencyCode()).isEqualTo("USD");
        assertThat(firstItem.buyRate()).isEqualByComparingTo(new BigDecimal("24500"));
        assertThat(firstItem.sellRate()).isEqualByComparingTo(new BigDecimal("24800"));
        assertThat(firstItem.fetchedAt()).isEqualTo(older);
    }

    @Test
    void getLatestReturnsEmptyWhenNoRows() {
        when(repository.findAllBySource("VCB")).thenReturn(List.of());

        ExchangeRateResponse response = service.getLatest();

        assertThat(response.source()).isEqualTo("VCB");
        assertThat(response.fetchedAt()).isNull();
        assertThat(response.rates()).isEmpty();
    }

    // ─── getHistory ───────────────────────────────────────────────────────────

    @Test
    void getHistoryMapsRowsToRateItems() {
        LocalDateTime ts = LocalDateTime.of(2026, 6, 20, 8, 0);
        ExchangeRateHistory h = ExchangeRateHistory.builder().id(1L).currencyCode("USD").source("VCB")
                .buyRate(new BigDecimal("24500")).transferRate(new BigDecimal("24600"))
                .sellRate(new BigDecimal("24800")).fetchedAt(ts).build();
        when(historyRepository.findHistory(eq("VCB"), eq("USD"), any(LocalDateTime.class)))
                .thenReturn(List.of(h));

        List<ExchangeRateResponse.RateItem> items = service.getHistory("USD", 30);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).currencyCode()).isEqualTo("USD");
        assertThat(items.get(0).buyRate()).isEqualByComparingTo(new BigDecimal("24500"));
        assertThat(items.get(0).transferRate()).isEqualByComparingTo(new BigDecimal("24600"));
        assertThat(items.get(0).sellRate()).isEqualByComparingTo(new BigDecimal("24800"));
        assertThat(items.get(0).fetchedAt()).isEqualTo(ts);
    }

    @Test
    void getHistoryReturnsEmptyWhenNoRows() {
        when(historyRepository.findHistory(eq("VCB"), anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertThat(service.getHistory("EUR", 7)).isEmpty();
    }

    @Test
    void getHistoryPassesSinceWindowBasedOnDays() {
        when(historyRepository.findHistory(eq("VCB"), eq("USD"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusDays(10);

        service.getHistory("USD", 10);

        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyRepository).findHistory(eq("VCB"), eq("USD"), sinceCaptor.capture());
        LocalDateTime after = LocalDateTime.now().minusDays(10);
        assertThat(sinceCaptor.getValue()).isBetween(before.minusMinutes(1), after.plusMinutes(1));
    }

    // ─── pruneHistory ─────────────────────────────────────────────────────────

    @Test
    void pruneHistoryDeletesRowsOlderThanNinetyDays() {
        when(historyRepository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(5);

        service.pruneHistory();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyRepository, times(1)).deleteOlderThan(cutoffCaptor.capture());
        LocalDateTime expected = LocalDateTime.now().minusDays(90);
        assertThat(cutoffCaptor.getValue()).isBetween(expected.minusMinutes(1), expected.plusMinutes(1));
    }
}
