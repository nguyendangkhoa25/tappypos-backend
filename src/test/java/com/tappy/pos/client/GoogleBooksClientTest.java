package com.tappy.pos.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleBooksClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GoogleBooksClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "apiKey", "");
    }

    private GoogleBooksClient.VolumesResponse responseWithVolume(GoogleBooksClient.VolumeInfo info) {
        GoogleBooksClient.Volume volume = new GoogleBooksClient.Volume();
        volume.volumeInfo = info;
        GoogleBooksClient.VolumesResponse resp = new GoogleBooksClient.VolumesResponse();
        resp.totalItems = 1;
        resp.items = List.of(volume);
        return resp;
    }

    private GoogleBooksClient.VolumeInfo fullVolumeInfo() {
        GoogleBooksClient.VolumeInfo info = new GoogleBooksClient.VolumeInfo();
        info.title = "  Dế Mèn Phiêu Lưu Ký  ";
        info.authors = List.of("Tô Hoài", "Other");
        info.publisher = "NXB Kim Đồng";
        info.categories = List.of("Fiction", "Children");
        info.language = "vi";
        GoogleBooksClient.ImageLinks links = new GoogleBooksClient.ImageLinks();
        links.thumbnail = "https://example.com/cover.jpg";
        info.imageLinks = links;
        return info;
    }

    @Test
    void isEnabledReflectsFlag() {
        assertThat(client.isEnabled()).isTrue();
        ReflectionTestUtils.setField(client, "enabled", false);
        assertThat(client.isEnabled()).isFalse();
    }

    @Test
    void fetchByIsbnReturnsEmptyWhenDisabled() {
        ReflectionTestUtils.setField(client, "enabled", false);

        Optional<GoogleBooksClient.BookInfo> result = client.fetchByIsbn("9786046992264");

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchByIsbnReturnsEmptyForNullIsbn() {
        assertThat(client.fetchByIsbn(null)).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchByIsbnReturnsEmptyForBlankIsbn() {
        assertThat(client.fetchByIsbn("   ")).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnMapsAllFieldsAndTrimsTitleAndTakesFirstAuthorAndCategory() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(fullVolumeInfo())));

        Optional<GoogleBooksClient.BookInfo> result = client.fetchByIsbn(" 9786046992264 ");

        assertThat(result).isPresent();
        GoogleBooksClient.BookInfo book = result.get();
        assertThat(book.isbn).isEqualTo("9786046992264");
        assertThat(book.title).isEqualTo("Dế Mèn Phiêu Lưu Ký");
        assertThat(book.author).isEqualTo("Tô Hoài");
        assertThat(book.publisher).isEqualTo("NXB Kim Đồng");
        assertThat(book.category).isEqualTo("Fiction");
        assertThat(book.language).isEqualTo("vi");
        assertThat(book.imageUrl).isEqualTo("https://example.com/cover.jpg");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnHandlesNullOptionalSubFields() {
        GoogleBooksClient.VolumeInfo info = new GoogleBooksClient.VolumeInfo();
        info.title = "Minimal Book";
        // authors/categories/imageLinks/publisher/language all null
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(info)));

        Optional<GoogleBooksClient.BookInfo> result = client.fetchByIsbn("123");

        assertThat(result).isPresent();
        GoogleBooksClient.BookInfo book = result.get();
        assertThat(book.title).isEqualTo("Minimal Book");
        assertThat(book.author).isNull();
        assertThat(book.category).isNull();
        assertThat(book.imageUrl).isNull();
        assertThat(book.publisher).isNull();
        assertThat(book.language).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnIncludesApiKeyInUrlWhenConfigured() {
        ReflectionTestUtils.setField(client, "apiKey", "SECRET_KEY");
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(restTemplate.exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(fullVolumeInfo())));

        client.fetchByIsbn("9786046992264");

        assertThat(urlCaptor.getValue()).contains("key=SECRET_KEY");
        assertThat(urlCaptor.getValue()).contains("isbn:9786046992264");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnSetsUserAgentHeader() {
        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), entityCaptor.capture(),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(fullVolumeInfo())));

        client.fetchByIsbn("9786046992264");

        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.USER_AGENT)).contains("TappyPOS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnReturnsEmptyWhenBodyNull() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnReturnsEmptyWhenTotalItemsZero() {
        GoogleBooksClient.VolumesResponse resp = new GoogleBooksClient.VolumesResponse();
        resp.totalItems = 0;
        resp.items = List.of();
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(resp));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnReturnsEmptyWhenItemsNull() {
        GoogleBooksClient.VolumesResponse resp = new GoogleBooksClient.VolumesResponse();
        resp.totalItems = 1;
        resp.items = null;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(resp));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnReturnsEmptyWhenItemsEmpty() {
        GoogleBooksClient.VolumesResponse resp = new GoogleBooksClient.VolumesResponse();
        resp.totalItems = 1;
        resp.items = List.of();
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(resp));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnReturnsEmptyWhenVolumeInfoNull() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(null)));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnReturnsEmptyWhenTitleNull() {
        GoogleBooksClient.VolumeInfo info = new GoogleBooksClient.VolumeInfo();
        info.title = null;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(info)));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnSwallowsRestClientException() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenThrow(new RestClientException("network down"));

        assertThat(client.fetchByIsbn("123")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchByIsbnHandlesEmptyAuthorsAndCategoriesLists() {
        GoogleBooksClient.VolumeInfo info = new GoogleBooksClient.VolumeInfo();
        info.title = "Book";
        info.authors = List.of();
        info.categories = List.of();
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(GoogleBooksClient.VolumesResponse.class)))
                .thenReturn(ResponseEntity.ok(responseWithVolume(info)));

        Optional<GoogleBooksClient.BookInfo> result = client.fetchByIsbn("123");

        assertThat(result).isPresent();
        assertThat(result.get().author).isNull();
        assertThat(result.get().category).isNull();
    }
}
