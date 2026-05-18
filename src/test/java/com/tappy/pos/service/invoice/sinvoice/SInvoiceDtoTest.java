package com.tappy.pos.service.invoice.sinvoice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("S-Invoice DTO Tests")
class SInvoiceDtoTest {

    // ── SAccessTokenResponse ──────────────────────────────────────────────────

    @Test
    @DisplayName("SAccessTokenResponse stores all token fields")
    void sAccessTokenResponse_gettersSetters() {
        SAccessTokenResponse token = new SAccessTokenResponse();
        token.setAccessToken("eyJhbGciOiJSUzI1NiJ9.xxx");
        token.setTokenType("Bearer");
        token.setRefreshToken("refresh_token_value");
        token.setExpiresIn(3600);
        token.setScope("read write");
        token.setIssuedAt(1699999999L);
        token.setInvoiceCluster("cluster-1");
        token.setType(1);
        token.setJwtId("jti-uuid");

        assertThat(token.getAccessToken()).isEqualTo("eyJhbGciOiJSUzI1NiJ9.xxx");
        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getRefreshToken()).isEqualTo("refresh_token_value");
        assertThat(token.getExpiresIn()).isEqualTo(3600);
        assertThat(token.getScope()).isEqualTo("read write");
        assertThat(token.getIssuedAt()).isEqualTo(1699999999L);
        assertThat(token.getInvoiceCluster()).isEqualTo("cluster-1");
        assertThat(token.getType()).isEqualTo(1);
        assertThat(token.getJwtId()).isEqualTo("jti-uuid");
    }

    @Test
    @DisplayName("SAccessTokenResponse equals and hashCode via @Data")
    void sAccessTokenResponse_equalsHashCode() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setAccessToken("token");
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setAccessToken("token");
        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    // ── SInvoiceStatusResponse ────────────────────────────────────────────────

    @Test
    @DisplayName("SInvoiceStatusResponse stores response fields")
    void sInvoiceStatusResponse_gettersSetters() {
        SInvoiceStatusResponse.StatusResult result = new SInvoiceStatusResponse.StatusResult();
        result.setSupplierTaxCode("0123456789");
        result.setInvoiceNo("INV-001");
        result.setReservationCode("RES-001");
        result.setIssueDate(1699999999L);
        result.setStatus("ISSUED");
        result.setExchangeStatus("EXCHANGED");
        result.setExchangeDes("Description");
        result.setCodeOfTax("TAX001");

        assertThat(result.getSupplierTaxCode()).isEqualTo("0123456789");
        assertThat(result.getInvoiceNo()).isEqualTo("INV-001");
        assertThat(result.getReservationCode()).isEqualTo("RES-001");
        assertThat(result.getIssueDate()).isEqualTo(1699999999L);
        assertThat(result.getStatus()).isEqualTo("ISSUED");
        assertThat(result.getExchangeStatus()).isEqualTo("EXCHANGED");
        assertThat(result.getExchangeDes()).isEqualTo("Description");
        assertThat(result.getCodeOfTax()).isEqualTo("TAX001");
    }

    @Test
    @DisplayName("SInvoiceStatusResponse stores outer response fields")
    void sInvoiceStatusResponse_outerFields() {
        SInvoiceStatusResponse response = new SInvoiceStatusResponse();
        response.setErrorCode("0");
        response.setDescription("Success");
        response.setTransactionUuid("tx-uuid-abc");
        response.setResult(List.of(new SInvoiceStatusResponse.StatusResult()));

        assertThat(response.getErrorCode()).isEqualTo("0");
        assertThat(response.getDescription()).isEqualTo("Success");
        assertThat(response.getTransactionUuid()).isEqualTo("tx-uuid-abc");
        assertThat(response.getResult()).hasSize(1);
    }

    // ── SInvoiceFileResponse ──────────────────────────────────────────────────

    @Test
    @DisplayName("SInvoiceFileResponse stores download response fields")
    void sInvoiceFileResponse_gettersSetters() {
        SInvoiceFileResponse response = new SInvoiceFileResponse();
        response.setErrorCode(0);
        response.setDescription("OK");
        response.setPaymentStatus(true);
        response.setFileName("invoice.pdf");
        response.setFileToBytes("base64encodedcontent");

        assertThat(response.getErrorCode()).isEqualTo(0);
        assertThat(response.getDescription()).isEqualTo("OK");
        assertThat(response.isPaymentStatus()).isTrue();
        assertThat(response.getFileName()).isEqualTo("invoice.pdf");
        assertThat(response.getFileToBytes()).isEqualTo("base64encodedcontent");
    }

    @Test
    @DisplayName("SInvoiceFileResponse equals and hashCode via @Data")
    void sInvoiceFileResponse_equalsHashCode() {
        SInvoiceFileResponse r1 = new SInvoiceFileResponse();
        r1.setFileName("file.pdf");
        SInvoiceFileResponse r2 = new SInvoiceFileResponse();
        r2.setFileName("file.pdf");
        assertThat(r1).isEqualTo(r2);
    }

    // ── FileInvoiceRequest ────────────────────────────────────────────────────

    @Test
    @DisplayName("FileInvoiceRequest builder creates request with all fields")
    void fileInvoiceRequest_builder() {
        FileInvoiceRequest request = FileInvoiceRequest.builder()
                .supplierTaxCode("0123456789")
                .invoiceNo("INV-001")
                .templateCode("01GTKT")
                .transactionUuid("tx-uuid-def")
                .fileType("pdf")
                .build();

        assertThat(request.getSupplierTaxCode()).isEqualTo("0123456789");
        assertThat(request.getInvoiceNo()).isEqualTo("INV-001");
        assertThat(request.getTemplateCode()).isEqualTo("01GTKT");
        assertThat(request.getTransactionUuid()).isEqualTo("tx-uuid-def");
        assertThat(request.getFileType()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("FileInvoiceRequest supports setters via @Data")
    void fileInvoiceRequest_setters() {
        FileInvoiceRequest request = FileInvoiceRequest.builder().build();
        request.setSupplierTaxCode("9876543210");
        request.setFileType("xml");
        assertThat(request.getSupplierTaxCode()).isEqualTo("9876543210");
        assertThat(request.getFileType()).isEqualTo("xml");
    }

    // ── SInvoiceStatusRequest ─────────────────────────────────────────────────

    @Test
    @DisplayName("SInvoiceStatusRequest builder creates request")
    void sInvoiceStatusRequest_builder() {
        SInvoiceStatusRequest request = SInvoiceStatusRequest.builder()
                .supplierTaxCode("0123456789")
                .transactionUuid("tx-uuid-ghi")
                .build();

        assertThat(request.getSupplierTaxCode()).isEqualTo("0123456789");
        assertThat(request.getTransactionUuid()).isEqualTo("tx-uuid-ghi");
    }

    @Test
    @DisplayName("SInvoiceStatusRequest supports setters via @Data")
    void sInvoiceStatusRequest_setters() {
        SInvoiceStatusRequest request = SInvoiceStatusRequest.builder().build();
        request.setSupplierTaxCode("1234567890");
        request.setTransactionUuid("new-uuid");
        assertThat(request.getSupplierTaxCode()).isEqualTo("1234567890");
        assertThat(request.getTransactionUuid()).isEqualTo("new-uuid");
    }

    // ── SInvoiceStatusRequest: equals / hashCode / toString ───────────────────

    @Test
    @DisplayName("SInvoiceStatusRequest: equals returns false for null")
    void sInvoiceStatusRequest_equalsNull() {
        SInvoiceStatusRequest r = SInvoiceStatusRequest.builder().supplierTaxCode("A").build();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: equals returns false for different type")
    void sInvoiceStatusRequest_equalsDifferentType() {
        SInvoiceStatusRequest r = SInvoiceStatusRequest.builder().build();
        assertThat(r.equals("string")).isFalse();
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: equals returns false when supplierTaxCode differs")
    void sInvoiceStatusRequest_equalsDifferentSupplier() {
        SInvoiceStatusRequest r1 = SInvoiceStatusRequest.builder().supplierTaxCode("A").build();
        SInvoiceStatusRequest r2 = SInvoiceStatusRequest.builder().supplierTaxCode("B").build();
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: equals returns false when transactionUuid differs")
    void sInvoiceStatusRequest_equalsDifferentUuid() {
        SInvoiceStatusRequest r1 = SInvoiceStatusRequest.builder().transactionUuid("uuid-1").build();
        SInvoiceStatusRequest r2 = SInvoiceStatusRequest.builder().transactionUuid("uuid-2").build();
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: equals handles null field on one side")
    void sInvoiceStatusRequest_equalsOneNull() {
        SInvoiceStatusRequest r1 = SInvoiceStatusRequest.builder().supplierTaxCode("A").build();
        SInvoiceStatusRequest r2 = SInvoiceStatusRequest.builder().build();
        assertThat(r1).isNotEqualTo(r2);
        assertThat(r2).isNotEqualTo(r1);
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: hashCode consistent across calls")
    void sInvoiceStatusRequest_hashCode() {
        SInvoiceStatusRequest r = SInvoiceStatusRequest.builder()
                .supplierTaxCode("0123456789").transactionUuid("uuid-abc").build();
        assertThat(r.hashCode()).isEqualTo(r.hashCode());
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: hashCode differs for null vs non-null field")
    void sInvoiceStatusRequest_hashCodeNullVsNonNull() {
        SInvoiceStatusRequest r1 = SInvoiceStatusRequest.builder().supplierTaxCode("A").build();
        SInvoiceStatusRequest r2 = SInvoiceStatusRequest.builder().build();
        assertThat(r1.hashCode()).isNotEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: toString contains field names")
    void sInvoiceStatusRequest_toString() {
        SInvoiceStatusRequest r = SInvoiceStatusRequest.builder()
                .supplierTaxCode("0123").transactionUuid("tx-1").build();
        assertThat(r.toString()).contains("supplierTaxCode").contains("transactionUuid");
    }

    // ── FileInvoiceRequest: untested fields and equals / hashCode / toString ──

    @Test
    @DisplayName("FileInvoiceRequest: invoiceNo, templateCode, transactionUuid getters")
    void fileInvoiceRequest_allFields() {
        FileInvoiceRequest r = FileInvoiceRequest.builder()
                .supplierTaxCode("0123456789")
                .invoiceNo("INV-TEST")
                .templateCode("01GTKT")
                .transactionUuid("uuid-xyz")
                .fileType("pdf")
                .build();
        assertThat(r.getInvoiceNo()).isEqualTo("INV-TEST");
        assertThat(r.getTemplateCode()).isEqualTo("01GTKT");
        assertThat(r.getTransactionUuid()).isEqualTo("uuid-xyz");
    }

    @Test
    @DisplayName("FileInvoiceRequest: equals returns false for null")
    void fileInvoiceRequest_equalsNull() {
        FileInvoiceRequest r = FileInvoiceRequest.builder().build();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    @DisplayName("FileInvoiceRequest: equals returns false for different type")
    void fileInvoiceRequest_equalsDifferentType() {
        FileInvoiceRequest r = FileInvoiceRequest.builder().build();
        assertThat(r.equals(42)).isFalse();
    }

    @Test
    @DisplayName("FileInvoiceRequest: equals returns false when invoiceNo differs")
    void fileInvoiceRequest_equalsDifferentInvoiceNo() {
        FileInvoiceRequest r1 = FileInvoiceRequest.builder().invoiceNo("INV-A").build();
        FileInvoiceRequest r2 = FileInvoiceRequest.builder().invoiceNo("INV-B").build();
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("FileInvoiceRequest: equals returns false when one field is null")
    void fileInvoiceRequest_equalsNullVsNonNull() {
        FileInvoiceRequest r1 = FileInvoiceRequest.builder().templateCode("01GTKT").build();
        FileInvoiceRequest r2 = FileInvoiceRequest.builder().build();
        assertThat(r1).isNotEqualTo(r2);
        assertThat(r2).isNotEqualTo(r1);
    }

    @Test
    @DisplayName("FileInvoiceRequest: equals true for same reference")
    void fileInvoiceRequest_equalsSameRef() {
        FileInvoiceRequest r = FileInvoiceRequest.builder().supplierTaxCode("A").build();
        assertThat(r).isEqualTo(r);
    }

    @Test
    @DisplayName("FileInvoiceRequest: hashCode consistent")
    void fileInvoiceRequest_hashCode() {
        FileInvoiceRequest r = FileInvoiceRequest.builder()
                .supplierTaxCode("A").invoiceNo("B").build();
        assertThat(r.hashCode()).isEqualTo(r.hashCode());
    }

    @Test
    @DisplayName("FileInvoiceRequest: hashCode differs when transactionUuid differs")
    void fileInvoiceRequest_hashCodeDiffers() {
        FileInvoiceRequest r1 = FileInvoiceRequest.builder().transactionUuid("uuid-1").build();
        FileInvoiceRequest r2 = FileInvoiceRequest.builder().transactionUuid("uuid-2").build();
        assertThat(r1.hashCode()).isNotEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("FileInvoiceRequest: toString includes field names")
    void fileInvoiceRequest_toString() {
        FileInvoiceRequest r = FileInvoiceRequest.builder()
                .supplierTaxCode("TAX").fileType("xml").build();
        assertThat(r.toString()).contains("supplierTaxCode").contains("fileType");
    }

    // ── SInvoiceStatusResponse: equals / hashCode / toString ─────────────────

    @Test
    @DisplayName("SInvoiceStatusResponse: equals returns false for null")
    void sInvoiceStatusResponse_equalsNull() {
        SInvoiceStatusResponse r = new SInvoiceStatusResponse();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: equals returns false for different type")
    void sInvoiceStatusResponse_equalsDifferentType() {
        SInvoiceStatusResponse r = new SInvoiceStatusResponse();
        assertThat(r.equals("string")).isFalse();
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: equals false when errorCode differs")
    void sInvoiceStatusResponse_equalsDifferentErrorCode() {
        SInvoiceStatusResponse r1 = new SInvoiceStatusResponse();
        r1.setErrorCode("0");
        SInvoiceStatusResponse r2 = new SInvoiceStatusResponse();
        r2.setErrorCode("1");
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: equals false when one side null field")
    void sInvoiceStatusResponse_equalsNullField() {
        SInvoiceStatusResponse r1 = new SInvoiceStatusResponse();
        r1.setDescription("Success");
        SInvoiceStatusResponse r2 = new SInvoiceStatusResponse();
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: hashCode consistent")
    void sInvoiceStatusResponse_hashCode() {
        SInvoiceStatusResponse r = new SInvoiceStatusResponse();
        r.setErrorCode("0");
        r.setTransactionUuid("tx-123");
        assertThat(r.hashCode()).isEqualTo(r.hashCode());
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: hashCode differs for different values")
    void sInvoiceStatusResponse_hashCodeDiffers() {
        SInvoiceStatusResponse r1 = new SInvoiceStatusResponse();
        r1.setTransactionUuid("tx-1");
        SInvoiceStatusResponse r2 = new SInvoiceStatusResponse();
        r2.setTransactionUuid("tx-2");
        assertThat(r1.hashCode()).isNotEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: toString contains class info")
    void sInvoiceStatusResponse_toString() {
        SInvoiceStatusResponse r = new SInvoiceStatusResponse();
        r.setErrorCode("0");
        r.setDescription("OK");
        assertThat(r.toString()).contains("errorCode").contains("description");
    }

    // ── SInvoiceFileResponse: additional equals / hashCode / toString ─────────

    @Test
    @DisplayName("SInvoiceFileResponse: equals false for null")
    void sInvoiceFileResponse_equalsNull() {
        SInvoiceFileResponse r = new SInvoiceFileResponse();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    @DisplayName("SInvoiceFileResponse: equals false for different type")
    void sInvoiceFileResponse_equalsDifferentType() {
        SInvoiceFileResponse r = new SInvoiceFileResponse();
        assertThat(r.equals(42)).isFalse();
    }

    @Test
    @DisplayName("SInvoiceFileResponse: equals false when errorCode differs")
    void sInvoiceFileResponse_equalsDifferentErrorCode() {
        SInvoiceFileResponse r1 = new SInvoiceFileResponse();
        r1.setErrorCode(0);
        SInvoiceFileResponse r2 = new SInvoiceFileResponse();
        r2.setErrorCode(1);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceFileResponse: equals false when description differs")
    void sInvoiceFileResponse_equalsDifferentDescription() {
        SInvoiceFileResponse r1 = new SInvoiceFileResponse();
        r1.setDescription("OK");
        SInvoiceFileResponse r2 = new SInvoiceFileResponse();
        r2.setDescription("Error");
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceFileResponse: equals false when paymentStatus differs")
    void sInvoiceFileResponse_equalsDifferentPaymentStatus() {
        SInvoiceFileResponse r1 = new SInvoiceFileResponse();
        r1.setPaymentStatus(true);
        SInvoiceFileResponse r2 = new SInvoiceFileResponse();
        r2.setPaymentStatus(false);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("SInvoiceFileResponse: equals false when fileName is null on one side")
    void sInvoiceFileResponse_equalsNullFileName() {
        SInvoiceFileResponse r1 = new SInvoiceFileResponse();
        r1.setFileName("invoice.pdf");
        SInvoiceFileResponse r2 = new SInvoiceFileResponse();
        assertThat(r1).isNotEqualTo(r2);
        assertThat(r2).isNotEqualTo(r1);
    }

    @Test
    @DisplayName("SInvoiceFileResponse: hashCode consistent")
    void sInvoiceFileResponse_hashCode() {
        SInvoiceFileResponse r = new SInvoiceFileResponse();
        r.setErrorCode(0);
        r.setFileName("test.pdf");
        assertThat(r.hashCode()).isEqualTo(r.hashCode());
    }

    @Test
    @DisplayName("SInvoiceFileResponse: toString contains field names")
    void sInvoiceFileResponse_toString() {
        SInvoiceFileResponse r = new SInvoiceFileResponse();
        r.setErrorCode(0);
        r.setDescription("OK");
        r.setFileName("invoice.pdf");
        assertThat(r.toString()).contains("description").contains("fileName");
    }

    // ── SAccessTokenResponse: additional coverage ─────────────────────────────

    @Test
    @DisplayName("SAccessTokenResponse: equals false for null")
    void sAccessTokenResponse_equalsNull() {
        SAccessTokenResponse t = new SAccessTokenResponse();
        assertThat(t.equals(null)).isFalse();
    }

    @Test
    @DisplayName("SAccessTokenResponse: equals false for different type")
    void sAccessTokenResponse_equalsDifferentType() {
        SAccessTokenResponse t = new SAccessTokenResponse();
        assertThat(t.equals("string")).isFalse();
    }

    @Test
    @DisplayName("SAccessTokenResponse: equals false when accessToken differs")
    void sAccessTokenResponse_equalsDifferentToken() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setAccessToken("token-A");
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setAccessToken("token-B");
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("SAccessTokenResponse: equals false when one accessToken is null")
    void sAccessTokenResponse_equalsNullToken() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setAccessToken("token");
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("SAccessTokenResponse: equals false when expiresIn differs (primitive int)")
    void sAccessTokenResponse_equalsDifferentExpiresIn() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setExpiresIn(3600);
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setExpiresIn(7200);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("SAccessTokenResponse: equals false when issuedAt differs (primitive long)")
    void sAccessTokenResponse_equalsDifferentIssuedAt() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setIssuedAt(1000000L);
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setIssuedAt(2000000L);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("SAccessTokenResponse: equals false when type differs (primitive int)")
    void sAccessTokenResponse_equalsDifferentType_field() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setType(1);
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setType(2);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("SAccessTokenResponse: hashCode consistent")
    void sAccessTokenResponse_hashCode_consistent() {
        SAccessTokenResponse t = new SAccessTokenResponse();
        t.setAccessToken("token");
        t.setExpiresIn(3600);
        assertThat(t.hashCode()).isEqualTo(t.hashCode());
    }

    @Test
    @DisplayName("SAccessTokenResponse: hashCode differs for different expiresIn")
    void sAccessTokenResponse_hashCode_differs() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setExpiresIn(3600);
        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setExpiresIn(7200);
        assertThat(t1.hashCode()).isNotEqualTo(t2.hashCode());
    }

    @Test
    @DisplayName("SAccessTokenResponse: toString includes field names")
    void sAccessTokenResponse_toString() {
        SAccessTokenResponse t = new SAccessTokenResponse();
        t.setAccessToken("eyJ...");
        t.setTokenType("Bearer");
        t.setJwtId("jti-1");
        assertThat(t.toString()).contains("accessToken").contains("tokenType");
    }

    // ── Full-population equals/hashCode (covers non-null field comparison branches) ──

    @Test
    @DisplayName("SAccessTokenResponse: equals true when all 9 fields are equal and non-null")
    void sAccessTokenResponse_equalsAllFieldsPopulated() {
        SAccessTokenResponse t1 = new SAccessTokenResponse();
        t1.setAccessToken("tok"); t1.setTokenType("Bearer"); t1.setRefreshToken("ref");
        t1.setExpiresIn(3600); t1.setScope("read"); t1.setIssuedAt(1000L);
        t1.setInvoiceCluster("c1"); t1.setType(1); t1.setJwtId("jti-1");

        SAccessTokenResponse t2 = new SAccessTokenResponse();
        t2.setAccessToken("tok"); t2.setTokenType("Bearer"); t2.setRefreshToken("ref");
        t2.setExpiresIn(3600); t2.setScope("read"); t2.setIssuedAt(1000L);
        t2.setInvoiceCluster("c1"); t2.setType(1); t2.setJwtId("jti-1");

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    @DisplayName("FileInvoiceRequest: equals true when all 5 fields are equal and non-null")
    void fileInvoiceRequest_equalsAllFieldsPopulated() {
        FileInvoiceRequest r1 = FileInvoiceRequest.builder()
                .supplierTaxCode("TAX001").invoiceNo("INV-A").templateCode("01GTKT")
                .transactionUuid("tx-uuid-123").fileType("pdf").build();

        FileInvoiceRequest r2 = FileInvoiceRequest.builder()
                .supplierTaxCode("TAX001").invoiceNo("INV-A").templateCode("01GTKT")
                .transactionUuid("tx-uuid-123").fileType("pdf").build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("FileInvoiceRequest: setters for invoiceNo, templateCode, transactionUuid")
    void fileInvoiceRequest_uncoveredSetters() {
        FileInvoiceRequest r = FileInvoiceRequest.builder().build();
        r.setInvoiceNo("INV-999");
        r.setTemplateCode("01GTKT0/001");
        r.setTransactionUuid("new-uuid-abc");

        assertThat(r.getInvoiceNo()).isEqualTo("INV-999");
        assertThat(r.getTemplateCode()).isEqualTo("01GTKT0/001");
        assertThat(r.getTransactionUuid()).isEqualTo("new-uuid-abc");
    }

    @Test
    @DisplayName("FileInvoiceRequest builder: toString does not throw")
    void fileInvoiceRequest_builderToString() {
        String str = FileInvoiceRequest.builder()
                .supplierTaxCode("X").invoiceNo("Y").toString();
        assertThat(str).isNotBlank();
    }

    @Test
    @DisplayName("SInvoiceStatusResponse: equals true when all fields equal and non-null")
    void sInvoiceStatusResponse_equalsAllFieldsPopulated() {
        SInvoiceStatusResponse r1 = new SInvoiceStatusResponse();
        r1.setErrorCode("0"); r1.setDescription("OK");
        r1.setTransactionUuid("tx-123"); r1.setResult(java.util.List.of());

        SInvoiceStatusResponse r2 = new SInvoiceStatusResponse();
        r2.setErrorCode("0"); r2.setDescription("OK");
        r2.setTransactionUuid("tx-123"); r2.setResult(java.util.List.of());

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("SInvoiceStatusRequest: equals true when both fields equal and non-null")
    void sInvoiceStatusRequest_equalsAllFieldsPopulated() {
        SInvoiceStatusRequest r1 = SInvoiceStatusRequest.builder()
                .supplierTaxCode("TAX").transactionUuid("tx-abc").build();
        SInvoiceStatusRequest r2 = SInvoiceStatusRequest.builder()
                .supplierTaxCode("TAX").transactionUuid("tx-abc").build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("SInvoiceStatusRequest builder: toString does not throw")
    void sInvoiceStatusRequest_builderToString() {
        String str = SInvoiceStatusRequest.builder()
                .supplierTaxCode("TAX").toString();
        assertThat(str).isNotBlank();
    }
}
