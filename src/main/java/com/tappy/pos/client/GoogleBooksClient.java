package com.tappy.pos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

/**
 * Looks up a book by ISBN via the Google Books API. Used as a book-specific
 * layer of the barcode lookup chain (mirrors {@link OpenFoodFactsClient}):
 * an ISBN-13 (978/979 "Bookland" EAN-13) won't be in Open Food Facts, so a
 * nhà sách scanning a book's barcode resolves title/author/publisher here.
 *
 * The basic volume lookup needs no API key; {@code books.api-key} is optional
 * (raises the anonymous quota). Returns empty on disabled/not-found/any error —
 * never throws into the lookup flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleBooksClient {

    private static final String VOLUMES_URL = "https://www.googleapis.com/books/v1/volumes";
    private static final String USER_AGENT = "TappyPOS/1.0 (pos.tappy.vn; support@tappy.vn)";

    private final RestTemplate restTemplate;

    @Value("${books.enabled:true}")
    private boolean enabled;

    @Value("${books.api-key:}")
    private String apiKey;

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Single-book lookup by ISBN. Returns empty if disabled, not found, or on any error.
     */
    public Optional<BookInfo> fetchByIsbn(String isbn) {
        if (!enabled) {
            log.debug("Google Books integration disabled — skipping ISBN lookup for {}", isbn);
            return Optional.empty();
        }
        String cleaned = isbn == null ? "" : isbn.trim();
        if (cleaned.isBlank()) return Optional.empty();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(VOLUMES_URL)
                .queryParam("q", "isbn:" + cleaned)
                .queryParam("maxResults", 1)
                .queryParam("country", "VN");
        if (apiKey != null && !apiKey.isBlank()) {
            builder.queryParam("key", apiKey);
        }

        log.info("Fetching Google Books volume isbn={}", cleaned);
        try {
            ResponseEntity<VolumesResponse> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, new HttpEntity<>(buildHeaders()), VolumesResponse.class);
            VolumesResponse body = response.getBody();
            if (body == null || body.totalItems < 1 || body.items == null || body.items.isEmpty()) {
                log.debug("No Google Books volume found for isbn={}", cleaned);
                return Optional.empty();
            }
            Volume volume = body.items.get(0);
            if (volume == null || volume.volumeInfo == null || volume.volumeInfo.title == null) {
                return Optional.empty();
            }
            return Optional.of(toBookInfo(cleaned, volume.volumeInfo));
        } catch (Exception e) {
            log.warn("Google Books lookup failed for isbn={}: {}", cleaned, e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        return headers;
    }

    private BookInfo toBookInfo(String isbn, VolumeInfo info) {
        BookInfo book = new BookInfo();
        book.isbn = isbn;
        book.title = info.title != null ? info.title.trim() : null;
        book.author = (info.authors != null && !info.authors.isEmpty()) ? info.authors.get(0) : null;
        book.publisher = info.publisher;
        book.category = (info.categories != null && !info.categories.isEmpty()) ? info.categories.get(0) : null;
        book.language = info.language;
        book.imageUrl = info.imageLinks != null ? info.imageLinks.thumbnail : null;
        return book;
    }

    // ─── Public result DTO ────────────────────────────────────────────────────

    /** Flattened book info surfaced to the lookup chain. */
    public static class BookInfo {
        public String isbn;
        public String title;
        public String author;
        public String publisher;
        public String category;
        public String language;
        public String imageUrl;
    }

    // ─── Inner JSON DTOs (Google Books volumes response) ──────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumesResponse {
        @JsonProperty("totalItems")
        public int totalItems;

        @JsonProperty("items")
        public List<Volume> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Volume {
        @JsonProperty("volumeInfo")
        public VolumeInfo volumeInfo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfo {
        @JsonProperty("title")
        public String title;

        @JsonProperty("authors")
        public List<String> authors;

        @JsonProperty("publisher")
        public String publisher;

        @JsonProperty("categories")
        public List<String> categories;

        @JsonProperty("language")
        public String language;

        @JsonProperty("imageLinks")
        public ImageLinks imageLinks;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinks {
        @JsonProperty("thumbnail")
        public String thumbnail;
    }
}
