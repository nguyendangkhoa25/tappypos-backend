package com.tappy.pos.service.exchangerate;

import com.tappy.pos.model.dto.exchangerate.ExchangeRateResponse;
import com.tappy.pos.model.entity.exchangerate.ExchangeRate;
import com.tappy.pos.repository.exchangerate.ExchangeRateHistoryRepository;
import com.tappy.pos.repository.exchangerate.ExchangeRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceTest {

    @Mock private ExchangeRateRepository repository;
    @Mock private ExchangeRateHistoryRepository historyRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateService service;

    private ExchangeRate rate(String code) {
        return ExchangeRate.builder()
                .currencyCode(code)
                .source("VCB")
                .buyRate(new BigDecimal("25000"))
                .transferRate(new BigDecimal("25100"))
                .sellRate(new BigDecimal("25300"))
                .fetchedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getLatest: maps stored rates into the response")
    void getLatest() {
        when(repository.findAllBySource("VCB")).thenReturn(List.of(rate("USD"), rate("EUR")));

        ExchangeRateResponse response = service.getLatest();

        assertThat(response.source()).isEqualTo("VCB");
        assertThat(response.rates()).hasSize(2);
        assertThat(response.rates().get(0).currencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("getLatest: no stored rates → empty response")
    void getLatest_empty() {
        when(repository.findAllBySource("VCB")).thenReturn(List.of());

        ExchangeRateResponse response = service.getLatest();

        assertThat(response.rates()).isEmpty();
    }

    @Test
    @DisplayName("getHistory: queries history for the currency window")
    void getHistory() {
        when(historyRepository.findHistory(eq("VCB"), eq("USD"), any())).thenReturn(List.of());

        assertThat(service.getHistory("USD", 30)).isEmpty();
    }

    @Test
    @DisplayName("pruneHistory: deletes rows older than the cutoff")
    void pruneHistory() {
        when(historyRepository.deleteOlderThan(any())).thenReturn(7);

        service.pruneHistory();

        org.mockito.Mockito.verify(historyRepository).deleteOlderThan(any());
    }
}
