package com.barbershop.service.invoice;

import com.barbershop.exception.ResourceNotFoundException;
import com.barbershop.model.dto.SInvoiceRequest;
import com.barbershop.model.dto.SInvoiceResponse;
import com.barbershop.model.dto.invoice.InvoiceResponse;
import com.barbershop.model.entity.Invoice;
import com.barbershop.model.entity.ShopInfo;
import com.barbershop.repository.ShopInfoRepository;
import com.barbershop.service.audit.ApiAuditLogService;
import com.barbershop.service.invoice.sinvoice.*;
import com.barbershop.util.TraceIdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.CompletableFuture;

import static com.barbershop.util.InvoiceUtil.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class SInvoiceService implements ExternalInvoiceService {
    private final RestTemplate restTemplate;
    private final ShopInfoRepository infoRepository;
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
    public InvoiceResponse createInvoice(Invoice request) {
        log.info("Creating S-Invoice {}", request);
        ShopInfo shopInfoEntity = infoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not found"));
        String templateCode = shopInfoEntity.getTemplateCode();
        String sinvoiceUsername = shopInfoEntity.getEInvoiceUsername();
        String sinvoicePassword = shopInfoEntity.getEInvoicePassword();
        String supplierTaxCode = shopInfoEntity.getSupplierTaxCode();

        List<SInvoiceRequest.ItemInfo> itemInfoList = request.getItems().stream().map(itemRequest -> {
            SInvoiceRequest.ItemInfo itemInfo = fromInvoiceItemRequest(itemRequest);
            //Process for the case when unit is empty or null, send quantity null and unitPrice null
            if (StringUtils.isEmpty(itemRequest.getUnit())) {
                if (BigDecimal.ZERO.equals(itemInfo.getQuantity())) {
                    itemInfo.setQuantity(null);
                }
                if (BigDecimal.ZERO.equals(itemInfo.getUnitPrice())) {
                    itemInfo.setUnitPrice(null);
                }
            }
            itemInfo.setItemCode(String.valueOf(itemRequest.getOrderItemId()));
            itemInfo.setLineNumber(Long.valueOf(itemRequest.getLineNumber()));
            itemInfo.setUnitName(itemRequest.getUnit());
            itemInfo.setItemTotalAmountAfterDiscount(itemRequest.getTotalAmountWithTax().setScale(0, RoundingMode.HALF_UP));
            itemInfo.setItemTotalAmountWithTax(itemRequest.getTotalAmountWithTax().setScale(0, RoundingMode.HALF_UP));
            return itemInfo;
        }).toList();
        BigDecimal totalAmountWithTax = itemInfoList.stream()
                .map(SInvoiceRequest.ItemInfo::getItemTotalAmountWithTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        SInvoiceRequest.GeneralInvoiceInfo invoiceInfo = fromGeneralInvoiceInfo(request);
        invoiceInfo.setPaymentTypeName(request.getPaymentType().code);
        invoiceInfo.setPaymentStatus(true);
        invoiceInfo.setTemplateCode(templateCode);
        invoiceInfo.setValidation(0);
        SInvoiceRequest.SummarizeInfo summarizeInfo = SInvoiceRequest.SummarizeInfo.builder()
                .totalAmountAfterDiscount(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .totalAmountWithTax(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .totalAmountWithoutTax(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .sumOfTotalLineAmountWithoutTax(totalAmountWithTax.setScale(0, RoundingMode.HALF_UP))
                .build();
        SInvoiceRequest.BuyerRequest buyerInfo = fromInvoiceBuyerRequest(request.getBuyer());
        if (StringUtils.isNotEmpty(buyerInfo.getBuyerIdNo())) {
            buyerInfo.setBuyerIdType("1");
        }
        if (request.getBuyer().isVisitingGuest()) {
            buyerInfo.setBuyerNotGetInvoice(1);
        }
        SInvoiceRequest sInvoiceRequest = SInvoiceRequest.builder()
                .generalInvoiceInfo(invoiceInfo)
                .itemInfo(itemInfoList)
                .summarizeInfo(summarizeInfo)
                .payments(Collections.singletonList(SInvoiceRequest.Payment.builder().paymentMethodName(request.getPaymentType().code).build()))
                .buyerInfo(buyerInfo)
                .build();
        HttpHeaders headers = createRequestHeaders(sinvoiceUsername, sinvoicePassword);
        HttpEntity<SInvoiceRequest> entity = new HttpEntity<>(sInvoiceRequest, headers);
        String createInvoiceUrl = sinvoiceApiUrl + S_INVOICE_EP_CREATE_INVOICE + supplierTaxCode;
        InvoiceResponse response = InvoiceResponse.builder()
                .success(false)
                .message("Failed to create invoice")
                .build();

        String traceId = TraceIdUtil.getOrGenerateTraceId();
        long startTime = System.currentTimeMillis();
        String responseBodyStr = null;
        Integer responseStatus = null;
        String errorMessage = null;
        String exceptionStackTrace = null;

        try {
            log.info("Executing create S-Invoice request {}, requestBody {}", createInvoiceUrl, objectMapper.writeValueAsString(sInvoiceRequest));
            ResponseEntity<SInvoiceResponse> responseEntity = restTemplate.exchange(createInvoiceUrl, HttpMethod.POST, entity, SInvoiceResponse.class);
            SInvoiceResponse sInvoiceResponse = responseEntity.getBody();
            responseStatus = responseEntity.getStatusCode().value();
            responseBodyStr = objectMapper.writeValueAsString(sInvoiceResponse);

            if (sInvoiceResponse != null && sInvoiceResponse.getResult() != null) {
                if (sInvoiceResponse.getResult().getInvoiceNo() != null) {
                    response.setInvoiceNo(sInvoiceResponse.getResult().getInvoiceNo());
                    response.setCodeOfTax(sInvoiceResponse.getResult().getCodeOfTax());
                    response.setSuccess(true);
                    response.setMessage("Invoice created successfully with invoice number: " + sInvoiceResponse.getResult().getInvoiceNo());
                } else {
                    String transactionUuid = sInvoiceResponse.getResult().getTransactionID();
                    CompletableFuture.runAsync(() -> pollForInvoiceNumber(response, transactionUuid, supplierTaxCode, sinvoiceUsername, sinvoicePassword));
                }
            } else if (sInvoiceResponse != null && StringUtils.isNotEmpty(sInvoiceResponse.getErrorCode())) {
                response.setSuccess(false);
                response.setMessage(sInvoiceResponse.getDescription());
            }
        } catch (HttpClientErrorException e) {
            responseStatus = e.getStatusCode().value();
            errorMessage = e.getMessage();
            exceptionStackTrace = ApiAuditLogService.getStackTraceAsString(e);

            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                response.setSuccess(false);
                response.setMessage(getErrorMessageBadRequest(e));
            }
            log.error("Exception while create S-Invoice", e);
        } catch (Exception ex) {
            responseStatus = 500;
            errorMessage = ex.getMessage();
            exceptionStackTrace = ApiAuditLogService.getStackTraceAsString(ex);

            log.error("Unexpected error while creating S-Invoice", ex);
            response.setSuccess(false);
            response.setMessage("Unexpected error: " + ex.getMessage());
        } finally {
            // Log the API call
            long executionTimeMs = System.currentTimeMillis() - startTime;
            try {
                ApiAuditLogService.ApiAuditLogRequest auditRequest = ApiAuditLogService.ApiAuditLogRequest.builder()
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
                        .description("S-Invoice create invoice request")
                        .build();
                auditLogService.logApiCall(auditRequest);
            } catch (Exception e) {
                log.error("Failed to log API audit", e);
            }
        }
        return response;
    }

    private String getErrorMessageBadRequest(HttpClientErrorException e) {
        String responseBody = e.getResponseBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(responseBody);
            return jsonNode.path("data").asText();
        } catch (JsonProcessingException ex) {
            log.error("Error parsing error message from S-Invoice response", ex);
            return "Bad Request";
        }
    }

    private HttpHeaders createRequestHeaders(String sinvoiceUsername, String sinvoicePassword) {
        HttpHeaders headers = new HttpHeaders();
        if (authenticationMode.equalsIgnoreCase("Basic")) {
            String auth = sinvoiceUsername + ":" + sinvoicePassword;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        } else {
            String token = getAccessToken(sinvoiceUsername, sinvoicePassword);
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }

    private String getAccessToken(String sinvoiceUsername, String sinvoicePassword) {
        if (accessToken == null || isTokenExpired()) {
            log.info("Get new access token.");
            // Request a new token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", sinvoiceUsername, sinvoicePassword);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<SAccessTokenResponse> responseEntity = restTemplate.exchange(sinvoiceTokenUrl, HttpMethod.POST, entity, SAccessTokenResponse.class);
            SAccessTokenResponse tokenResponse = responseEntity.getBody();
            if (tokenResponse != null) {
                accessToken = tokenResponse.getAccessToken();
                tokenExpirationTime = Instant.now().getEpochSecond() + tokenResponse.getExpiresIn() - 60; // 1 minute buffer
            }
        }
        return accessToken;
    }

    private boolean isTokenExpired() {
        return Instant.now().getEpochSecond() >= tokenExpirationTime;
    }

    private void pollForInvoiceNumber(InvoiceResponse response,
                                      String transactionID,
                                      String supplierTaxCode,
                                      String sInvoiceUsername,
                                      String sInvoicePassword) {
        try {
            log.info("Polling invoice number for {}", transactionID);
            // Polling logic
            boolean invoiceRetrieved = false;
            int attempts = 0;
            while (!invoiceRetrieved && attempts < maxAttempts) {
                Thread.sleep(pollDelay);
                attempts++;
                SInvoiceStatusRequest statusRequest = SInvoiceStatusRequest.builder()
                        .transactionUuid(transactionID)
                        .supplierTaxCode(supplierTaxCode)
                        .build();
                HttpHeaders headers = createRequestHeaders(sInvoiceUsername, sInvoicePassword);
                HttpEntity<SInvoiceStatusRequest> entity = new HttpEntity<>(statusRequest, headers);
                String getInvoiceStatusUrl = sinvoiceApiUrl + S_INVOICE_EP_SEARCH_BY_TRANSACTION_INVOICE;
                ResponseEntity<SInvoiceStatusResponse> responseEntity = restTemplate.exchange(getInvoiceStatusUrl, HttpMethod.POST, entity, SInvoiceStatusResponse.class);
                SInvoiceStatusResponse statusResponse = responseEntity.getBody();
                if (statusResponse != null && statusResponse.getResult() != null && !statusResponse.getResult().isEmpty()) {
                    SInvoiceStatusResponse.StatusResult result = statusResponse.getResult().get(0);
                    if (result.getInvoiceNo() != null) {
                        response.setMessage("Invoice created successfully with invoice number: " + result.getInvoiceNo());
                        invoiceRetrieved = true;
                        response.setInvoiceNo(result.getInvoiceNo());
                        response.setCodeOfTax(result.getCodeOfTax());
                        response.setSuccess(true);
                    }
                }
            }
            if (!invoiceRetrieved) {
                response.setSuccess(false);
                response.setMessage("Failed to retrieve invoice number within the expected time.");
            }
        } catch (InterruptedException e) {
            log.error("Error while polling the invoice number", e);
            response.setSuccess(false);
            response.setMessage("Error while polling for invoice number: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws IOException {
        String invoiceNo = invoice.getExternalInvoiceId();
        String transactionUuid = invoice.getTransactionUuid();
        String fileType = "pdf";
        ShopInfo shopInfoEntity = infoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not found"));
        String templateCode = shopInfoEntity.getTemplateCode();
        String sinvoiceUsername = shopInfoEntity.getEInvoiceUsername();
        String sinvoicePassword = shopInfoEntity.getEInvoicePassword();
        String supplierTaxCode = shopInfoEntity.getSupplierTaxCode();

        log.info("Start downloading the PDF invoice supplierTaxCode {}, invoiceNo {}, templateCode {}, transactionUuid {}, fileType {}", supplierTaxCode, invoiceNo, templateCode, transactionUuid, fileType);
        FileInvoiceRequest invoiceRequest = FileInvoiceRequest.builder()
                .supplierTaxCode(supplierTaxCode)
                .invoiceNo(invoiceNo)
                .templateCode(templateCode)
                .transactionUuid(transactionUuid)
                .fileType(fileType)
                .build();

        // Create headers
        HttpHeaders headers = createRequestHeaders(sinvoiceUsername, sinvoicePassword);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Create entity
        HttpEntity<FileInvoiceRequest> entity = new HttpEntity<>(invoiceRequest, headers);

        log.info("Executing S-Invoice request to download PDF file");
        try {
            ResponseEntity<SInvoiceFileResponse> response = restTemplate.exchange(
                    sinvoiceApiUrl + S_INVOICE_EP_DOWNLOAD_INVOICE, HttpMethod.POST, entity, SInvoiceFileResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SInvoiceFileResponse responseBody = response.getBody();
                String base64File = responseBody.getFileToBytes();
                log.info("Successfully retrieved invoice PDF, file size: {} bytes", base64File.length());
                return Base64.getDecoder().decode(base64File);
            } else {
                log.error("Failed to download invoice PDF. Status code: {}", response.getStatusCode());
                throw new IOException("Failed to download invoice PDF from SInvoice system. Status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error while downloading invoice PDF. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new IOException("Failed to download invoice PDF from SInvoice system. Error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while downloading invoice PDF", e);
            throw new IOException("Failed to download invoice PDF from SInvoice system. Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public InvoiceResponse sendEmailInvoice(Invoice invoice) {
        log.info("S-Invoice sending email {}", invoice.getInvoiceNumber());
        ShopInfo shopInfoEntity = infoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not found"));
        String sinvoiceUsername = shopInfoEntity.getEInvoiceUsername();
        String sinvoicePassword = shopInfoEntity.getEInvoicePassword();
        String supplierTaxCode = shopInfoEntity.getSupplierTaxCode();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("supplierTaxCode", supplierTaxCode);
        requestBody.put("lstTransactionUuid", invoice.getTransactionUuid());
        HttpHeaders headers = createRequestHeaders(sinvoiceUsername, sinvoicePassword);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        String emailInvoiceUrl = sinvoiceApiUrl + S_INVOICE_EP_SEND_EMAIL_INVOICE;
        InvoiceResponse response = InvoiceResponse.builder()
                .success(false)
                .message("Failed to create invoice")
                .build();
        try {
            log.info("Executing S-Invoice request to send email");
            restTemplate.exchange(emailInvoiceUrl, HttpMethod.POST, entity, SInvoiceResponse.class);
            response.setSuccess(true);
            response.setMessage("");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                response.setSuccess(false);
                response.setMessage(getErrorMessageBadRequest(e));
            }
            log.error("Exception while sending email S-Invoice", e);
        }
        return response;
    }
}