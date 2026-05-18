package com.tappy.pos.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenFoodFactsClient Unit Tests")
class OpenFoodFactsClientTest {

    @Mock private RestTemplate restTemplate;

    private OpenFoodFactsClient client;

    @BeforeEach
    void setUp() {
        client = new OpenFoodFactsClient(restTemplate);
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "userId", "");
        ReflectionTestUtils.setField(client, "password", "");
    }

    @Test
    @DisplayName("isEnabled: returns configured value")
    void isEnabled_returnsTrue() {
        assertThat(client.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("fetchByBarcode: returns empty when disabled")
    void fetchByBarcode_disabled() {
        ReflectionTestUtils.setField(client, "enabled", false);

        Optional<OpenFoodFactsClient.OffProduct> result = client.fetchByBarcode("4006381333931");

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("fetchByBarcode: returns product when found")
    void fetchByBarcode_found() {
        OpenFoodFactsClient.OffProduct product = new OpenFoodFactsClient.OffProduct();
        product.code = "4006381333931";
        product.product_name = "Coca-Cola";
        product.brands = "Coca-Cola";

        OpenFoodFactsClient.OffProductResponse body = new OpenFoodFactsClient.OffProductResponse();
        body.status = 1;
        body.product = product;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffProductResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        Optional<OpenFoodFactsClient.OffProduct> result = client.fetchByBarcode("4006381333931");

        assertThat(result).isPresent();
        assertThat(result.get().product_name).isEqualTo("Coca-Cola");
    }

    @Test
    @DisplayName("fetchByBarcode: returns empty when status is 0 (not found)")
    void fetchByBarcode_notFound() {
        OpenFoodFactsClient.OffProductResponse body = new OpenFoodFactsClient.OffProductResponse();
        body.status = 0;
        body.product = null;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffProductResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        Optional<OpenFoodFactsClient.OffProduct> result = client.fetchByBarcode("0000000000000");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchByBarcode: returns empty when null body returned")
    void fetchByBarcode_nullBody() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffProductResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        Optional<OpenFoodFactsClient.OffProduct> result = client.fetchByBarcode("1234567890123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchByBarcode: returns empty on RestTemplate exception")
    void fetchByBarcode_exception() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffProductResponse.class)))
                .thenThrow(new RuntimeException("network error"));

        Optional<OpenFoodFactsClient.OffProduct> result = client.fetchByBarcode("4006381333931");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("search: returns response with products list")
    void search_success() {
        OpenFoodFactsClient.OffProduct product = new OpenFoodFactsClient.OffProduct();
        product.product_name = "Bánh mì";

        OpenFoodFactsClient.OffSearchResponse body = new OpenFoodFactsClient.OffSearchResponse();
        body.count = 1;
        body.page_count = 1;
        body.products = List.of(product);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        OpenFoodFactsClient.OffSearchResponse result = client.search(1, 20);

        assertThat(result.products).hasSize(1);
        assertThat(result.count).isEqualTo(1);
    }

    @Test
    @DisplayName("search: returns empty response when null body")
    void search_nullBody() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        OpenFoodFactsClient.OffSearchResponse result = client.search(1, 20);

        assertThat(result).isNotNull();
        assertThat(result.products).isNull();
    }

    @Test
    @DisplayName("search: with credentials when userId and password are set")
    void search_withCredentials() {
        ReflectionTestUtils.setField(client, "userId", "testuser");
        ReflectionTestUtils.setField(client, "password", "testpass");

        OpenFoodFactsClient.OffSearchResponse body = new OpenFoodFactsClient.OffSearchResponse();
        body.count = 0;
        body.products = List.of();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OpenFoodFactsClient.OffSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        OpenFoodFactsClient.OffSearchResponse result = client.search(1, 10);

        assertThat(result).isNotNull();
    }
}
