package com.tappy.pos.controller.utilities;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.exchangerate.ExchangeRateResponse;
import com.tappy.pos.model.dto.marketgold.MarketGoldPriceResponse;
import com.tappy.pos.service.exchangerate.ExchangeRateService;
import com.tappy.pos.service.marketgold.MarketGoldPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/utilities")
@RequiredArgsConstructor
public class UtilitiesController {

    private final ExchangeRateService exchangeRateService;
    private final MarketGoldPriceService marketGoldPriceService;

    @GetMapping("/exchange-rates")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getExchangeRates() {
        log.info("Endpoint: GET /utilities/exchange-rates");
        ExchangeRateResponse data = exchangeRateService.getLatest();
        if (data.rates().isEmpty()) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Exchange rate data not yet available"));
        }
        return ResponseEntity.ok(ApiResponse.success(data, "OK"));
    }

    @GetMapping("/exchange-rates/history")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse.RateItem>>> getExchangeRateHistory(
            @RequestParam String currency,
            @RequestParam(defaultValue = "7") int days) {
        log.info("Endpoint: GET /utilities/exchange-rates/history currency={} days={}", currency, days);
        int safeDays = Math.min(Math.max(days, 1), 90);
        List<ExchangeRateResponse.RateItem> history = exchangeRateService.getHistory(currency, safeDays);
        return ResponseEntity.ok(ApiResponse.success(history, "OK"));
    }

    @GetMapping("/market-gold-prices")
    public ResponseEntity<ApiResponse<MarketGoldPriceResponse>> getMarketGoldPrices(
            @RequestParam(required = false) String source) {
        log.info("Endpoint: GET /utilities/market-gold-prices source={}", source);
        MarketGoldPriceResponse data = marketGoldPriceService.getLatest(source);
        if (data.prices().isEmpty()) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Market gold price data not yet available"));
        }
        return ResponseEntity.ok(ApiResponse.success(data, "OK"));
    }

    @GetMapping("/market-gold-prices/history")
    public ResponseEntity<ApiResponse<List<MarketGoldPriceResponse.PriceItem>>> getMarketGoldPriceHistory(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String ktype,
            @RequestParam(defaultValue = "7") int days) {
        log.info("Endpoint: GET /utilities/market-gold-prices/history source={} ktype={} days={}", source, ktype, days);
        int safeDays = Math.min(Math.max(days, 1), 90);
        List<MarketGoldPriceResponse.PriceItem> history = marketGoldPriceService.getHistory(source, ktype, safeDays);
        return ResponseEntity.ok(ApiResponse.success(history, "OK"));
    }
}
