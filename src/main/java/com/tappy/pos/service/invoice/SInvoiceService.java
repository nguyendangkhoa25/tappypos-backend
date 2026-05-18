package com.tappy.pos.service.invoice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.dto.invoice.SInvoiceRequest;
import com.tappy.pos.model.dto.invoice.SInvoiceResponse;
import com.tappy.pos.model.entity.finance.Invoice;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.tenant.ShopConfigService;
import com.tappy.pos.service.audit.ApiAuditLogService;
import com.tappy.pos.service.invoice.sinvoice.FileInvoiceRequest;
import com.tappy.pos.service.invoice.sinvoice.SAccessTokenResponse;
import com.tappy.pos.service.invoice.sinvoice.SInvoiceFileResponse;
import com.tappy.pos.service.invoice.sinvoice.SInvoiceStatusRequest;
import com.tappy.pos.service.invoice.sinvoice.SInvoiceStatusResponse;
import com.tappy.pos.util.TraceIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static com.tappy.pos.util.InvoiceUtil.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SInvoiceService implements ExternalInvoiceService {

    private final RestTemplate restTemplate;
    private final SInvoiceMapper sInvoiceMapper;
    private final ShopInfoRepository shopInfoRepository;
    private final ShopConfigService shopConfigService;
    private final ApiAuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${sinvoice.baseUrl}")
    private String sinvoiceApiUrl;
    @Value("${sinvoice.poll.maxAttempts:5}")
    private Integer maxAttempts;
    @Value("${sinvoice.poll.delay:10000}")
    private Integer pollDelay;
    @Value("${sinvoice.tokenUrl}")
    private String sinvoiceTokenUrl;
    @Value("${sinvoice.authenticationMode:Basic}")
    private String authenticationMode;

    private String accessToken;
    private long tokenExpirationTime;

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("Creating S-Invoice for orderId={}", request.getOrderId());

        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not found"));

        String templateCode    = shopConfigService.getString(ShopConfigKey.EINVOICE_TEMPLATE_CODE);
        String username        = shopConfigService.getString(ShopConfigKey.EINVOICE_USERNAME);
        String password        = shopConfigService.getString(ShopConfigKey.EINVOICE_PASSWORD);
        String supplierTaxCode = shopInfo.getSupplierTaxCode();

        List<SInvoiceRequest.ItemInfo> itemInfoList = request.getItemInfo().stream().map(itemRequest -> {
            SInvoiceRequest.ItemInfo itemInfo = sInvoiceMapper.fromInvoiceItemRequest(itemRequest);
            if (StringUtils.isEmpty(itemRequest.getUnit())) {
                if (BigDecimal.ZERO.equals(itemInfo.getQuantity())) itemInfo.setQuantity(null);
                if (BigDecimal.ZERO.equals(itemInfo.getUnitPrice())) itemInfo.setUnitPrice(null);
            }
            itemInfo.setItemCode(itemRequest.getItemCode());
            itemInfo.setLineNumber(itemRequest.getLineNumber());
            itemInfo.setUnitName(itemRequest.getUnit());
            if (itemRequest.getItemTotalAmountWithTax() != null) {
                itemInfo.setItemTotalAmountAfterDiscount(itemRequest.getItemTotalAmountWithTax().setScale(0, RoundingMode.HALF_UP));
                itemInfo.setItemTotalAmountWithTax(itemRequest.getItemTotalAmountWithTax().setScale(0, RoundingMode.HALF_UP));
            }
            return itemInfo;
        }).toList();

        BigDecimal totalAmountWithTax = itemInfoList.stream()
                .map(SInvoiceRequest.ItemInfo::getItemTotalAmountWithTax)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SInvoiceRequest.GeneralInvoiceInfo invoiceInfo = sInvoiceMapper.fromGeneralInvoiceInfo(request);
        invoiceInfo.setPaymentTypeName(request.getPaymentType());
        invoiceInfo.setPaymentStatus(true);
        invoiceInfo.setTemplateCode(templateCode);
        invoiceInfo.setValidation(0);

        SInvoiceRequest.SummarizeInfo summarizeInfo = SInvoiceRequest.SummarizeInfo.builder()
                .totalAmountAfterDiscount(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .totalAmountWithTax(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .totalAmountWithoutTax(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .sumOfTotalLineAmountWithoutTax(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .build();

        SInvoiceRequest.BuyerRequest buyerInfo = sInvoiceMapper.fromInvoiceBuyerRequest(request.getBuyerInfo());
        if (buyerInfo != null) {
            if (StringUtils.isNotEmpty(buyerInfo.getBuyerIdNo())) {
                buyerInfo.setBuyerIdType("1");
            }
            if (request.getBuyerInfo() != null && request.getBuyerInfo().isVisitingGuest()) {
                buyerInfo.setBuyerNotGetInvoice(1);
            }
        }

        SInvoiceRequest sInvoiceRequest = SInvoiceRequest.builder()
                .generalInvoiceInfo(invoiceInfo)
                .itemInfo(itemInfoList)
                .summarizeInfo(summarizeInfo)
                .payments(Collections.singletonList(
                        SInvoiceRequest.Payment.builder().paymentMethodName(request.getPaymentType()).build()))
                .buyerInfo(buyerInfo)
                .build();

        HttpHeaders headers = createRequestHeaders(username, password);
        HttpEntity<SInvoiceRequest> entity = new HttpEntity<>(sInvoiceRequest, headers);
        String createInvoiceUrl = sinvoiceApiUrl + S_INVOICE_EP_CREATE_INVOICE + supplierTaxCode;

        InvoiceResponse response = InvoiceResponse.builder().success(false).message("Failed to create invoice").build();

        String traceId = TraceIdUtil.getOrGenerateTraceId();
        long startTime = System.currentTimeMillis();
        String responseBodyStr = null;
        Integer responseStatus = null;
        String errorMessage = null;
        String exceptionStackTrace = null;

        try {
            log.info("POST S-Invoice create: {}", createInvoiceUrl);
            ResponseEntity<SInvoiceResponse> responseEntity =
                    restTemplate.exchange(createInvoiceUrl, HttpMethod.POST, entity, SInvoiceResponse.class);
            SInvoiceResponse sInvoiceResponse = responseEntity.getBody();
            responseStatus = responseEntity.getStatusCode().value();
            responseBodyStr = objectMapper.writeValueAsString(sInvoiceResponse);

            if (sInvoiceResponse != null && sInvoiceResponse.getResult() != null) {
                if (sInvoiceResponse.getResult().getInvoiceNo() != null) {
                    response.setInvoiceNo(sInvoiceResponse.getResult().getInvoiceNo());
                    response.setCodeOfTax(sInvoiceResponse.getResult().getCodeOfTax());
                    response.setSuccess(true);
                    response.setMessage("Invoice created: " + sInvoiceResponse.getResult().getInvoiceNo());
                } else {
                    // S-Invoice returned a transactionUuid — poll synchronously so the caller
                    // (AsyncInvoiceService) can persist the final invoiceNo to the DB.
                    String transactionUuid = sInvoiceResponse.getResult().getTransactionID();
                    response.setTransactionId(transactionUuid);
                    pollForInvoiceNumber(response, transactionUuid, supplierTaxCode, username, password);
                }
            } else if (sInvoiceResponse != null && StringUtils.isNotEmpty(sInvoiceResponse.getErrorCode())) {
                response.setMessage(sInvoiceResponse.getDescription());
            }
        } catch (HttpClientErrorException e) {
            responseStatus = e.getStatusCode().value();
            errorMessage = e.getMessage();
            exceptionStackTrace = ApiAuditLogService.getStackTraceAsString(e);
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                response.setMessage(extractBadRequestMessage(e));
            }
            log.error("HTTP error creating S-Invoice", e);
        } catch (Exception ex) {
            responseStatus = 500;
            errorMessage = ex.getMessage();
            exceptionStackTrace = ApiAuditLogService.getStackTraceAsString(ex);
            response.setMessage("Unexpected error: " + ex.getMessage());
            log.error("Unexpected error creating S-Invoice", ex);
        } finally {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            try {
                auditLogService.logApiCall(ApiAuditLogService.ApiAuditLogRequest.builder()
                        .traceId(traceId)
                        .apiEndpoint(createInvoiceUrl)
                        .httpMethod("POST")
                        .requestBody(sInvoiceRequest)
                        .requestHeaders(headers)
                        .responseBody(responseBodyStr)
                        .responseStatus(responseStatus != null ? responseStatus : 500)
                        .executionTimeMs(executionTimeMs)
                        .errorMessage(errorMessage)
                        .exceptionStackTrace(exceptionStackTrace)
                        .userId("SYSTEM")
                        .ipAddress(TraceIdUtil.getClientIpAddress())
                        .status(response.isSuccess() ? "SUCCESS" : "FAILURE")
                        .description("S-Invoice create invoice")
                        .build());
            } catch (Exception e) {
                log.error("Failed to log audit", e);
            }
        }
        return response;
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws IOException {
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not found"));

        String invoiceNo       = invoice.getExternalInvoiceId();
        String transactionUuid = invoice.getTransactionUuid();
        String templateCode    = shopConfigService.getString(ShopConfigKey.EINVOICE_TEMPLATE_CODE);
        String username        = shopConfigService.getString(ShopConfigKey.EINVOICE_USERNAME);
        String password        = shopConfigService.getString(ShopConfigKey.EINVOICE_PASSWORD);
        String supplierTaxCode = shopInfo.getSupplierTaxCode();

        log.info("Downloading S-Invoice PDF: supplierTaxCode={}, invoiceNo={}, transactionUuid={}", supplierTaxCode, invoiceNo, transactionUuid);

        FileInvoiceRequest invoiceRequest = FileInvoiceRequest.builder()
                .supplierTaxCode(supplierTaxCode)
                .invoiceNo(invoiceNo)
                .templateCode(templateCode)
                .transactionUuid(transactionUuid)
                .fileType("pdf")
                .build();

        HttpHeaders headers = createRequestHeaders(username, password);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FileInvoiceRequest> entity = new HttpEntity<>(invoiceRequest, headers);

        try {
            ResponseEntity<SInvoiceFileResponse> response = restTemplate.exchange(
                    sinvoiceApiUrl + S_INVOICE_EP_DOWNLOAD_INVOICE, HttpMethod.POST, entity, SInvoiceFileResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String base64File = response.getBody().getFileToBytes();
                log.info("S-Invoice PDF retrieved, size={}", base64File.length());
                return Base64.getDecoder().decode(base64File);
            }
            throw new IOException("S-Invoice PDF download failed. Status: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.error("HTTP error downloading S-Invoice PDF: {}", e.getResponseBodyAsString(), e);
            throw new IOException("S-Invoice PDF download failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error downloading S-Invoice PDF", e);
            throw new IOException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public InvoiceResponse sendEmailInvoice(Invoice invoice) {
        log.info("S-Invoice send email for invoiceId={}", invoice.getId());

        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not found"));

        String username        = shopConfigService.getString(ShopConfigKey.EINVOICE_USERNAME);
        String password        = shopConfigService.getString(ShopConfigKey.EINVOICE_PASSWORD);
        String supplierTaxCode = shopInfo.getSupplierTaxCode();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("supplierTaxCode", supplierTaxCode);
        requestBody.put("lstTransactionUuid", invoice.getTransactionUuid());

        HttpHeaders headers = createRequestHeaders(username, password);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        InvoiceResponse response = InvoiceResponse.builder().success(false).message("Failed to send email").build();
        try {
            restTemplate.exchange(sinvoiceApiUrl + S_INVOICE_EP_SENDEMAIL_INVOICE, HttpMethod.POST, entity, SInvoiceResponse.class);
            response.setSuccess(true);
            response.setMessage("Email sent successfully");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                response.setMessage(extractBadRequestMessage(e));
            }
            log.error("HTTP error sending S-Invoice email", e);
        }
        return response;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String extractBadRequestMessage(HttpClientErrorException e) {
        try {
            JsonNode node = objectMapper.readTree(e.getResponseBodyAsString());
            return node.path("data").asText();
        } catch (JsonProcessingException ex) {
            return "Bad Request";
        }
    }

    private HttpHeaders createRequestHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        if ("Basic".equalsIgnoreCase(authenticationMode)) {
            String auth = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encoded);
        } else {
            headers.set("Authorization", "Bearer " + getAccessToken(username, password));
        }
        return headers;
    }

    private String getAccessToken(String username, String password) {
        if (accessToken == null || Instant.now().getEpochSecond() >= tokenExpirationTime) {
            log.info("Refreshing S-Invoice access token");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<SAccessTokenResponse> resp =
                    restTemplate.exchange(sinvoiceTokenUrl, HttpMethod.POST, entity, SAccessTokenResponse.class);
            SAccessTokenResponse token = resp.getBody();
            if (token != null) {
                accessToken = token.getAccessToken();
                tokenExpirationTime = Instant.now().getEpochSecond() + token.getExpiresIn() - 60;
            }
        }
        return accessToken;
    }

    private void pollForInvoiceNumber(InvoiceResponse response, String transactionUuid,
                                       String supplierTaxCode, String username, String password) {
        try {
            log.info("Polling S-Invoice for transactionUuid={}", transactionUuid);
            boolean retrieved = false;
            int attempts = 0;
            while (!retrieved && attempts < maxAttempts) {
                Thread.sleep(pollDelay);
                attempts++;
                SInvoiceStatusRequest statusRequest = SInvoiceStatusRequest.builder()
                        .transactionUuid(transactionUuid)
                        .supplierTaxCode(supplierTaxCode)
                        .build();
                HttpHeaders headers = createRequestHeaders(username, password);
                HttpEntity<SInvoiceStatusRequest> entity = new HttpEntity<>(statusRequest, headers);
                ResponseEntity<SInvoiceStatusResponse> statusResp = restTemplate.exchange(
                        sinvoiceApiUrl + S_INVOICE_EP_SEARCH_BYTRANSACTION_INVOICE,
                        HttpMethod.POST, entity, SInvoiceStatusResponse.class);
                SInvoiceStatusResponse statusBody = statusResp.getBody();
                if (statusBody != null && statusBody.getResult() != null && !statusBody.getResult().isEmpty()) {
                    SInvoiceStatusResponse.StatusResult result = statusBody.getResult().get(0);
                    if (result.getInvoiceNo() != null) {
                        response.setInvoiceNo(result.getInvoiceNo());
                        response.setCodeOfTax(result.getCodeOfTax());
                        response.setSuccess(true);
                        response.setMessage("Invoice created: " + result.getInvoiceNo());
                        retrieved = true;
                    }
                }
            }
            if (!retrieved) {
                response.setSuccess(false);
                response.setMessage("Failed to retrieve invoice number within expected time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Polling interrupted", e);
            response.setSuccess(false);
            response.setMessage("Polling interrupted: " + e.getMessage());
        }
    }
}
