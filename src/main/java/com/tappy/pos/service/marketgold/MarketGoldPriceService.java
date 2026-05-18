package com.tappy.pos.service.marketgold;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.model.dto.marketgold.MarketGoldPriceResponse;
import com.tappy.pos.model.entity.marketgold.MarketGoldPrice;
import com.tappy.pos.model.entity.marketgold.MarketGoldPriceHistory;
import com.tappy.pos.repository.marketgold.MarketGoldPriceHistoryRepository;
import com.tappy.pos.repository.marketgold.MarketGoldPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketGoldPriceService {

    // ── Source identifiers ────────────────────────────────────────────────────
    private static final String SJC    = "SJC";
    private static final String MIHONG = "MIHONG";
    private static final String PNJ    = "PNJ";
    private static final String BTMC   = "BTMC";

    // ── Source URLs ───────────────────────────────────────────────────────────
    private static final String SJC_URL    = "https://sjc.com.vn/GoldApi/json/GetGoldPrice";
    private static final String MIHONG_URL = "https://api.mihong.vn/v1/gold-prices?market=domestic";
    private static final String PNJ_URL    = "https://edge-api.pnj.io/ecom-frontend/v1/get-gold-price?zone=00";
    private static final String BTMC_URL   = "https://www.btmc.vn/api/BTMCAPI/getpricebtmc?Key=3kd8ub1llcg9t45hnoh8hmn7t5kc2v";

    private static final Map<String, String> MIHONG_NAME_MAP = Map.of(
            "SJC", "Vàng miếng SJC",
            "999", "Vàng 999.9 (24K)",
            "985", "Vàng 985 (23.6K)",
            "980", "Vàng 980 (23.5K)",
            "950", "Vàng 950 (22.8K)",
            "750", "Vàng 750 (18K)",
            "680", "Vàng 680 (16.3K)",
            "610", "Vàng 610 (14.6K)",
            "580", "Vàng 580 (13.9K)",
            "410", "Vàng 410 (9.8K)"
    );

    private final MarketGoldPriceRepository repository;
    private final MarketGoldPriceHistoryRepository historyRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ── Scheduled pollers (staggered initialDelay to avoid simultaneous network hits) ──

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 0)
    @Transactional
    public void pollSjc() {
        poll(SJC, SJC_URL, this::parseSjc);
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 5_000)
    @Transactional
    public void pollMihong() {
        poll(MIHONG, MIHONG_URL, this::parseMihong);
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 10_000)
    @Transactional
    public void pollPnj() {
        poll(PNJ, PNJ_URL, this::parsePnj);
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 15_000)
    @Transactional
    public void pollBtmc() {
        poll(BTMC, BTMC_URL, this::parseBtmc);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MarketGoldPriceResponse getLatest(String source) {
        List<MarketGoldPrice> rows = source != null
                ? repository.findAllBySourceOrderByKtypeAsc(source)
                : repository.findAllByOrderBySourceAscKtypeAsc();

        LocalDateTime fetchedAt = rows.stream()
                .map(MarketGoldPrice::getFetchedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<MarketGoldPriceResponse.PriceItem> items = rows.stream()
                .map(r -> new MarketGoldPriceResponse.PriceItem(
                        r.getKtype(), r.getName(), r.getSource(),
                        r.getBuyPrice(), r.getSellPrice(), r.getFetchedAt()))
                .toList();

        return new MarketGoldPriceResponse(source != null ? source : "ALL", fetchedAt, items);
    }

    @Transactional(readOnly = true)
    public List<MarketGoldPriceResponse.PriceItem> getHistory(String source, String ktype, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return historyRepository.findHistory(source, ktype, since).stream()
                .map(h -> new MarketGoldPriceResponse.PriceItem(
                        h.getKtype(), h.getName(), h.getSource(),
                        h.getBuyPrice(), h.getSellPrice(), h.getFetchedAt()))
                .toList();
    }

    @Scheduled(cron = "0 0 4 * * SUN")
    @Transactional
    public void pruneHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = historyRepository.deleteOlderThan(cutoff);
        log.info("MarketGoldPriceService: pruned {} history rows older than 90 days", deleted);
    }

    // ── Core poll logic ───────────────────────────────────────────────────────

    private static final Map<String, String> SOURCE_REFERER = Map.of(
            SJC,    "https://sjc.com.vn/",
            MIHONG, "https://mihong.vn/",
            PNJ,    "https://www.pnj.com.vn/",
            BTMC,   "https://www.btmc.vn/"
    );

    private void poll(String source, String url, Function<String, List<RawItem>> parser) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            headers.set("Referer", SOURCE_REFERER.getOrDefault(source, "https://google.com/"));
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");

            String body = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
            if (body == null || body.isBlank()) {
                log.warn("MarketGoldPriceService [{}]: empty response", source);
                return;
            }

            List<RawItem> items = parser.apply(body);
            if (items.isEmpty()) {
                log.warn("MarketGoldPriceService [{}]: no items parsed", source);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            List<MarketGoldPrice> latest   = new ArrayList<>();
            List<MarketGoldPriceHistory> history = new ArrayList<>();

            for (RawItem item : items) {
                latest.add(MarketGoldPrice.builder()
                        .ktype(item.ktype()).source(source).name(item.name())
                        .buyPrice(item.buy()).sellPrice(item.sell()).fetchedAt(now)
                        .build());
                history.add(MarketGoldPriceHistory.builder()
                        .ktype(item.ktype()).source(source).name(item.name())
                        .buyPrice(item.buy()).sellPrice(item.sell()).fetchedAt(now)
                        .build());
            }

            repository.saveAll(latest);
            historyRepository.saveAll(history);
            log.info("MarketGoldPriceService [{}]: upserted {} prices", source, latest.size());

        } catch (Exception e) {
            log.error("MarketGoldPriceService [{}]: poll failed", source, e);
        }
    }

    // ── Source-specific parsers ───────────────────────────────────────────────

    /** SJC JSON: [{@ktype, name, buy, sell}] — prices in Vietnamese dot-format "119.000.000" */
    private List<RawItem> parseSjc(String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<List<SjcItem>>() {}).stream()
                    .filter(i -> i.ktype() != null && !i.ktype().isBlank())
                    .map(i -> new RawItem(
                            i.ktype().trim(),
                            i.name() != null ? i.name().trim() : i.ktype().trim(),
                            parseVietnamesePrice(i.buy()),
                            parseVietnamesePrice(i.sell())))
                    .toList();
        } catch (Exception e) {
            log.error("MarketGoldPriceService [SJC]: parse failed", e);
            return List.of();
        }
    }

    /** Mi Hong JSON: [{code, buyingPrice, sellingPrice}] — prices in full VND */
    private List<RawItem> parseMihong(String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<List<MihongItem>>() {}).stream()
                    .filter(i -> i.code() != null && !i.code().isBlank())
                    .map(i -> {
                        String code = i.code().trim();
                        String name = MIHONG_NAME_MAP.getOrDefault(code, "Vàng " + code);
                        BigDecimal buy  = (i.buyingPrice()  != null && i.buyingPrice()  > 0) ? new BigDecimal(i.buyingPrice())  : null;
                        BigDecimal sell = (i.sellingPrice() != null && i.sellingPrice() > 0) ? new BigDecimal(i.sellingPrice()) : null;
                        return new RawItem(code, name, buy, sell);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("MarketGoldPriceService [MIHONG]: parse failed", e);
            return List.of();
        }
    }

    /** PNJ JSON: {data:[{masp, tensp, giaban, giamua}]} — prices in thousands VND (×1000) */
    private List<RawItem> parsePnj(String body) {
        try {
            PnjResponse response = objectMapper.readValue(body, PnjResponse.class);
            if (response.data() == null) return List.of();
            return response.data().stream()
                    .filter(i -> i.masp() != null && !i.masp().isBlank())
                    .map(i -> new RawItem(
                            i.masp().trim(),
                            i.tensp() != null ? i.tensp().trim() : i.masp().trim(),
                            i.giamua() != null ? BigDecimal.valueOf(i.giamua()).multiply(BigDecimal.valueOf(1000)) : null,
                            i.giaban() != null ? BigDecimal.valueOf(i.giaban()).multiply(BigDecimal.valueOf(1000)) : null))
                    .toList();
        } catch (Exception e) {
            log.error("MarketGoldPriceService [PNJ]: parse failed", e);
            return List.of();
        }
    }

    /** BTMC JSON: {DataList:{Data:[{"@n_1":"name","@pb":"buyPrice","@ps":"sellPrice"}]}} — prices in full VND */
    private List<RawItem> parseBtmc(String body) {
        try {
            BtmcResponse response = objectMapper.readValue(body, BtmcResponse.class);
            if (response.dataList() == null || response.dataList().data() == null) return List.of();
            return response.dataList().data().stream()
                    .filter(i -> i.name() != null && !i.name().isBlank())
                    .map(i -> new RawItem(
                            i.name().trim(),
                            i.name().trim(),
                            parseFullVndPrice(i.buyPrice()),
                            parseFullVndPrice(i.sellPrice())))
                    .filter(r -> r.buy() != null || r.sell() != null)
                    .toList();
        } catch (Exception e) {
            log.error("MarketGoldPriceService [BTMC]: parse failed", e);
            return List.of();
        }
    }

    // ── Price parsers ─────────────────────────────────────────────────────────

    /** "119.000.000" → 119000000 (Vietnamese dot = thousands separator) */
    private BigDecimal parseVietnamesePrice(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("0")) return null;
        try {
            return new BigDecimal(raw.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.debug("MarketGoldPriceService: cannot parse price '{}'", raw);
            return null;
        }
    }

    /** BTMC prices: strip all non-digits, treat as full VND integer */
    private BigDecimal parseFullVndPrice(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.isBlank() || digits.equals("0")) return null;
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            log.debug("MarketGoldPriceService: cannot parse BTMC price '{}'", raw);
            return null;
        }
    }

    // ── Internal DTOs for JSON deserialization ────────────────────────────────

    private record RawItem(String ktype, String name, BigDecimal buy, BigDecimal sell) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SjcItem(
            @JsonProperty("@ktype") String ktype,
            @JsonProperty("name")   String name,
            @JsonProperty("buy")    String buy,
            @JsonProperty("sell")   String sell
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MihongItem(
            @JsonProperty("code")          String code,
            @JsonProperty("buyingPrice")   Long   buyingPrice,
            @JsonProperty("sellingPrice")  Long   sellingPrice
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PnjResponse(@JsonProperty("data") List<PnjItem> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PnjItem(
            @JsonProperty("masp")   String masp,
            @JsonProperty("tensp")  String tensp,
            @JsonProperty("giaban") Long   giaban,
            @JsonProperty("giamua") Long   giamua
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BtmcResponse(@JsonProperty("DataList") BtmcDataList dataList) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BtmcDataList(@JsonProperty("Data") List<BtmcItem> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BtmcItem(
            @JsonProperty("@n_1") String name,
            @JsonProperty("@pb")  String buyPrice,
            @JsonProperty("@ps")  String sellPrice
    ) {}
}
