package com.tappy.pos.service.exchangerate;

import com.tappy.pos.model.dto.exchangerate.ExchangeRateResponse;
import com.tappy.pos.model.entity.exchangerate.ExchangeRate;
import com.tappy.pos.model.entity.exchangerate.ExchangeRateHistory;
import com.tappy.pos.repository.exchangerate.ExchangeRateHistoryRepository;
import com.tappy.pos.repository.exchangerate.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String SOURCE = "VCB";
    private static final String VCB_URL =
            "https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx?b=10";
    private static final List<String> SUPPORTED =
            List.of("USD", "EUR", "JPY", "KRW", "SGD", "THB", "CNY", "GBP", "AUD");

    private static final Pattern RATE_PATTERN = Pattern.compile(
            "<Exrate[^>]*CurrencyCode=\"([A-Z]{3})\"[^>]*Buy=\"([^\"]*?)\"[^>]*Transfer=\"([^\"]*?)\"[^>]*Sell=\"([^\"]*?)\"",
            Pattern.CASE_INSENSITIVE
    );

    private final ExchangeRateRepository repository;
    private final ExchangeRateHistoryRepository historyRepository;
    private final RestTemplate restTemplate;

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 0)
    @Transactional
    public void pollVcb() {
        try {
            String xml = restTemplate.getForObject(VCB_URL, String.class);
            if (xml == null || xml.isBlank()) {
                log.warn("ExchangeRateService: empty response from VCB");
                return;
            }
            List<ExchangeRate> rows = parse(xml);
            if (rows.isEmpty()) {
                log.warn("ExchangeRateService: no rates parsed from VCB response");
                return;
            }
            repository.saveAll(rows);
            historyRepository.saveAll(rows.stream().map(r -> ExchangeRateHistory.builder()
                    .currencyCode(r.getCurrencyCode())
                    .source(r.getSource())
                    .buyRate(r.getBuyRate())
                    .transferRate(r.getTransferRate())
                    .sellRate(r.getSellRate())
                    .fetchedAt(r.getFetchedAt())
                    .build()).toList());
            log.info("ExchangeRateService: upserted {} rates and appended history from VCB", rows.size());
        } catch (Exception e) {
            log.error("ExchangeRateService: failed to poll VCB rates", e);
        }
    }

    @Transactional(readOnly = true)
    public ExchangeRateResponse getLatest() {
        List<ExchangeRate> rows = repository.findAllBySource(SOURCE);
        LocalDateTime fetchedAt = rows.stream()
                .map(ExchangeRate::getFetchedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<ExchangeRateResponse.RateItem> items = rows.stream()
                .map(r -> new ExchangeRateResponse.RateItem(
                        r.getCurrencyCode(),
                        r.getBuyRate(),
                        r.getTransferRate(),
                        r.getSellRate(),
                        r.getFetchedAt()
                ))
                .toList();

        return new ExchangeRateResponse(SOURCE, fetchedAt, items);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse.RateItem> getHistory(String currency, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return historyRepository.findHistory(SOURCE, currency, since).stream()
                .map(h -> new ExchangeRateResponse.RateItem(
                        h.getCurrencyCode(),
                        h.getBuyRate(),
                        h.getTransferRate(),
                        h.getSellRate(),
                        h.getFetchedAt()
                ))
                .toList();
    }

    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void pruneHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = historyRepository.deleteOlderThan(cutoff);
        log.info("ExchangeRateService: pruned {} history rows older than 90 days", deleted);
    }

    private List<ExchangeRate> parse(String xml) {
        Matcher matcher = RATE_PATTERN.matcher(xml);
        List<ExchangeRate> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        while (matcher.find()) {
            String code = matcher.group(1).toUpperCase();
            if (!SUPPORTED.contains(code)) continue;

            result.add(ExchangeRate.builder()
                    .currencyCode(code)
                    .source(SOURCE)
                    .buyRate(parseRate(matcher.group(2)))
                    .transferRate(parseRate(matcher.group(3)))
                    .sellRate(parseRate(matcher.group(4)))
                    .fetchedAt(now)
                    .build());
        }
        return result;
    }

    private BigDecimal parseRate(String raw) {
        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            log.debug("ExchangeRateService: could not parse rate '{}'", raw);
            return null;
        }
    }
}
