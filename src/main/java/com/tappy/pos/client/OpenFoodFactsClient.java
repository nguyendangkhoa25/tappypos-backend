package com.tappy.pos.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenFoodFactsClient {

    private static final String SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl";
    private static final String PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String FIELDS = "code,product_name,brands,categories_tags,quantity,image_front_url";
    private static final String USER_AGENT = "TappyPOS/1.0 (tappypos.vn; contact@tappypos.vn)";

    private final RestTemplate restTemplate;

    @Value("${off.enabled:true}")
    private boolean enabled;

    @Value("${off.user-id:}")
    private String userId;

    @Value("${off.password:}")
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Bulk search — filtered by Vietnam, paginated.
     * Attaches credentials when configured to avoid anonymous rate limits (503s).
     */
    public OffSearchResponse search(int page, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("action", "process")
                .queryParam("countries_tags_en", "vietnam")
                .queryParam("json", "1")
                .queryParam("fields", FIELDS)
                .queryParam("page_size", pageSize)
                .queryParam("page", page);

        if (hasCredentials()) {
            builder.queryParam("user_id", userId).queryParam("password", password);
        }

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        log.info("Fetching OFF page {} (size={}, authenticated={})", page, pageSize, hasCredentials());
        try {
            ResponseEntity<OffSearchResponse> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, OffSearchResponse.class);
            if (response.getBody() == null) {
                log.warn("Null response from OFF for page {}", page);
                return new OffSearchResponse();
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch OFF page {}: {}", page, e.getMessage());
            throw e;
        }
    }

    /**
     * Single-product lookup by barcode — used for real-time layer-3 suggestion.
     * Returns empty if disabled, not found, or on any error.
     */
    public Optional<OffProduct> fetchByBarcode(String barcode) {
        if (!enabled) {
            log.debug("OFF integration disabled — skipping barcode lookup for {}", barcode);
            return Optional.empty();
        }
        String url = PRODUCT_URL + barcode + "?fields=" + FIELDS;
        log.info("Fetching OFF product barcode={}", barcode);
        try {
            ResponseEntity<OffProductResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildHeaders()), OffProductResponse.class);
            OffProductResponse body = response.getBody();
            if (body == null || body.status != 1 || body.product == null) {
                log.debug("Product not found in OFF for barcode={}", barcode);
                return Optional.empty();
            }
            return Optional.of(body.product);
        } catch (Exception e) {
            log.warn("OFF lookup failed for barcode={}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        return headers;
    }

    private boolean hasCredentials() {
        return userId != null && !userId.isBlank() && password != null && !password.isBlank();
    }

    // ─── Inner DTO classes ────────────────────────────────────────────────────

    public static class OffProductResponse {
        @JsonProperty("status")
        public int status; // 1 = found, 0 = not found

        @JsonProperty("product")
        public OffProduct product;
    }

    public static class OffSearchResponse {
        @JsonProperty("count")
        public int count;

        @JsonProperty("page_count")
        public int page_count;

        @JsonProperty("products")
        public List<OffProduct> products;
    }

    public static class OffProduct {
        @JsonProperty("code")
        public String code;

        @JsonProperty("product_name")
        public String product_name;

        @JsonProperty("brands")
        public String brands;

        @JsonProperty("categories_tags")
        public List<String> categories_tags;

        @JsonProperty("quantity")
        public String quantity;

        @JsonProperty("image_front_url")
        public String image_front_url;
    }
}
