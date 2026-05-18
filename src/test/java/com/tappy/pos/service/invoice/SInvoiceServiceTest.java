package com.tappy.pos.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.invoice.*;
import com.tappy.pos.model.entity.finance.Invoice;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.audit.ApiAuditLogService;
import com.tappy.pos.service.invoice.sinvoice.SAccessTokenResponse;
import com.tappy.pos.service.invoice.sinvoice.SInvoiceFileResponse;
import com.tappy.pos.service.tenant.ShopConfigService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SInvoiceService Unit Tests")
class SInvoiceServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private SInvoiceMapper sInvoiceMapper;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private ShopConfigService shopConfigService;
    @Mock private ApiAuditLogService auditLogService;

    @InjectMocks private SInvoiceService sInvoiceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ShopInfo shopInfo;
    private Invoice invoice;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(sInvoiceService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(sInvoiceService, "sinvoiceApiUrl", "https://sinvoice.example.com");
        ReflectionTestUtils.setField(sInvoiceService, "sinvoiceTokenUrl", "https://sinvoice.example.com/auth/token");
        ReflectionTestUtils.setField(sInvoiceService, "authenticationMode", "Basic");
        ReflectionTestUtils.setField(sInvoiceService, "maxAttempts", 2);
        ReflectionTestUtils.setField(sInvoiceService, "pollDelay", 100);

        shopInfo = new ShopInfo();
        shopInfo.setShopName("Tiệm Vàng ABC");
        shopInfo.setSupplierTaxCode("0123456789");

        invoice = Invoice.builder()
                .invoiceNumber("INV-20240101-0001")
                .status(Invoice.InvoiceStatus.COMPLETED)
                .build();
        invoice.setId(1L);

        lenient().when(shopConfigService.getString(ShopConfigKey.EINVOICE_USERNAME)).thenReturn("testuser");
        lenient().when(shopConfigService.getString(ShopConfigKey.EINVOICE_PASSWORD)).thenReturn("testpass");
        lenient().when(shopConfigService.getString(ShopConfigKey.EINVOICE_TEMPLATE_CODE)).thenReturn("01GTKT");
        lenient().doNothing().when(auditLogService).logApiCall(any());
    }

    // ── createInvoice ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createInvoice: throws ResourceNotFoundException when shop info not found")
    void createInvoice_shopInfoNotFound() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        assertThatThrownBy(() -> sInvoiceService.createInvoice(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shop info not found");
    }

    @Test
    @DisplayName("createInvoice: returns success response when S-Invoice creates with invoiceNo")
    void createInvoice_successWithInvoiceNo() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .lineNumber(1L)
                .itemName("Nhẫn vàng")
                .itemCode("NV001")
                .unit("Cái")
                .unitPrice(new BigDecimal("2000000"))
                .quantity(new BigDecimal("1"))
                .itemTotalAmountWithTax(new BigDecimal("2200000"))
                .build();

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        SInvoiceRequest.ItemInfo itemInfo = new SInvoiceRequest.ItemInfo();
        itemInfo.setItemTotalAmountWithTax(new BigDecimal("2200000"));

        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceItemRequest(any())).thenReturn(itemInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        SInvoiceResponse.Result resultBody = new SInvoiceResponse.Result();
        resultBody.setInvoiceNo("INV-SYS-001");
        resultBody.setCodeOfTax("TAX001");
        SInvoiceResponse sInvoiceResponse = new SInvoiceResponse();
        sInvoiceResponse.setResult(resultBody);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(sInvoiceResponse));

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of(item))
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getInvoiceNo()).isEqualTo("INV-SYS-001");
        assertThat(response.getCodeOfTax()).isEqualTo("TAX001");
    }

    @Test
    @DisplayName("createInvoice: returns transactionId when response has no invoiceNo")
    void createInvoice_asyncWithTransactionId() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        SInvoiceResponse.Result resultBody = new SInvoiceResponse.Result();
        resultBody.setTransactionID("tx-uuid-pending");
        SInvoiceResponse sInvoiceResponse = new SInvoiceResponse();
        sInvoiceResponse.setResult(resultBody);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(sInvoiceResponse));

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response.getTransactionId()).isEqualTo("tx-uuid-pending");
    }

    @Test
    @DisplayName("createInvoice: handles errorCode response from S-Invoice")
    void createInvoice_errorCodeResponse() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        SInvoiceResponse sInvoiceResponse = new SInvoiceResponse();
        sInvoiceResponse.setErrorCode("ERR-001");
        sInvoiceResponse.setDescription("Invoice already exists");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(sInvoiceResponse));

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invoice already exists");
    }

    @Test
    @DisplayName("createInvoice: handles HTTP 400 BadRequest error")
    void createInvoice_http400Error() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, "{\"data\":\"Invalid supplier tax code\"}".getBytes(), null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenThrow(badRequest);

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Invalid supplier tax code");
    }

    @Test
    @DisplayName("createInvoice: handles non-400 HTTP error")
    void createInvoice_httpNon400Error() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        HttpClientErrorException unauthorized = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized",
                HttpHeaders.EMPTY, new byte[0], null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenThrow(unauthorized);

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("createInvoice: handles unexpected exception")
    void createInvoice_unexpectedException() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenThrow(new RuntimeException("Network error"));

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Unexpected error");
    }

    @Test
    @DisplayName("createInvoice: with visiting guest buyer sets buyerNotGetInvoice")
    void createInvoice_visitingGuestBuyer() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        SInvoiceRequest.BuyerRequest buyerReq = new SInvoiceRequest.BuyerRequest();
        buyerReq.setBuyerName("Guest");
        buyerReq.setBuyerIdNo("");

        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(buyerReq);

        SInvoiceResponse sResp = new SInvoiceResponse();
        sResp.setErrorCode("ERR");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(sResp));

        InvoiceBuyerRequest buyer = InvoiceBuyerRequest.builder()
                .buyerName("Guest")
                .visitingGuest(true)
                .build();

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .buyerInfo(buyer)
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response).isNotNull();
        assertThat(buyerReq.getBuyerNotGetInvoice()).isEqualTo(1);
    }

    @Test
    @DisplayName("createInvoice: item with empty unit sets null quantity and price when zero")
    void createInvoice_itemEmptyUnit_zeroQuantityPrice() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        SInvoiceRequest.ItemInfo itemInfo = new SInvoiceRequest.ItemInfo();
        itemInfo.setQuantity(BigDecimal.ZERO);
        itemInfo.setUnitPrice(BigDecimal.ZERO);

        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceItemRequest(any())).thenReturn(itemInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        SInvoiceResponse sResp = new SInvoiceResponse();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(sResp));

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .lineNumber(1L)
                .itemName("Service")
                .unit("")
                .quantity(BigDecimal.ZERO)
                .unitPrice(BigDecimal.ZERO)
                .itemTotalAmountWithTax(BigDecimal.ZERO)
                .build();

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of(item))
                .build();

        sInvoiceService.createInvoice(req);

        assertThat(itemInfo.getQuantity()).isNull();
        assertThat(itemInfo.getUnitPrice()).isNull();
    }

    // ── downloadInvoicePdf ────────────────────────────────────────────────────

    @Test
    @DisplayName("downloadInvoicePdf: throws ResourceNotFoundException when shop info not found")
    void downloadInvoicePdf_shopInfoNotFound() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sInvoiceService.downloadInvoicePdf(invoice))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("downloadInvoicePdf: returns decoded PDF bytes on success")
    void downloadInvoicePdf_success() throws Exception {
        invoice.setExternalInvoiceId("INV-001");
        invoice.setTransactionUuid("tx-uuid-123");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        String pdfContent = "PDF Content";
        String base64Pdf = java.util.Base64.getEncoder().encodeToString(pdfContent.getBytes());

        SInvoiceFileResponse fileResponse = new SInvoiceFileResponse();
        fileResponse.setErrorCode(0);
        fileResponse.setFileToBytes(base64Pdf);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceFileResponse.class)))
                .thenReturn(ResponseEntity.ok(fileResponse));

        byte[] result = sInvoiceService.downloadInvoicePdf(invoice);

        assertThat(result).isEqualTo(pdfContent.getBytes());
    }

    @Test
    @DisplayName("downloadInvoicePdf: throws IOException on HTTP error")
    void downloadInvoicePdf_httpError() {
        invoice.setExternalInvoiceId("INV-001");
        invoice.setTransactionUuid("tx-uuid-123");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        HttpClientErrorException notFound = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found",
                HttpHeaders.EMPTY, new byte[0], null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceFileResponse.class)))
                .thenThrow(notFound);

        assertThatThrownBy(() -> sInvoiceService.downloadInvoicePdf(invoice))
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("downloadInvoicePdf: throws IOException on non-OK status")
    void downloadInvoicePdf_nonOkStatus() {
        invoice.setExternalInvoiceId("INV-001");
        invoice.setTransactionUuid("tx-uuid-456");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        ResponseEntity<SInvoiceFileResponse> response =
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceFileResponse.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> sInvoiceService.downloadInvoicePdf(invoice))
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("downloadInvoicePdf: throws IOException on unexpected exception")
    void downloadInvoicePdf_unexpectedException() {
        invoice.setExternalInvoiceId("INV-001");
        invoice.setTransactionUuid("tx-uuid-789");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceFileResponse.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> sInvoiceService.downloadInvoicePdf(invoice))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("Unexpected error");
    }

    // ── sendEmailInvoice ──────────────────────────────────────────────────────

    @Test
    @DisplayName("sendEmailInvoice: throws ResourceNotFoundException when shop info not found")
    void sendEmailInvoice_shopInfoNotFound() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sInvoiceService.sendEmailInvoice(invoice))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("sendEmailInvoice: returns success when email sent")
    void sendEmailInvoice_success() {
        invoice.setTransactionUuid("tx-uuid-abc");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(new SInvoiceResponse()));

        InvoiceResponse result = sInvoiceService.sendEmailInvoice(invoice);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Email sent successfully");
    }

    @Test
    @DisplayName("sendEmailInvoice: handles HTTP 400 error with bad request message")
    void sendEmailInvoice_http400Error() {
        invoice.setTransactionUuid("tx-uuid-def");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request",
                HttpHeaders.EMPTY, "{\"data\":\"Email sending failed\"}".getBytes(), null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenThrow(badRequest);

        InvoiceResponse result = sInvoiceService.sendEmailInvoice(invoice);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Email sending failed");
    }

    @Test
    @DisplayName("sendEmailInvoice: handles non-400 HTTP error")
    void sendEmailInvoice_httpNon400Error() {
        invoice.setTransactionUuid("tx-uuid-ghi");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        HttpClientErrorException serverError = HttpClientErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                HttpHeaders.EMPTY, new byte[0], null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenThrow(serverError);

        InvoiceResponse result = sInvoiceService.sendEmailInvoice(invoice);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Failed to send email");
    }

    // ── createRequestHeaders with Bearer mode ─────────────────────────────────

    @Test
    @DisplayName("createInvoice: uses Bearer auth mode when configured")
    void createInvoice_bearerAuthMode() {
        ReflectionTestUtils.setField(sInvoiceService, "authenticationMode", "Bearer");
        ReflectionTestUtils.setField(sInvoiceService, "accessToken", null);
        ReflectionTestUtils.setField(sInvoiceService, "tokenExpirationTime", 0L);

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        SAccessTokenResponse tokenResponse = new SAccessTokenResponse();
        tokenResponse.setAccessToken("bearer-token-123");
        tokenResponse.setExpiresIn(3600);

        when(restTemplate.exchange(contains("/auth/token"), eq(HttpMethod.POST), any(), eq(SAccessTokenResponse.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));
        when(restTemplate.exchange(contains("/InvoiceWS"), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(new SInvoiceResponse()));

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        InvoiceResponse response = sInvoiceService.createInvoice(req);

        assertThat(response).isNotNull();
        verify(restTemplate, atLeast(1)).exchange(
                contains("/auth/token"), eq(HttpMethod.POST), any(), eq(SAccessTokenResponse.class));
    }

    @Test
    @DisplayName("createInvoice: reuses cached token when not expired")
    void createInvoice_reusesCachedToken() {
        ReflectionTestUtils.setField(sInvoiceService, "authenticationMode", "Bearer");
        ReflectionTestUtils.setField(sInvoiceService, "accessToken", "cached-token");
        ReflectionTestUtils.setField(sInvoiceService, "tokenExpirationTime", Long.MAX_VALUE);

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(shopInfo));

        SInvoiceRequest.GeneralInvoiceInfo generalInfo = new SInvoiceRequest.GeneralInvoiceInfo();
        when(sInvoiceMapper.fromGeneralInvoiceInfo(any())).thenReturn(generalInfo);
        when(sInvoiceMapper.fromInvoiceBuyerRequest(any())).thenReturn(null);

        when(restTemplate.exchange(contains("/InvoiceWS"), eq(HttpMethod.POST), any(), eq(SInvoiceResponse.class)))
                .thenReturn(ResponseEntity.ok(new SInvoiceResponse()));

        InvoiceRequest req = InvoiceRequest.builder()
                .orderId(1L)
                .paymentType("CASH")
                .itemInfo(List.of())
                .build();

        sInvoiceService.createInvoice(req);

        verify(restTemplate, never()).exchange(
                contains("/auth/token"), any(), any(), eq(SAccessTokenResponse.class));
    }
}
